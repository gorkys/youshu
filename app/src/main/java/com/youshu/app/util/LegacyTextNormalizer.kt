package com.youshu.app.util

/**
 * Keeps compatibility with early builds that wrote mojibake names into the database.
 * Business/UI/AI code should use normalized names and avoid duplicating these legacy values.
 */
object LegacyTextNormalizer {
    const val HOME_ROOT_NAME = "我的家"

    val categoryReplacements = listOf(
        "椋熷搧" to "食品",
        "鑽搧" to "药品",
        "鏃ョ敤鍝�" to "日用品",
        "鏃ョ敤鍝?" to "日用品",
        "鏁扮爜" to "数码",
        "琛ｇ墿" to "衣物",
        "鏂囧叿" to "文具",
        "宸ュ叿" to "工具",
        "鍏朵粬" to "其他"
    )

    val locationReplacements = listOf(
        "鎴戠殑瀹�" to HOME_ROOT_NAME,
        "鎴戠殑瀹?" to HOME_ROOT_NAME,
        "鍘ㄦ埧" to "厨房",
        "鍐扮" to "冰箱",
        "姗辨煖" to "橱柜",
        "鍙伴潰" to "台面",
        "瀹㈠巺" to "客厅",
        "鐢佃鏌�" to "电视柜",
        "鐢佃鏌?" to "电视柜",
        "鑼跺嚑" to "茶几",
        "涔︽灦" to "书架",
        "鍗у" to "卧室",
        "琛ｆ煖" to "衣柜",
        "搴婂ご鏌�" to "床头柜",
        "搴婂ご鏌?" to "床头柜",
        "姊冲鍙�" to "梳妆台",
        "姊冲鍙?" to "梳妆台",
        "鍗敓闂�" to "卫生间",
        "鍗敓闂?" to "卫生间",
        "娲楁墜鍙�" to "洗手台",
        "娲楁墜鍙?" to "洗手台",
        "娣嬫荡闂�" to "淋浴间",
        "娣嬫荡闂?" to "淋浴间",
        "闃冲彴" to "阳台",
        "鍌ㄧ墿鏌�" to "储物柜",
        "鍌ㄧ墿鏌?" to "储物柜",
        "涔︽埧" to "书房",
        "涔︽" to "书桌",
        "涔︽煖" to "书柜",
        "鍌ㄧ墿闂�" to "储物间",
        "鍌ㄧ墿闂?" to "储物间",
        "鏀剁撼鏋�" to "收纳架",
        "鏀剁撼鏋?" to "收纳架"
    )

    val legacyUnitNames = listOf("浠�", "浠?", "娑?", "")

    private val nameReplacements = (categoryReplacements + locationReplacements).toMap()

    fun normalizeName(name: String): String {
        val trimmed = name.trim()
        return nameReplacements[trimmed] ?: trimmed
    }

    fun aliases(name: String): Set<String> {
        val trimmed = name.trim()
        val canonical = normalizeName(trimmed)
        val result = linkedSetOf(canonical, trimmed)
        nameReplacements.forEach { (legacy, normalized) ->
            if (normalized == canonical) {
                result += legacy
            }
        }
        return result.filter { it.isNotBlank() }.toSet()
    }

    fun namesMatch(actual: String?, requested: String?): Boolean {
        val actualValue = actual?.trim()?.takeIf { it.isNotBlank() } ?: return false
        val requestedValue = requested?.trim()?.takeIf { it.isNotBlank() } ?: return false
        val actualAliases = aliases(actualValue)
        val requestedAliases = aliases(requestedValue)
        return actualAliases.any { actualAlias ->
            requestedAliases.any { requestedAlias ->
                actualAlias.equals(requestedAlias, ignoreCase = true) ||
                    actualAlias.contains(requestedAlias, ignoreCase = true) ||
                    requestedAlias.contains(actualAlias, ignoreCase = true)
            }
        }
    }
}
