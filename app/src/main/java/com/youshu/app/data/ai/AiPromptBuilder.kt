package com.youshu.app.data.ai

import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.local.entity.Location
import com.youshu.app.util.LegacyTextNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AiPromptBuilder {

    fun buildItemRecognitionPrompt(
        categories: List<Category>,
        locations: List<Location>,
        userHint: String?
    ): String {
        return """
            你是家庭物品管理 App「有数」的录入助手。
            请根据图片和用户补充信息识别一个待收纳物品，只返回 JSON，不要输出 Markdown。
            今天日期：${today()}
            已有分类：${formatNames(categories.map { it.name })}
            已有位置：${formatNames(locations.map { it.name })}
            用户补充：${userHint.orEmpty().ifBlank { "无" }}

            返回字段：
            {
              "name": "必须填写。图片里最主要的单个物品名称，尽量简短，例如鼠标、键盘、牛奶",
              "categoryName": "优先从已有分类中选择，无法判断则为空",
              "locationName": "优先从已有位置中选择，无法判断则为空",
              "quantity": 1,
              "unit": "件/袋/瓶/盒等",
              "price": null,
              "expireDate": "yyyy-MM-dd 或 null",
              "note": "品牌、规格、保质期依据等补充说明",
              "confidence": 0.0 到 1.0,
              "needsReview": ["低置信度或需要用户确认的字段"]
            }

            约束：
            1. name 是核心字段；只要能看出主物体，就必须返回名称。
            2. 分类和位置不确定时用 null，不要编造。
            3. 有效期必须是 yyyy-MM-dd，无法确定具体日期则返回 null。
            4. 分类和位置优先匹配已有名称，不要自行创造复杂层级。
            5. 如果无法识别图片主体，name 返回 null 且 confidence 返回 0。
            6. 例如图片主体是鼠标时，即使不知道品牌，也应返回 {"name":"鼠标","categoryName":"数码","quantity":1,"unit":"件","confidence":0.6}。
        """.trimIndent()
    }

    fun buildSearchIntentPrompt(
        query: String,
        categories: List<Category>,
        locations: List<Location>
    ): String {
        return """
            你是家庭物品管理 App「有数」的搜索解析器。
            请把用户自然语言搜索解析为 JSON 条件，只返回 JSON，不要输出 Markdown。
            今天日期：${today()}
            用户搜索：$query
            已有分类：${formatNames(categories.map { it.name })}
            已有位置：${formatNames(locations.map { it.name })}

            返回字段：
            {
              "keywords": ["无法归入分类/位置/状态的关键词"],
              "categoryName": "匹配到的分类名或 null",
              "locationName": "匹配到的位置名或 null",
              "status": "in_use | used_up | all",
              "expiringWithinDays": 7,
              "ratingState": "pending | reviewed | any",
              "sort": "created_desc | expire_asc | name_asc",
              "summary": "一句话说明你理解的搜索条件"
            }

            约束：
            1. “快过期/即将过期/临期”默认 expiringWithinDays = 7，sort = expire_asc。
            2. “待评价/没评价”表示 status = used_up 且 ratingState = pending。
            3. “已评价”表示 status = used_up 且 ratingState = reviewed。
            4. “用完/已用完”表示 status = used_up。
            5. 未提到状态时 status = in_use。
            6. 无法确定分类或位置时返回 null，不要编造。
            7. “有哪些/有什么/都有什么/查找/搜索/物品/东西”这类泛化词不要放入 keywords。
        """.trimIndent()
    }

    fun buildSearchFallbackSummary(query: String, resultCount: Int): String {
        return "按关键词「$query」找到 $resultCount 件匹配物品。"
    }

    fun buildItemDigest(item: ItemDetail): String {
        val entity = item.item
        return listOf(
            entity.name,
            item.categoryName.orEmpty(),
            item.locationName.orEmpty(),
            entity.note
        ).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun formatNames(names: List<String>): String {
        return names.map(LegacyTextNormalizer::normalizeName).distinct().joinToString()
    }
}
