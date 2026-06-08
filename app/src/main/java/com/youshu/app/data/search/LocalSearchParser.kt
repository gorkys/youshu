package com.youshu.app.data.search

class LocalSearchParser {

    fun parse(query: String): SearchCriteria {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            return SearchCriteria()
        }
        val statusCriteria = parseStatusCriteria(normalized)
        if (statusCriteria != null) return statusCriteria

        return SearchCriteria(
            kind = SearchIntentKind.PLAIN_TEXT,
            text = normalized
        )
    }

    private fun parseStatusCriteria(query: String): SearchCriteria? {
        val criteria = when {
            containsAny(query, pendingReviewWords) -> SearchCriteria(
                kind = SearchIntentKind.FILTER_ITEMS,
                status = SearchItemStatus.USED_UP,
                ratingState = SearchRatingState.PENDING,
                summary = "筛选待评价的物品"
            )

            containsAny(query, reviewedWords) -> SearchCriteria(
                kind = SearchIntentKind.FILTER_ITEMS,
                status = SearchItemStatus.USED_UP,
                ratingState = SearchRatingState.REVIEWED,
                summary = "筛选已评价的物品"
            )

            containsAny(query, usedUpWords) -> SearchCriteria(
                kind = SearchIntentKind.FILTER_ITEMS,
                status = SearchItemStatus.USED_UP,
                summary = "筛选已用完的物品"
            )

            containsAny(query, expiringWords) -> SearchCriteria(
                kind = SearchIntentKind.FILTER_ITEMS,
                status = SearchItemStatus.IN_USE,
                expiringWithinDays = 7,
                sort = SearchSort.EXPIRE_ASC,
                summary = "筛选 7 天内临期的物品"
            )

            else -> null
        }
        return criteria?.takeIf { query.isPureStatusQuery() }
    }

    private fun containsAny(query: String, words: List<String>): Boolean {
        val upperQuery = query.uppercase()
        return words.any { word ->
            query.contains(word) || upperQuery.contains(word.uppercase())
        }
    }

    private fun String.isPureStatusQuery(): Boolean {
        var value = this.uppercase()
        (pendingReviewWords + reviewedWords + usedUpWords + expiringWords + genericWords)
            .sortedByDescending { it.length }
            .forEach { word ->
                value = value.replace(word.uppercase(), "")
            }
        value = value.replace(Regex("[\\s,，。.!！?？、：:；;（）()【】\\[\\]{}=]+"), "")
        return value.trim().isBlank()
    }

    private companion object {
        val pendingReviewWords = listOf("待评价", "未评价", "没评价", "还没评价", "待评分", "未评分", "没评分", "PENDING", "PENDING_REVIEW")
        val reviewedWords = listOf("已评价", "评价过", "已评分", "评分过", "REVIEWED")
        val usedUpWords = listOf("已用完", "用完", "用掉", "耗尽", "空了", "USED_UP")
        val expiringWords = listOf("快过期", "即将过期", "临期", "快到期", "即将到期")
        val genericWords = listOf(
            "的",
            "物品",
            "东西",
            "商品",
            "全部",
            "所有",
            "查找",
            "搜索",
            "筛选",
            "查看",
            "一下",
            "帮我",
            "请问",
            "状态",
            "评价状态",
            "为",
            "且",
            "和",
            "以及"
        )
    }
}
