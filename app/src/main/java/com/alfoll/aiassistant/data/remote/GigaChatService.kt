package com.alfoll.aiassistant.data.remote

import com.alfoll.aiassistant.data.local.MessageEntity
import java.util.UUID
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

object GigaChatService {

    private const val AUTH_BASE_URL = "https://ngw.devices.sberbank.ru:9443/"
    private const val API_BASE_URL = "https://gigachat.devices.sberbank.ru/"
    private const val MODEL = "GigaChat-2"
    private const val SCOPE = "GIGACHAT_API_PERS"
    private const val TOKEN_REFRESH_BUFFER_SECONDS = 60L

    private const val SYSTEM_PROMPT =
        "Ты полезный AI-ассистент. Отвечай кратко, по делу и на русском языке, если пользователь не попросил другое."

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    private val authApi: GigaChatAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GigaChatAuthApi::class.java)
    }

    private val chatApi: GigaChatApi by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GigaChatApi::class.java)
    }

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedExpiresAtSeconds: Long = 0L

    suspend fun generateReply(
        authKey: String,
        history: List<MessageEntity>
    ): GigaChatReplyResult {
        require(authKey.isNotBlank()) {
            "В проекте не задан GIGACHAT_AUTH_KEY"
        }

        val accessToken = getValidAccessToken(authKey)

        val response = chatApi.chatCompletions(
            authorization = "Bearer $accessToken",
            request = GigaChatChatRequest(
                model = MODEL,
                messages = buildRequestMessages(history)
            )
        )

        val replyText = response.choices
            .firstOrNull()
            ?.message
            ?.content
            ?.trim()
            ?.ifBlank { "GigaChat вернул пустой ответ." }
            ?: "GigaChat вернул пустой ответ."

        return GigaChatReplyResult(
            text = replyText,
            totalTokens = response.usage?.totalTokens ?: 0
        )
    }

    private suspend fun getValidAccessToken(authKey: String): String {
        val nowSeconds = System.currentTimeMillis() / 1000
        val existingToken = cachedAccessToken

        if (
            existingToken != null &&
            nowSeconds < cachedExpiresAtSeconds - TOKEN_REFRESH_BUFFER_SECONDS
        ) {
            return existingToken
        }

        val tokenResponse = authApi.getAccessToken(
            authorization = "Basic $authKey",
            requestId = UUID.randomUUID().toString()
        )

        cachedAccessToken = tokenResponse.accessToken
        cachedExpiresAtSeconds = tokenResponse.expiresAt

        return tokenResponse.accessToken
    }

    private fun buildRequestMessages(history: List<MessageEntity>): List<GigaChatMessageDto> {
        return buildList {
            add(
                GigaChatMessageDto(
                    role = "system",
                    content = SYSTEM_PROMPT
                )
            )

            history
                .sortedWith(compareBy<MessageEntity> { it.createdAt }.thenBy { it.id })
                .forEach { message ->
                    add(
                        GigaChatMessageDto(
                            role = if (message.isUser) "user" else "assistant",
                            content = message.text
                        )
                    )
                }
        }
    }

    private interface GigaChatAuthApi {

        @FormUrlEncoded
        @POST("api/v2/oauth")
        suspend fun getAccessToken(
            @Header("Authorization") authorization: String,
            @Header("RqUID") requestId: String,
            @Field("scope") scope: String = SCOPE
        ): GigaChatTokenResponse
    }

    private interface GigaChatApi {

        @POST("api/v1/chat/completions")
        suspend fun chatCompletions(
            @Header("Authorization") authorization: String,
            @Body request: GigaChatChatRequest
        ): GigaChatChatResponse
    }
}