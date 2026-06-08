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
        locations: List<Location>,
        items: List<Item>
    ): String {
        return """
            你是家庭物品管理 App「有数」里的库存搜索 Agent。
            你的任务不是复述用户问题，而是基于候选物品、候选分类、候选位置，输出 App 可以直接执行的搜索计划 JSON。
            只返回 JSON，不要输出 Markdown。
            今天日期：${today()}
            用户搜索：$query

            候选物品：
            ${formatItemCandidates(items, categories, locations)}

            候选分类：
            ${formatCategoryCandidates(categories)}

            候选位置：
            ${formatLocationCandidates(locations)}

            返回字段：
            {
              "intentType": "find_item_location | filter_items | browse_location | browse_category | unknown",
              "itemIds": [所有符合用户语义的候选物品 id，没有则 []],
              "categoryIds": [匹配置信度最高的候选分类 id，没有则 []],
              "locationIds": [匹配置信度最高的候选位置 id，没有则 []],
              "itemName": "无法匹配候选物品 id 时，用户提到的物品名或 null",
              "keywords": ["只放必须参与文本过滤且无法归入其它字段的关键词"],
              "semanticTerms": ["用户表达的抽象物品概念，仅用于解释和兜底，例如电脑输入设备、早餐食品、清洁用品"],
              "categoryName": "匹配到的已有分类名或 null",
              "locationName": "匹配到的已有位置名或 null",
              "status": "in_use | used_up | all",
              "expiringWithinDays": 7,
              "ratingState": "pending | reviewed | any",
              "sort": "created_desc | expire_asc | name_asc",
              "confidence": 0.0 到 1.0,
              "summary": "一句话说明你理解的搜索条件"
            }

            约束：
            执行规则：
            1. 必须先读候选物品，再决定 itemIds。用户问抽象概念时，例如“药物、衣物、电脑输入设备、清洁用品、早餐”，你要根据候选物品的 name/category/location/status 判断哪些物品符合，并把这些物品 id 放入 itemIds。
            2. 只要用户语义可以落到候选物品，就优先返回 itemIds；不要只把理解写到 summary 或 semanticTerms。
            3. itemIds 表示最终命中的候选物品集合；一旦返回 itemIds，就必须包含所有符合用户语义的候选物品，不要依赖 summary/keywords 再解释。
            4. itemIds/categoryIds/locationIds 必须来自候选列表；不能编造 id。
            5. summary 只是展示文案，不能替代 itemIds/categoryIds/locationIds/status/ratingState。
            6. 用户问“某物在哪/在哪里/放哪/位置”时，intentType=find_item_location，并返回最相关 itemIds。
            7. 用户问“某位置有什么/有哪些”时，intentType=browse_location，并返回匹配位置 locationIds；如果位置范围内还指定了抽象物品，也要返回符合语义的 itemIds。
            8. 用户问“某分类有哪些/有什么”时，intentType=browse_category，并返回匹配分类 categoryIds；如果分类范围内还指定了抽象物品，也要返回符合语义的 itemIds。
            9. 用户问“待评价/已评价/已用完/快过期”等状态条件时，intentType=filter_items，只填写状态、评价和效期字段，不要把“物品/东西”当作关键词。
            10. “快过期/即将过期/临期”默认 expiringWithinDays = 7，sort = expire_asc。
            11. “待评价/没评价/未评价”表示 status = used_up 且 ratingState = pending。
            12. “已评价”表示 status = used_up 且 ratingState = reviewed。
            13. “用完/已用完”表示 status = used_up。
            14. 未提到状态时 status = all，ratingState = any。
            15. “有哪些/有什么/都有什么/查找/搜索/物品/东西/我的/在哪/在哪儿/在哪里/哪儿/哪里/放哪/位置”这类意图词不要放入 keywords。
            16. 如果用户只是问“数码有哪些”，返回 categoryIds 即可，不要额外缩窄 itemIds。
            17. 如果用户问“电脑输入设备有哪些存货”，应从候选物品中挑选键盘、鼠标、触控板等实际存在的 itemIds，而不是只返回“数码分类”。
            18. 如果用户问“有哪些药物/衣物/食品”，应从候选物品中挑选符合语义的实际 itemIds；如果候选分类已能完整覆盖该语义，也可以同时返回 categoryIds。
        """.trimIndent()
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

    fun buildSearchRepairPrompt(
        query: String,
        failedIntent: AiSearchIntent,
        matchedCount: Int,
        categories: List<Category>,
        locations: List<Location>,
        items: List<Item>
    ): String {
        return """
            你是家庭物品管理 App「有数」里的库存搜索 Agent。
            上一次搜索计划没有给出可用结果，或只返回了过宽的范围。请重新基于候选库存输出可执行 JSON。
            只返回 JSON，不要输出 Markdown。

            用户搜索：$query
            上一次计划：$failedIntent
            上一次本地匹配数量：$matchedCount

            候选物品：
            ${formatItemCandidates(items, categories, locations)}

            候选分类：
            ${formatCategoryCandidates(categories)}

            候选位置：
            ${formatLocationCandidates(locations)}

            返回字段与上一轮完全一致：
            {
              "intentType": "find_item_location | filter_items | browse_location | browse_category | unknown",
              "itemIds": [所有符合用户语义的候选物品 id，没有则 []],
              "categoryIds": [匹配候选分类 id，没有则 []],
              "locationIds": [匹配候选位置 id，没有则 []],
              "itemName": null,
              "keywords": [],
              "semanticTerms": [],
              "categoryName": null,
              "locationName": null,
              "status": "in_use | used_up | all",
              "expiringWithinDays": null,
              "ratingState": "pending | reviewed | any",
              "sort": "created_desc | expire_asc | name_asc",
              "confidence": 0.0,
              "summary": "一句话说明修正后的搜索计划"
            }

            修复要求：
            1. 如果用户问题包含抽象物品语义，例如药物、衣物、食品、电脑输入设备、清洁用品，必须直接从候选物品中挑选符合语义的 itemIds。
            2. 不要只返回一个大分类来代替抽象语义过滤，除非用户确实只问这个分类有哪些。
            3. 如果候选里没有符合语义的物品，itemIds 返回 []，summary 说明没有找到符合语义的候选。
            4. 如果 summary/semanticTerms 写出了候选物品名，这些物品必须同步进入 itemIds。
            5. itemIds/categoryIds/locationIds 必须来自候选列表，不能编造。
        """.trimIndent()
    }

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun formatNames(names: List<String>): String {
        return names.map(LegacyTextNormalizer::normalizeName).distinct().joinToString()
    }

    private fun formatItemCandidates(
        items: List<Item>,
        categories: List<Category>,
        locations: List<Location>
    ): String {
        if (items.isEmpty()) return "无"
        val categoryNames = categories.associate { it.id to LegacyTextNormalizer.normalizeName(it.name) }
        val locationNames = locations.associate { it.id to LegacyTextNormalizer.normalizeName(it.name) }
        return items.take(120).joinToString("\n") { item ->
            val category = item.categoryId?.let(categoryNames::get).orEmpty()
            val location = item.locationId?.let(locationNames::get).orEmpty()
            val status = when (item.status) {
                Item.STATUS_USED_UP -> "used_up"
                Item.STATUS_DISCARDED -> "discarded"
                else -> "in_use"
            }
            "- id=${item.id}, name=${LegacyTextNormalizer.normalizeName(item.name)}, category=$category, location=$location, status=$status"
        }
    }

    private fun formatCategoryCandidates(categories: List<Category>): String {
        if (categories.isEmpty()) return "无"
        return categories.joinToString("\n") { category ->
            "- id=${category.id}, name=${LegacyTextNormalizer.normalizeName(category.name)}"
        }
    }

    private fun formatLocationCandidates(locations: List<Location>): String {
        if (locations.isEmpty()) return "无"
        val locationNames = locations.associate { it.id to LegacyTextNormalizer.normalizeName(it.name) }
        return locations.joinToString("\n") { location ->
            val parent = location.parentId?.let(locationNames::get)
            val path = if (parent == null) {
                LegacyTextNormalizer.normalizeName(location.name)
            } else {
                "$parent/${LegacyTextNormalizer.normalizeName(location.name)}"
            }
            "- id=${location.id}, name=${LegacyTextNormalizer.normalizeName(location.name)}, path=$path"
        }
    }
}
