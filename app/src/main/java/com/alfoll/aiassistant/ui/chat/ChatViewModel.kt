package com.alfoll.aiassistant.ui.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alfoll.aiassistant.R
import com.alfoll.aiassistant.data.local.AppDatabase
import com.alfoll.aiassistant.data.local.MessageEntity
import com.alfoll.aiassistant.data.local.TokenUsageStorage
import com.alfoll.aiassistant.data.remote.GigaChatService
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val tokenUsageStorage = TokenUsageStorage(application)

    private var currentChatId: String? = null
    private var currentChatTitle: String? = null
    private var chatJob: Job? = null
    private var messagesJob: Job? = null
    private var failedMessageText: String? = null

    var chatTitle by mutableStateOf("Чат")
        private set

    var messages by mutableStateOf<List<MessageEntity>>(emptyList())
        private set

    var isGenerating by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var showRetryAction by mutableStateOf(false)
        private set

    fun start(chatId: String) {
        if (currentChatId == chatId) return

        currentChatId = chatId
        isLoading = true

        chatJob?.cancel()
        messagesJob?.cancel()

        chatJob = viewModelScope.launch {
            chatDao.observeChat(chatId).collectLatest { chat ->
                currentChatTitle = chat?.title
                chatTitle = chat?.title ?: "Чат"
            }
        }

        messagesJob = viewModelScope.launch {
            messageDao.observeMessages(chatId).collectLatest { dbMessages ->
                messages = dbMessages
                isLoading = false
            }
        }
    }

    fun sendMessage(text: String) {
        sendMessageInternal(
            text = text,
            saveUserMessage = true
        )
    }

    fun retryLastMessage() {
        val text = failedMessageText ?: return
        sendMessageInternal(
            text = text,
            saveUserMessage = false
        )
    }

    fun clearError() {
        errorMessage = null
        showRetryAction = false
    }

    private fun sendMessageInternal(
        text: String,
        saveUserMessage: Boolean
    ) {
        val chatId = currentChatId ?: return
        val cleanText = text.trim()

        if (cleanText.isBlank() || isGenerating) return

        viewModelScope.launch {
            errorMessage = null
            showRetryAction = false

            if (saveUserMessage) {
                if (currentChatTitle == "Новый чат") {
                    val generatedTitle = cleanText.take(30)
                    chatDao.updateChatTitle(chatId, generatedTitle)
                }

                messageDao.insertMessage(
                    MessageEntity(
                        chatId = chatId,
                        text = cleanText,
                        isUser = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            isGenerating = true

            try {
                val authKey = getApplication<Application>().getString(
                    R.string.gigachat_auth_key
                )

                val history = messageDao.getMessages(chatId)

                val replyResult = GigaChatService.generateReply(
                    authKey = authKey,
                    history = history
                )

                tokenUsageStorage.addTokens(replyResult.totalTokens)

                messageDao.insertMessage(
                    MessageEntity(
                        chatId = chatId,
                        text = replyResult.text,
                        isUser = false,
                        createdAt = System.currentTimeMillis()
                    )
                )

                failedMessageText = null
            } catch (error: Throwable) {
                failedMessageText = cleanText
                errorMessage = mapChatError(error)
                showRetryAction = true
            } finally {
                isGenerating = false
            }
        }
    }

    private fun mapChatError(error: Throwable): String {
        return when (error) {
            is IllegalStateException -> {
                error.message ?: "Не настроен ключ GigaChat"
            }

            is IOException -> {
                "Не удалось связаться с GigaChat. Проверьте интернет и попробуйте снова."
            }

            is HttpException -> {
                "GigaChat вернул ошибку ${error.code()}. Попробуйте ещё раз."
            }

            else -> {
                error.message ?: "Не удалось получить ответ от GigaChat."
            }
        }
    }
}