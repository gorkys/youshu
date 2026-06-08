package com.youshu.app.data.ai

import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.search.SearchIntentKind
import com.youshu.app.data.search.SearchItemStatus
import com.youshu.app.data.search.SearchRatingState
import org.junit.Assert.assertEquals
import org.junit.Test

class AiSearchIntentNormalizerTest {

    @Test
    fun toSearchCriteria_keepsPendingReviewAsStructuredFilterWithoutText() {
        val intent = AiSearchIntent(
            intentType = AiSearchIntentType.FILTER_ITEMS,
            status = AiSearchStatus.USED_UP,
            ratingState = AiRatingState.PENDING,
            summary = "筛选待评价的物品"
        )

        val criteria = AiSearchIntentNormalizer.toSearchCriteria("待评价的物品", intent)

        assertEquals(SearchIntentKind.FILTER_ITEMS, criteria.kind)
        assertEquals("", criteria.text)
        assertEquals(SearchItemStatus.USED_UP, criteria.status)
        assertEquals(SearchRatingState.PENDING, criteria.ratingState)
    }

    @Test
    fun normalize_infersLocationIdFromAiLocationName() {
        val result = AiSearchIntentNormalizer.normalize(
            result = AiCallResult.Success(
                AiSearchIntent(
                    intentType = AiSearchIntentType.BROWSE_LOCATION,
                    locationName = "桌面",
                    status = AiSearchStatus.ALL,
                    ratingState = AiRatingState.ANY
                )
            ),
            query = "桌面有哪些东西",
            items = listOf(Item(id = 1L, name = "键盘", locationId = 2L)),
            categories = emptyList(),
            locations = listOf(Location(id = 2L, name = "台面"))
        )

        val intent = (result as AiCallResult.Success).data
        val criteria = AiSearchIntentNormalizer.toSearchCriteria("桌面有哪些东西", intent)

        assertEquals(SearchIntentKind.BROWSE_LOCATION, criteria.kind)
        assertEquals(setOf(2L), criteria.locationIds)
        assertEquals("", criteria.text)
    }

    @Test
    fun normalize_infersItemIdsFromSemanticTermsWhenBrowsingCategory() {
        val result = AiSearchIntentNormalizer.normalize(
            result = AiCallResult.Success(
                AiSearchIntent(
                    intentType = AiSearchIntentType.BROWSE_CATEGORY,
                    categoryIds = listOf(1L),
                    status = AiSearchStatus.IN_USE,
                    ratingState = AiRatingState.ANY,
                    semanticTerms = listOf("电脑输入设备", "键盘", "鼠标"),
                    summary = "查找数码分类下的电脑输入设备存货"
                )
            ),
            query = "电脑输入设备有哪些存货",
            items = listOf(
                Item(id = 1L, name = "键盘", categoryId = 1L),
                Item(id = 2L, name = "鼠标", categoryId = 1L),
                Item(id = 3L, name = "充电宝", categoryId = 1L)
            ),
            categories = listOf(Category(id = 1L, name = "数码")),
            locations = emptyList()
        )

        val intent = (result as AiCallResult.Success).data
        val criteria = AiSearchIntentNormalizer.toSearchCriteria("电脑输入设备有哪些存货", intent)

        assertEquals(setOf(1L, 2L), criteria.itemIds)
        assertEquals(setOf(1L), criteria.categoryIds)
        assertEquals("", criteria.text)
    }

    @Test
    fun normalize_infersItemIdsFromKnownItemNamesInSummary() {
        val result = AiSearchIntentNormalizer.normalize(
            result = AiCallResult.Success(
                AiSearchIntent(
                    intentType = AiSearchIntentType.BROWSE_CATEGORY,
                    categoryIds = listOf(1L),
                    status = AiSearchStatus.ALL,
                    ratingState = AiRatingState.ANY,
                    summary = "搜索电脑输入设备相关的存货物品（键盘、鼠标）"
                )
            ),
            query = "电脑输入设备有哪些存货",
            items = listOf(
                Item(id = 1L, name = "键盘", categoryId = 1L),
                Item(id = 2L, name = "鼠标", categoryId = 1L),
                Item(id = 3L, name = "充电宝", categoryId = 1L)
            ),
            categories = listOf(Category(id = 1L, name = "数码")),
            locations = emptyList()
        )

        val intent = (result as AiCallResult.Success).data
        val criteria = AiSearchIntentNormalizer.toSearchCriteria("电脑输入设备有哪些存货", intent)

        assertEquals(setOf(1L, 2L), criteria.itemIds)
        assertEquals("", criteria.text)
    }

    @Test
    fun normalize_dropsExpireFilterWhenQueryDoesNotMentionExpiry() {
        val result = AiSearchIntentNormalizer.normalize(
            result = AiCallResult.Success(
                AiSearchIntent(
                    intentType = AiSearchIntentType.FIND_ITEM_LOCATION,
                    semanticTerms = listOf("充电器"),
                    expiringWithinDays = 7,
                    status = AiSearchStatus.ALL,
                    ratingState = AiRatingState.ANY
                )
            ),
            query = "我的充电器在哪",
            items = listOf(Item(id = 1L, name = "充电器")),
            categories = emptyList(),
            locations = emptyList()
        )

        val intent = (result as AiCallResult.Success).data
        val criteria = AiSearchIntentNormalizer.toSearchCriteria("我的充电器在哪", intent)

        assertEquals(null, criteria.expiringWithinDays)
        assertEquals(listOf("充电器"), criteria.semanticTerms)
    }

    @Test
    fun normalize_infersItemIdsWhenSummaryMentionsShortNamesInsideLongItemNames() {
        val result = AiSearchIntentNormalizer.normalize(
            result = AiCallResult.Success(
                AiSearchIntent(
                    intentType = AiSearchIntentType.BROWSE_CATEGORY,
                    categoryIds = listOf(1L),
                    status = AiSearchStatus.ALL,
                    ratingState = AiRatingState.ANY,
                    summary = "搜索电脑输入设备的存货，包括键盘和鼠标"
                )
            ),
            query = "电脑输入设备有哪些存货",
            items = listOf(
                Item(id = 1L, name = "机械键盘", categoryId = 1L),
                Item(id = 2L, name = "罗技鼠标", categoryId = 1L),
                Item(id = 3L, name = "充电宝", categoryId = 1L)
            ),
            categories = listOf(Category(id = 1L, name = "数码")),
            locations = emptyList()
        )

        val intent = (result as AiCallResult.Success).data
        val criteria = AiSearchIntentNormalizer.toSearchCriteria("电脑输入设备有哪些存货", intent)

        assertEquals(setOf(1L, 2L), criteria.itemIds)
        assertEquals("", criteria.text)
    }

    @Test
    fun normalize_doesNotNarrowCategoryBrowseWithoutSemanticTerms() {
        val result = AiSearchIntentNormalizer.normalize(
            result = AiCallResult.Success(
                AiSearchIntent(
                    intentType = AiSearchIntentType.BROWSE_CATEGORY,
                    categoryIds = listOf(1L),
                    status = AiSearchStatus.ALL,
                    ratingState = AiRatingState.ANY,
                    summary = "查找数码分类下的所有存货"
                )
            ),
            query = "数码有哪些",
            items = listOf(
                Item(id = 1L, name = "键盘", categoryId = 1L),
                Item(id = 2L, name = "鼠标", categoryId = 1L),
                Item(id = 3L, name = "充电宝", categoryId = 1L)
            ),
            categories = listOf(Category(id = 1L, name = "数码")),
            locations = emptyList()
        )

        val intent = (result as AiCallResult.Success).data
        val criteria = AiSearchIntentNormalizer.toSearchCriteria("数码有哪些", intent)

        assertEquals(emptySet<Long>(), criteria.itemIds)
        assertEquals(setOf(1L), criteria.categoryIds)
    }

}
