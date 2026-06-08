package com.youshu.app.data.search

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSearchParserTest {

    private val parser = LocalSearchParser()

    @Test
    fun parse_returnsStructuredPendingReviewCriteriaForPureStatusQuery() {
        val criteria = parser.parse("待评价的物品")

        assertEquals(SearchIntentKind.FILTER_ITEMS, criteria.kind)
        assertEquals(SearchItemStatus.USED_UP, criteria.status)
        assertEquals(SearchRatingState.PENDING, criteria.ratingState)
        assertEquals("", criteria.text)
    }

    @Test
    fun parse_returnsStructuredPendingReviewCriteriaForEnumStyleStatusQuery() {
        val criteria = parser.parse("状态=USED_UP，评价状态=PENDING")

        assertEquals(SearchIntentKind.FILTER_ITEMS, criteria.kind)
        assertEquals(SearchItemStatus.USED_UP, criteria.status)
        assertEquals(SearchRatingState.PENDING, criteria.ratingState)
    }

    @Test
    fun parse_keepsComplexLocationStatusQueryForAiParsing() {
        val criteria = parser.parse("书房里待评价的东西")

        assertEquals(SearchIntentKind.PLAIN_TEXT, criteria.kind)
        assertEquals("书房里待评价的东西", criteria.text)
    }
}
