package com.youshu.app.data.ai

import com.youshu.app.BuildConfig
import com.youshu.app.data.local.entity.AiModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiCompatibleClient @Inject constructor() : AiClient {

    private val client = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun recognizeItem(
        config: AiModelConfig,
        prompt: String,
        imageBase64List: List<String>
    ): AiCallResult<AiItemSuggestion> {
        if (imageBase64List.isNotEmpty() && !config.supportsVision) {
            return AiCallResult.Error("当前模型未开启图片识别能力")
        }
        return chatJson(config, buildVisionMessages(prompt, imageBase64List)).mapJson()
    }

    override suspend fun parseSearchIntent(
        config: AiModelConfig,
        prompt: String
    ): AiCallResult<AiSearchIntent> {
        return chatJson(config, buildTextMessages(prompt)).mapJson()
    }

    override suspend fun testConnection(config: AiModelConfig): AiCallResult<Unit> {
        return when (val result = chatJson(config, buildTextMessages("Return exactly this JSON object and nothing else: {\"ok\":true}"))) {
            is AiCallResult.Success -> AiCallResult.Success(Unit)
            is AiCallResult.Error -> result
        }
    }

    private suspend fun chatJson(
        config: AiModelConfig,
        messages: List<OpenAiMessage>
    ): AiCallResult<String> = withContext(Dispatchers.IO) {
        if (config.endpoint.isBlank() || config.modelName.isBlank()) {
            return@withContext AiCallResult.Error("AI 模型配置不完整")
        }
        if (config.apiKey.isBlank() && config.provider == AiProviders.OPENAI_COMPATIBLE) {
            return@withContext AiCallResult.Error("请先配置 API Key")
        }

        val requestBody = OpenAiChatRequest(
            model = config.modelName,
            messages = messages,
            temperature = 0.1
        )
        val request = Request.Builder()
            .url("${config.endpoint.trimEnd('/')}/chat/completions")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "YouShu-Android/${BuildConfig.VERSION_NAME}")
            .header("HTTP-Referer", "https://github.com/gorkys/youshu")
            .header("X-Title", "YouShu")
            .apply {
                if (config.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
            }
            .post(json.encodeToString(requestBody).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AiCallResult.Error(buildHttpErrorMessage(response.code, body, config.apiKey))
                }
                val parsed = json.decodeFromString<OpenAiChatResponse>(body)
                val content = parsed.choices.firstOrNull()?.message?.content.orEmpty()
                if (content.isBlank()) {
                    AiCallResult.Error("AI 未返回有效内容")
                } else {
                    AiCallResult.Success(extractJson(content))
                }
            }
        }.getOrElse { throwable ->
            AiCallResult.Error(buildCallFailureMessage(throwable), throwable)
        }
    }

    private fun buildTextMessages(prompt: String): List<OpenAiMessage> {
        return listOf(
            OpenAiMessage(
                role = "user",
                content = buildJsonArray {
                    add(textPart(prompt))
                }
            )
        )
    }

    private fun buildVisionMessages(prompt: String, imageBase64List: List<String>): List<OpenAiMessage> {
        val content = buildJsonArray {
            add(textPart(prompt))
            imageBase64List.forEach { base64 ->
                add(imagePart(base64))
            }
        }
        return listOf(OpenAiMessage(role = "user", content = content))
    }

    private fun textPart(text: String) = buildJsonObject {
        put("type", JsonPrimitive("text"))
        put("text", JsonPrimitive(text))
    }

    private fun imagePart(base64: String) = buildJsonObject {
        put("type", JsonPrimitive("image_url"))
        put(
            "image_url",
            buildJsonObject {
                put("url", JsonPrimitive("data:image/jpeg;base64,$base64"))
            }
        )
    }

    private inline fun <reified T> AiCallResult<String>.mapJson(): AiCallResult<T> {
        return when (this) {
            is AiCallResult.Success -> runCatching {
                json.decodeFromString<T>(data)
            }.fold(
                onSuccess = { AiCallResult.Success(it) },
                onFailure = {
                    AiCallResult.Error("AI 返回格式无法解析：${data.toSafeExcerpt()}", it)
                }
            )

            is AiCallResult.Error -> this
        }
    }

    private fun extractJson(content: String): String {
        val trimmed = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun buildHttpErrorMessage(code: Int, body: String, apiKey: String): String {
        val detail = body
            .replace(apiKey, "***")
            .toSafeExcerpt()
            .takeIf { it.isNotBlank() }
        return if (detail == null) {
            "AI 调用失败：HTTP $code"
        } else {
            "AI 调用失败：HTTP $code，$detail"
        }
    }

    private fun String.toSafeExcerpt(maxLength: Int = 260): String {
        return replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLength)
    }

    private fun buildCallFailureMessage(throwable: Throwable): String {
        val detail = throwable.message
            ?.toSafeExcerpt(160)
            ?.takeIf { it.isNotBlank() }
        return when (throwable) {
            is IOException -> if (detail == null) {
                "AI 调用失败：网络连接异常"
            } else {
                "AI 调用失败：网络连接异常：$detail"
            }

            else -> "AI 调用失败：${detail ?: "未知错误"}"
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

@Serializable
private data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double
)

@Serializable
private data class OpenAiMessage(
    val role: String,
    val content: kotlinx.serialization.json.JsonElement
)

@Serializable
private data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList()
)

@Serializable
private data class OpenAiChoice(
    val message: OpenAiResponseMessage
)

@Serializable
private data class OpenAiResponseMessage(
    val content: String? = null
)
