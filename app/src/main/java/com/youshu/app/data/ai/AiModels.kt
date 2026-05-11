package com.youshu.app.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AiProviders {
    const val OPENAI_COMPATIBLE = "openai-compatible"
    const val OLLAMA = "ollama"

    val supported = listOf(OPENAI_COMPATIBLE, OLLAMA)
}

sealed interface AiCallResult<out T> {
    data class Success<T>(val data: T) : AiCallResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AiCallResult<Nothing>
}

@Serializable
data class AiItemSuggestion(
    val name: String? = null,
    val categoryName: String? = null,
    val locationName: String? = null,
    val quantity: Int? = null,
    val unit: String? = null,
    val price: Double? = null,
    val expireDate: String? = null,
    val note: String? = null,
    val confidence: Double? = null,
    val needsReview: List<String> = emptyList()
)

@Serializable
data class AiSearchIntent(
    val keywords: List<String> = emptyList(),
    val categoryName: String? = null,
    val locationName: String? = null,
    val status: AiSearchStatus? = null,
    val expiringWithinDays: Int? = null,
    val ratingState: AiRatingState? = null,
    val sort: AiSearchSort? = null,
    val summary: String? = null
)

@Serializable
enum class AiSearchStatus {
    @SerialName("in_use")
    IN_USE,

    @SerialName("used_up")
    USED_UP,

    @SerialName("all")
    ALL
}

@Serializable
enum class AiRatingState {
    @SerialName("pending")
    PENDING,

    @SerialName("reviewed")
    REVIEWED,

    @SerialName("any")
    ANY
}

@Serializable
enum class AiSearchSort {
    @SerialName("created_desc")
    CREATED_DESC,

    @SerialName("expire_asc")
    EXPIRE_ASC,

    @SerialName("name_asc")
    NAME_ASC
}
