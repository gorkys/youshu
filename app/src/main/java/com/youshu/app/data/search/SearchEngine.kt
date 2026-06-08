package com.youshu.app.data.search

import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.local.entity.Location
import com.youshu.app.util.LegacyTextNormalizer

class SearchEngine {

    fun filter(
        items: List<ItemDetail>,
        locations: List<Location>,
        criteria: SearchCriteria
    ): List<ItemDetail> {
        val matched = items.filter { itemDetail ->
            itemDetail.matchesCriteria(criteria, locations)
        }
        return matched.sortBy(criteria.sort)
    }

    private fun ItemDetail.matchesCriteria(
        criteria: SearchCriteria,
        locations: List<Location>
    ): Boolean {
        return matchesStructuredFields(criteria, locations) &&
            matchesText(criteria, locations) &&
            matchesStatus(criteria.status) &&
            matchesRating(criteria.ratingState) &&
            matchesExpiry(criteria.expiringWithinDays)
    }

    private fun ItemDetail.matchesStructuredFields(
        criteria: SearchCriteria,
        locations: List<Location>
    ): Boolean {
        if (criteria.itemIds.isNotEmpty()) {
            return item.id in criteria.itemIds
        }
        return matchesItem(criteria) &&
            matchesCategory(criteria) &&
            matchesLocation(criteria, locations)
    }

    private fun ItemDetail.matchesItem(criteria: SearchCriteria): Boolean {
        return when {
            criteria.itemIds.isNotEmpty() -> item.id in criteria.itemIds
            !criteria.itemName.isNullOrBlank() -> namesMatch(item.name, criteria.itemName)
            else -> true
        }
    }

    private fun ItemDetail.matchesCategory(criteria: SearchCriteria): Boolean {
        return when {
            criteria.categoryIds.isNotEmpty() -> item.categoryId in criteria.categoryIds
            !criteria.categoryName.isNullOrBlank() -> namesMatch(categoryName, criteria.categoryName)
            else -> true
        }
    }

    private fun ItemDetail.matchesLocation(
        criteria: SearchCriteria,
        locations: List<Location>
    ): Boolean {
        return when {
            criteria.locationIds.isNotEmpty() -> locationIdsForItem(locations).any { it in criteria.locationIds }
            !criteria.locationName.isNullOrBlank() -> locationNamesForItem(locations).any {
                namesMatch(it, criteria.locationName)
            }
            else -> true
        }
    }

    private fun ItemDetail.matchesStatus(status: SearchItemStatus): Boolean {
        return when (status) {
            SearchItemStatus.ANY -> true
            SearchItemStatus.IN_USE -> item.status == Item.STATUS_IN_USE
            SearchItemStatus.USED_UP -> item.status == Item.STATUS_USED_UP
        }
    }

    private fun ItemDetail.matchesRating(ratingState: SearchRatingState): Boolean {
        return when (ratingState) {
            SearchRatingState.ANY -> true
            SearchRatingState.PENDING -> item.isPendingReview()
            SearchRatingState.REVIEWED -> item.hasReview()
        }
    }

    private fun ItemDetail.matchesExpiry(expiringWithinDays: Int?): Boolean {
        val days = expiringWithinDays ?: return true
        val expireTime = item.expireTime ?: return false
        val threshold = System.currentTimeMillis() + days * ONE_DAY_MS
        return expireTime in System.currentTimeMillis()..threshold
    }

    private fun ItemDetail.matchesText(
        criteria: SearchCriteria,
        locations: List<Location>
    ): Boolean {
        if (criteria.itemIds.isNotEmpty() || !criteria.itemName.isNullOrBlank()) {
            return true
        }
        val hasText = criteria.text.isNotBlank()
        val semanticTerms = criteria.semanticTerms.filter { it.isNotBlank() }
        if (!hasText && semanticTerms.isEmpty()) return true
        return (hasText && matchesPlainText(criteria.text, locations)) ||
            semanticTerms.any { term -> matchesPlainText(term, locations) }
    }

    private fun ItemDetail.matchesPlainText(
        text: String,
        locations: List<Location>
    ): Boolean {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return true
        val digest = listOf(
            item.name,
            categoryName.orEmpty(),
            locationName.orEmpty(),
            locationNamesForItem(locations).joinToString(" "),
            item.note
        ).joinToString(" ")
        if (digest.contains(normalizedText, ignoreCase = true)) return true
        return splitSearchTerms(normalizedText).any { term ->
            digest.contains(term, ignoreCase = true)
        }
    }

    private fun ItemDetail.locationNamesForItem(locations: List<Location>): List<String> {
        val currentLocationId = item.locationId ?: return listOfNotNull(locationName)
        val result = mutableListOf<String>()
        var cursor = locations.firstOrNull { it.id == currentLocationId }
        if (cursor == null) locationName?.let(result::add)
        while (cursor != null) {
            result += cursor.name
            cursor = cursor.parentId?.let { parentId -> locations.firstOrNull { it.id == parentId } }
        }
        return result.distinct()
    }

    private fun ItemDetail.locationIdsForItem(locations: List<Location>): List<Long> {
        val currentLocationId = item.locationId ?: return emptyList()
        val result = mutableListOf<Long>()
        var cursor = locations.firstOrNull { it.id == currentLocationId }
        if (cursor == null) return listOf(currentLocationId)
        while (cursor != null) {
            result += cursor.id
            cursor = cursor.parentId?.let { parentId -> locations.firstOrNull { it.id == parentId } }
        }
        return result.distinct()
    }

    private fun List<ItemDetail>.sortBy(sort: SearchSort): List<ItemDetail> {
        return when (sort) {
            SearchSort.EXPIRE_ASC -> sortedBy { it.item.expireTime ?: Long.MAX_VALUE }
            SearchSort.NAME_ASC -> sortedBy { it.item.name }
            SearchSort.CREATED_DESC -> sortedByDescending { it.item.createdAt }
        }
    }

    private fun splitSearchTerms(text: String): List<String> {
        val normalized = text
            .replace(Regex("[\\s,，。.!！?？、：:；;（）()【】\\[\\]{}]+"), " ")
            .split(" ")
            .map { LegacyTextNormalizer.normalizeSearchText(it) }
            .map { it.trim() }
            .filter { it.length >= 2 }
        return normalized.ifEmpty { listOf(text) }
    }

    private fun namesMatch(actual: String?, requested: String?): Boolean {
        return LegacyTextNormalizer.namesMatch(actual, requested)
    }

    private companion object {
        const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    }
}
