package com.alfoll.aiassistant.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GigaChatTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_at")
    val expiresAt: Long
)

@Serializable
data class GigaChatChatRequest(
    val model: String,
    val messages: List<GigaChatMessageDto>
)

@Serializable
data class GigaChatMessageDto(
    val role: String,
    val content: String = ""
)

@Serializable
data class GigaChatChatResponse(
    val choices: List<GigaChatChoiceDto> = emptyList(),
    val usage: GigaChatUsageDto? = null
)

@Serializable
data class GigaChatChoiceDto(
    val message: GigaChatMessageDto
)

@Serializable
data class GigaChatUsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

data class GigaChatReplyResult(
    val text: String,
    val totalTokens: Int
)