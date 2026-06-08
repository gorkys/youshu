package com.youshu.app.data.ai

import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.search.SearchCriteria
import com.youshu.app.data.search.SearchIntentKind
import com.youshu.app.data.search.SearchItemStatus
import com.youshu.app.data.search.SearchRatingState
import com.youshu.app.data.search.SearchSort
import com.youshu.app.util.LegacyTextNormalizer

internal object AiSearchIntentNormalizer {

    /**
     * 将 AI 返回的搜索意图归一化为当前数据库可执行的结构化条件
     *
     * @param result AI 原始调用结果
     * @param query 用户原始搜索内容
     * @param items 当前可搜索物品候选
     * @param categories 当前分类候选
     * @param locations 当前位置候选
     * @return 已校验 id、名称和枚举字段的 AI 搜索意图
     */
    fun normalize(
        result: AiCallResult<AiSearchIntent>,
        query: String,
        items: List<Item>,
        categories: List<Category>,
        locations: List<Location>
    ): AiCallResult<AiSearchIntent> {
        return when (result) {
            is AiCallResult.Error -> result
            is AiCallResult.Success -> AiCallResult.Success(
                normalizeIntent(
                    intent = result.data,
                    query = query,
                    items = items,
                    categories = categories,
                    locations = locations
                )
            )
        }
    }

    /**
     * 将归一化后的 AI 搜索意图转换成本地搜索引擎入参
     *
     * @param query 用户原始搜索内容
     * @param intent 已归一化的 AI 搜索意图
     * @return 本地 SearchEngine 可执行的搜索条件
     */
    fun toSearchCriteria(query: String, intent: AiSearchIntent): SearchCriteria {
        return SearchCriteria(
            kind = intent.intentType.toSearchIntentKind(),
            text = if (intent.hasStructuredSearchFields()) "" else query.trim(),
            semanticTerms = intent.searchTerms(query),
            itemIds = intent.itemIds.toSet(),
            itemName = intent.itemName,
            categoryIds = intent.categoryIds.toSet(),
            categoryName = intent.categoryName,
            locationIds = intent.locationIds.toSet(),
            locationName = intent.locationName,
            status = intent.status.toSearchItemStatus(),
            ratingState = intent.ratingState.toSearchRatingState(),
            expiringWithinDays = intent.expiringWithinDays,
            sort = intent.sort.toSearchSort(),
            summary = intent.summary
        )
    }

    private fun normalizeIntent(
        intent: AiSearchIntent,
        query: String,
        items: List<Item>,
        categories: List<Category>,
        locations: List<Location>
    ): AiSearchIntent {
        val groundingTexts = buildGroundingTexts(query, null, intent.summary, intent.keywords + intent.semanticTerms)
        val intentType = inferIntentType(intent.intentType, query, categories, locations)
        val ratingState = intent.ratingState ?: inferRatingState(groundingTexts)
        val status = inferSearchStatus(intent.status, ratingState, groundingTexts)
        val expiringWithinDays = normalizeExpiringWithinDays(intent.expiringWithinDays, groundingTexts)
        val keywords = normalizeKeywords(intent.keywords)
        val semanticTerms = normalizeSemanticTerms(intent.semanticTerms)
        val categoryIds = normalizeCategoryIds(intent, intentType, query, categories, keywords)
        val locationIds = normalizeLocationIds(intent, intentType, query, locations, keywords)
        val matchedScopeNames = buildMatchedScopeNames(categoryIds, locationIds, categories, locations)
        val itemIds = normalizeItemIds(intent, intentType, items, keywords, semanticTerms, matchedScopeNames)

        return intent.copy(
            intentType = intentType,
            itemIds = itemIds,
            categoryIds = categoryIds,
            locationIds = locationIds,
            itemName = normalizeItemName(intent, itemIds, items),
            keywords = keywords,
            semanticTerms = semanticTerms,
            categoryName = normalizeCategoryName(intent, categoryIds, categories),
            locationName = normalizeLocationName(intent, locationIds, locations),
            status = status,
            ratingState = ratingState,
            expiringWithinDays = expiringWithinDays,
            confidence = intent.confidence?.coerceIn(0.0, 1.0)
        )
    }

    private fun normalizeKeywords(keywords: List<String>): List<String> {
        return keywords
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot(::isStatusOrRatingKeyword)
            .filterNot(::isGenericIntentKeyword)
            .distinct()
    }

    private fun normalizeSemanticTerms(semanticTerms: List<String>): List<String> {
        return semanticTerms
            .flatMap { it.extractListedTerms() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot(::isStatusOrRatingKeyword)
            .filterNot(::isGenericIntentKeyword)
            .distinct()
    }

    private fun normalizeCategoryIds(
        intent: AiSearchIntent,
        intentType: AiSearchIntentType,
        query: String,
        categories: List<Category>,
        keywords: List<String>
    ): List<Long> {
        val shouldInfer = intent.categoryName?.isNotBlank() == true ||
            intentType == AiSearchIntentType.BROWSE_CATEGORY
        return mergeValidIds(
            originalIds = intent.categoryIds,
            candidates = categories,
            idSelector = Category::id,
            inferredIds = if (shouldInfer) {
                findMatchingCategoryIds(
                    categories = categories,
                    texts = buildGroundingTexts(query, intent.categoryName, null, keywords)
                )
            } else {
                emptyList()
            }
        )
    }

    private fun normalizeLocationIds(
        intent: AiSearchIntent,
        intentType: AiSearchIntentType,
        query: String,
        locations: List<Location>,
        keywords: List<String>
    ): List<Long> {
        val shouldInfer = intent.locationName?.isNotBlank() == true ||
            intentType == AiSearchIntentType.BROWSE_LOCATION
        return mergeValidIds(
            originalIds = intent.locationIds,
            candidates = locations,
            idSelector = Location::id,
            inferredIds = if (shouldInfer) {
                findMatchingLocationIds(
                    locations = locations,
                    texts = buildGroundingTexts(query, intent.locationName, null, keywords)
                )
            } else {
                emptyList()
            }
        )
    }

    private fun normalizeItemIds(
        intent: AiSearchIntent,
        intentType: AiSearchIntentType,
        items: List<Item>,
        keywords: List<String>,
        semanticTerms: List<String>,
        matchedScopeNames: List<String>
    ): List<Long> {
        val knownItemNamesInAiText = extractKnownItemNamesFromTexts(
            texts = listOfNotNull(intent.summary) + semanticTerms + keywords,
            items = items
        )
        val shouldInfer = intent.itemName?.isNotBlank() == true ||
            intentType == AiSearchIntentType.FIND_ITEM_LOCATION ||
            semanticTerms.isNotEmpty() ||
            knownItemNamesInAiText.isNotEmpty()
        return mergeValidIds(
            originalIds = intent.itemIds,
            candidates = items,
            idSelector = Item::id,
            inferredIds = if (shouldInfer) {
                findMatchingItemIds(
                    items = items,
                    texts = buildItemGroundingTexts(
                        directName = intent.itemName,
                        summary = intent.summary,
                        keywords = keywords,
                        semanticTerms = semanticTerms,
                        knownItemNamesInAiText = knownItemNamesInAiText,
                        items = items,
                        matchedScopeNames = matchedScopeNames
                    )
                )
            } else {
                emptyList()
            }
        )
    }

    private fun normalizeItemName(
        intent: AiSearchIntent,
        itemIds: List<Long>,
        items: List<Item>
    ): String? {
        return when {
            itemIds.isNotEmpty() -> items.firstOrNull { it.id in itemIds }?.name
            !intent.itemName.isNullOrBlank() -> intent.itemName.matchExistingName(items.map { it.name })
            else -> null
        }
    }

    private fun normalizeCategoryName(
        intent: AiSearchIntent,
        categoryIds: List<Long>,
        categories: List<Category>
    ): String? {
        return when {
            categoryIds.isNotEmpty() -> categories.firstOrNull { it.id in categoryIds }?.name
            !intent.categoryName.isNullOrBlank() -> intent.categoryName.matchExistingName(categories.map { it.name })
            else -> null
        }
    }

    private fun normalizeLocationName(
        intent: AiSearchIntent,
        locationIds: List<Long>,
        locations: List<Location>
    ): String? {
        return when {
            locationIds.isNotEmpty() -> locations.firstOrNull { it.id in locationIds }?.name
            !intent.locationName.isNullOrBlank() -> intent.locationName.matchExistingName(locations.map { it.name })
            else -> null
        }
    }

    private fun inferIntentType(
        originalType: AiSearchIntentType,
        query: String,
        categories: List<Category>,
        locations: List<Location>
    ): AiSearchIntentType {
        val value = query.trim()
        if (listOf("在哪", "在哪里", "哪儿", "哪里", "放哪", "位置").any(value::contains)) {
            return AiSearchIntentType.FIND_ITEM_LOCATION
        }
        if (originalType != AiSearchIntentType.UNKNOWN) return originalType
        if (!listOf("有什么", "有哪些", "都有什么", "有啥").any(value::contains)) {
            return AiSearchIntentType.UNKNOWN
        }
        val texts = listOf(query)
        return when {
            findMatchingCategoryIds(categories, texts).isNotEmpty() -> AiSearchIntentType.BROWSE_CATEGORY
            findMatchingLocationIds(locations, texts).isNotEmpty() -> AiSearchIntentType.BROWSE_LOCATION
            else -> AiSearchIntentType.BROWSE_LOCATION
        }
    }

    private fun <T> mergeValidIds(
        originalIds: List<Long>,
        candidates: List<T>,
        idSelector: (T) -> Long,
        inferredIds: List<Long>
    ): List<Long> {
        val validIds = candidates.map(idSelector).toSet()
        return (originalIds + inferredIds)
            .filter { it in validIds }
            .distinct()
    }

    private fun buildGroundingTexts(
        query: String,
        directName: String?,
        summary: String?,
        keywords: List<String>
    ): List<String> {
        return (listOf(query, directName.orEmpty(), summary.orEmpty()) + keywords)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun inferSearchStatus(
        originalStatus: AiSearchStatus?,
        ratingState: AiRatingState?,
        texts: List<String>
    ): AiSearchStatus? {
        if (ratingState == AiRatingState.PENDING || ratingState == AiRatingState.REVIEWED) {
            return AiSearchStatus.USED_UP
        }
        if (texts.any(::containsUsedUpIntent)) return AiSearchStatus.USED_UP
        if (texts.any(::containsInUseIntent)) return AiSearchStatus.IN_USE
        return originalStatus
    }

    private fun inferRatingState(texts: List<String>): AiRatingState? {
        return when {
            texts.any(::containsPendingReviewIntent) -> AiRatingState.PENDING
            texts.any(::containsReviewedIntent) -> AiRatingState.REVIEWED
            else -> null
        }
    }

    private fun isStatusOrRatingKeyword(keyword: String): Boolean {
        return containsPendingReviewIntent(keyword) ||
            containsReviewedIntent(keyword) ||
            containsUsedUpIntent(keyword) ||
            containsInUseIntent(keyword)
    }

    private fun isGenericIntentKeyword(keyword: String): Boolean {
        val value = LegacyTextNormalizer.normalizeSearchText(keyword)
        return value.isBlank() ||
            listOf("物品", "东西", "查找", "搜索", "我的", "位置", "在哪", "哪里", "有哪些", "有什么")
                .any { value == it }
    }

    private fun containsPendingReviewIntent(text: String): Boolean {
        val value = text.trim()
        val upperValue = value.uppercase()
        if ("PENDING" in upperValue || "PENDING_REVIEW" in upperValue) return true
        return listOf("待评价", "未评价", "没评价", "还没评价", "未评分", "没评分", "待评分", "需要评价")
            .any(value::contains)
    }

    private fun containsReviewedIntent(text: String): Boolean {
        val value = text.trim()
        val upperValue = value.uppercase()
        if (containsPendingReviewIntent(value)) return false
        if ("REVIEWED" in upperValue) return true
        return listOf("已评价", "评价过", "已评分", "评分过")
            .any(value::contains)
    }

    private fun containsUsedUpIntent(text: String): Boolean {
        val value = text.trim()
        val upperValue = value.uppercase()
        if ("USED_UP" in upperValue) return true
        return listOf("已用完", "用完", "耗尽", "空了", "吃完", "喝完", "用掉")
            .any(value::contains)
    }

    private fun containsInUseIntent(text: String): Boolean {
        val value = text.trim()
        val upperValue = value.uppercase()
        if ("IN_USE" in upperValue) return true
        return listOf("在用", "未用完", "没用完", "还能用", "还在用")
            .any(value::contains)
    }

    private fun normalizeExpiringWithinDays(
        expiringWithinDays: Int?,
        texts: List<String>
    ): Int? {
        if (!texts.any(::containsExpiringIntent)) return null
        return expiringWithinDays?.coerceIn(1, 365) ?: 7
    }

    private fun containsExpiringIntent(text: String): Boolean {
        val value = text.trim()
        return listOf("快过期", "即将过期", "临期", "到期", "过期", "expire", "expiring")
            .any(value::contains)
    }

    private fun buildItemGroundingTexts(
        directName: String?,
        summary: String?,
        keywords: List<String>,
        semanticTerms: List<String>,
        knownItemNamesInAiText: List<String>,
        items: List<Item>,
        matchedScopeNames: List<String>
    ): List<String> {
        val directTexts = listOf(directName.orEmpty()).filter { it.isNotBlank() }
        return (directTexts + semanticTerms + keywords + extractKnownItemNames(summary, items) + knownItemNamesInAiText)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { text -> matchedScopeNames.any { scopeName -> isPureScopeText(text, scopeName) } }
            .distinct()
    }

    private fun buildMatchedScopeNames(
        categoryIds: List<Long>,
        locationIds: List<Long>,
        categories: List<Category>,
        locations: List<Location>
    ): List<String> {
        return categories
            .filter { it.id in categoryIds }
            .map { it.name } +
            locations
                .filter { it.id in locationIds }
                .map { it.name }
    }

    private fun isPureScopeText(text: String, scopeName: String): Boolean {
        val normalizedText = LegacyTextNormalizer.normalizeSearchText(text)
        val normalizedScope = LegacyTextNormalizer.normalizeSearchText(scopeName)
        return normalizedText.equals(normalizedScope, ignoreCase = true) ||
            normalizedScope.contains(normalizedText, ignoreCase = true)
    }

    private fun findMatchingItemIds(items: List<Item>, texts: List<String>): List<Long> {
        return items
            .filter { item -> texts.any { text -> candidateAppearsInText(item.name, text) } }
            .map { it.id }
            .distinct()
    }

    private fun findMatchingCategoryIds(categories: List<Category>, texts: List<String>): List<Long> {
        return categories
            .filter { category -> texts.any { text -> candidateAppearsInText(category.name, text) } }
            .map { it.id }
            .distinct()
    }

    private fun findMatchingLocationIds(locations: List<Location>, texts: List<String>): List<Long> {
        val locationPaths = buildLocationPaths(locations)
        return locations
            .filter { location ->
                val path = locationPaths[location.id].orEmpty()
                texts.any { text ->
                    candidateAppearsInText(location.name, text) ||
                        candidateAppearsInText(path, text)
                }
            }
            .map { it.id }
            .distinct()
    }

    private fun candidateAppearsInText(candidateName: String, text: String): Boolean {
        val candidate = LegacyTextNormalizer.normalizeSearchText(candidateName)
        val normalizedText = LegacyTextNormalizer.normalizeSearchText(text)
        if (candidate.isBlank() || normalizedText.isBlank()) return false
        return LegacyTextNormalizer.namesMatch(candidate, normalizedText) ||
            normalizedText.contains(candidate, ignoreCase = true) ||
            candidate.contains(normalizedText, ignoreCase = true)
    }

    private fun buildLocationPaths(locations: List<Location>): Map<Long, String> {
        val byId = locations.associateBy { it.id }
        return locations.associate { location ->
            val names = mutableListOf<String>()
            var cursor: Location? = location
            while (cursor != null) {
                names += LegacyTextNormalizer.normalizeName(cursor.name)
                cursor = cursor.parentId?.let(byId::get)
            }
            location.id to names.asReversed().joinToString("/")
        }
    }

    private fun String?.matchExistingName(candidates: List<String>): String? {
        val normalized = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return candidates.firstOrNull { candidate ->
            LegacyTextNormalizer.namesMatch(candidate, normalized)
        }
            ?: candidates.firstOrNull { normalized.contains(it, ignoreCase = true) }
            ?: candidates.firstOrNull { it.contains(normalized, ignoreCase = true) }
    }

    private fun AiSearchIntent.searchTerms(query: String): List<String> {
        val structuredTerms = itemName.extractListedTerms() +
            categoryName.extractListedTerms() +
            locationName.extractListedTerms() +
            semanticTerms.flatMap { it.extractListedTerms() } +
            keywords
        return structuredTerms
            .map { LegacyTextNormalizer.normalizeSearchText(it) }
            .filter { it.length >= 2 }
            .filterNot(::isGenericIntentKeyword)
            .filterNot(::isStatusOrRatingKeyword)
            .distinct()
    }

    private fun extractKnownItemNames(text: String?, items: List<Item>): List<String> {
        val source = text?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
        val normalizedSource = LegacyTextNormalizer.normalizeSearchText(source)
        val sourceTerms = source.extractListedTerms()
        return items
            .map { LegacyTextNormalizer.normalizeName(it.name) }
            .filter { itemName ->
                source.containsKnownItemName(itemName) ||
                    sourceTerms.any { term -> candidateAppearsInText(itemName, term) } ||
                    exactCandidateNameAppearsInText(itemName, normalizedSource)
            }
            .distinct()
    }

    private fun extractKnownItemNamesFromTexts(texts: List<String>, items: List<Item>): List<String> {
        if (texts.isEmpty()) return emptyList()
        val source = texts.joinToString(" ")
        return extractKnownItemNames(source, items)
    }

    private fun String.containsKnownItemName(itemName: String): Boolean {
        if (itemName.isBlank()) return false
        val source = LegacyTextNormalizer.normalizeSearchText(this)
        val normalizedName = LegacyTextNormalizer.normalizeSearchText(itemName)
        return source.contains(normalizedName, ignoreCase = true)
    }

    private fun exactCandidateNameAppearsInText(candidateName: String, normalizedText: String): Boolean {
        val candidate = LegacyTextNormalizer.normalizeSearchText(candidateName)
        return candidate.isNotBlank() && normalizedText.contains(candidate, ignoreCase = true)
    }

    private fun String?.extractListedTerms(): List<String> {
        val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
        return value
            .replace(Regex("[（）()【】\\[\\]{}]"), " ")
            .replace(Regex("(包括|包含|含有|例如|比如|比如说|例如说|以及|并且|还有|或者|和|与|及|或)"), " ")
            .split(Regex("[,，、/；;\\s]+"))
            .map { LegacyTextNormalizer.normalizeSearchText(it) }
            .filter { it.length >= 2 }
            .distinct()
    }

    private fun AiSearchIntent.hasStructuredSearchFields(): Boolean {
        return itemIds.isNotEmpty() ||
            categoryIds.isNotEmpty() ||
            locationIds.isNotEmpty() ||
            !itemName.isNullOrBlank() ||
            !categoryName.isNullOrBlank() ||
            !locationName.isNullOrBlank() ||
            semanticTerms.isNotEmpty() ||
            (status != null && status != AiSearchStatus.ALL) ||
            (ratingState != null && ratingState != AiRatingState.ANY) ||
            expiringWithinDays != null
    }

    private fun AiSearchIntentType.toSearchIntentKind(): SearchIntentKind {
        return when (this) {
            AiSearchIntentType.FIND_ITEM_LOCATION -> SearchIntentKind.FIND_ITEM
            AiSearchIntentType.BROWSE_LOCATION -> SearchIntentKind.BROWSE_LOCATION
            AiSearchIntentType.BROWSE_CATEGORY -> SearchIntentKind.BROWSE_CATEGORY
            AiSearchIntentType.FILTER_ITEMS -> SearchIntentKind.FILTER_ITEMS
            AiSearchIntentType.UNKNOWN -> SearchIntentKind.PLAIN_TEXT
        }
    }

    private fun AiSearchStatus?.toSearchItemStatus(): SearchItemStatus {
        return when (this) {
            AiSearchStatus.IN_USE -> SearchItemStatus.IN_USE
            AiSearchStatus.USED_UP -> SearchItemStatus.USED_UP
            AiSearchStatus.ALL,
            null -> SearchItemStatus.ANY
        }
    }

    private fun AiRatingState?.toSearchRatingState(): SearchRatingState {
        return when (this) {
            AiRatingState.PENDING -> SearchRatingState.PENDING
            AiRatingState.REVIEWED -> SearchRatingState.REVIEWED
            AiRatingState.ANY,
            null -> SearchRatingState.ANY
        }
    }

    private fun AiSearchSort?.toSearchSort(): SearchSort {
        return when (this) {
            AiSearchSort.EXPIRE_ASC -> SearchSort.EXPIRE_ASC
            AiSearchSort.NAME_ASC -> SearchSort.NAME_ASC
            AiSearchSort.CREATED_DESC,
            null -> SearchSort.CREATED_DESC
        }
    }
}
