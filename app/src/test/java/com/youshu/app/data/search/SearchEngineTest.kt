package com.youshu.app.data.search

import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.local.entity.Location
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchEngineTest {

    private val engine = SearchEngine()

    @Test
    fun filter_returnsPendingReviewItemsWhenCriteriaUsesUsedUpAndPendingRating() {
        val items = listOf(
            itemDetail(id = 1L, name = "充电宝", status = Item.STATUS_USED_UP, rating = null),
            itemDetail(id = 2L, name = "键盘", status = Item.STATUS_USED_UP, rating = 5),
            itemDetail(id = 3L, name = "鼠标", status = Item.STATUS_IN_USE, rating = null)
        )

        val result = engine.filter(
            items = items,
            locations = emptyList(),
            criteria = SearchCriteria(
                kind = SearchIntentKind.FILTER_ITEMS,
                status = SearchItemStatus.USED_UP,
                ratingState = SearchRatingState.PENDING
            )
        )

        assertEquals(listOf(1L), result.map { it.item.id })
    }

    @Test
    fun filter_returnsReviewedItemsWhenCriteriaUsesReviewedRating() {
        val items = listOf(
            itemDetail(id = 1L, name = "充电宝", status = Item.STATUS_USED_UP, rating = null),
            itemDetail(id = 2L, name = "键盘", status = Item.STATUS_USED_UP, rating = 4)
        )

        val result = engine.filter(
            items = items,
            locations = emptyList(),
            criteria = SearchCriteria(
                kind = SearchIntentKind.FILTER_ITEMS,
                status = SearchItemStatus.USED_UP,
                ratingState = SearchRatingState.REVIEWED
            )
        )

        assertEquals(listOf(2L), result.map { it.item.id })
    }

    @Test
    fun filter_matchesLocationAncestorsWhenBrowsingLocation() {
        val locations = listOf(
            Location(id = 1L, name = "我的家"),
            Location(id = 2L, name = "书房", parentId = 1L),
            Location(id = 3L, name = "台面", parentId = 2L)
        )
        val items = listOf(
            itemDetail(id = 1L, name = "键盘", locationId = 3L, locationName = "台面"),
            itemDetail(id = 2L, name = "牛奶", locationId = null, locationName = "冰箱")
        )

        val result = engine.filter(
            items = items,
            locations = locations,
            criteria = SearchCriteria(
                kind = SearchIntentKind.BROWSE_LOCATION,
                locationName = "书房"
            )
        )

        assertEquals(listOf(1L), result.map { it.item.id })
    }

    @Test
    fun filter_keepsPlainTextSearchForItemName() {
        val items = listOf(
            itemDetail(id = 1L, name = "充电宝"),
            itemDetail(id = 2L, name = "键盘")
        )

        val result = engine.filter(
            items = items,
            locations = emptyList(),
            criteria = SearchCriteria(text = "我的充电在哪")
        )

        assertEquals(listOf(1L), result.map { it.item.id })
    }

    @Test
    fun filter_matchesAnySemanticTerm() {
        val items = listOf(
            itemDetail(id = 1L, name = "键盘"),
            itemDetail(id = 2L, name = "鼠标"),
            itemDetail(id = 3L, name = "充电宝")
        )

        val result = engine.filter(
            items = items,
            locations = emptyList(),
            criteria = SearchCriteria(
                semanticTerms = listOf("键盘", "鼠标")
            )
        )

        assertEquals(listOf(2L, 1L), result.map { it.item.id })
    }

    @Test
    fun filter_doesNotApplySemanticTermsAgainWhenItemIdsAreExecutablePlan() {
        val items = listOf(
            itemDetail(id = 1L, name = "键盘", categoryName = "数码"),
            itemDetail(id = 2L, name = "鼠标", categoryName = "数码"),
            itemDetail(id = 3L, name = "充电宝", categoryName = "数码")
        )

        val result = engine.filter(
            items = items,
            locations = emptyList(),
            criteria = SearchCriteria(
                kind = SearchIntentKind.BROWSE_CATEGORY,
                itemIds = setOf(1L, 2L),
                categoryIds = setOf(9L),
                semanticTerms = listOf("电脑输入设备")
            )
        )

        assertEquals(listOf(2L, 1L), result.map { it.item.id })
    }

    private fun itemDetail(
        id: Long,
        name: String,
        status: Int = Item.STATUS_IN_USE,
        rating: Int? = null,
        locationId: Long? = null,
        locationName: String? = null,
        categoryName: String? = null
    ): ItemDetail {
        return ItemDetail(
            item = Item(
                id = id,
                name = name,
                status = status,
                rating = rating,
                locationId = locationId,
                createdAt = id
            ),
            categoryName = categoryName,
            locationName = locationName
        )
    }
}
