package com.youshu.app.data.search

enum class SearchIntentKind {
    PLAIN_TEXT,
    FIND_ITEM,
    FILTER_ITEMS,
    BROWSE_LOCATION,
    BROWSE_CATEGORY
}

enum class SearchItemStatus {
    ANY,
    IN_USE,
    USED_UP
}

enum class SearchRatingState {
    ANY,
    PENDING,
    REVIEWED
}

enum class SearchSort {
    CREATED_DESC,
    EXPIRE_ASC,
    NAME_ASC
}

data class SearchCriteria(
    val kind: SearchIntentKind = SearchIntentKind.PLAIN_TEXT,
    val text: String = "",
    val semanticTerms: List<String> = emptyList(),
    val itemIds: Set<Long> = emptySet(),
    val itemName: String? = null,
    val categoryIds: Set<Long> = emptySet(),
    val categoryName: String? = null,
    val locationIds: Set<Long> = emptySet(),
    val locationName: String? = null,
    val status: SearchItemStatus = SearchItemStatus.ANY,
    val ratingState: SearchRatingState = SearchRatingState.ANY,
    val expiringWithinDays: Int? = null,
    val sort: SearchSort = SearchSort.CREATED_DESC,
    val summary: String? = null
) {
    fun hasStructuredFilters(): Boolean {
        return itemIds.isNotEmpty() ||
            itemName != null ||
            categoryIds.isNotEmpty() ||
            categoryName != null ||
            locationIds.isNotEmpty() ||
            locationName != null ||
            semanticTerms.isNotEmpty() ||
            status != SearchItemStatus.ANY ||
            ratingState != SearchRatingState.ANY ||
            expiringWithinDays != null
    }
}
