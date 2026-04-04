package com.alfoll.aiassistant.ui.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alfoll.aiassistant.data.local.AppDatabase
import com.alfoll.aiassistant.data.local.MessageEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()

    private var currentChatId: String? = null
    private var currentChatTitle: String? = null
    private var chatJob: Job? = null
    private var messagesJob: Job? = null

    var chatTitle by mutableStateOf("Чат")
        private set

    var messages by mutableStateOf<List<MessageEntity>>(emptyList())
        private set

    var isGenerating by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(true)
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
        val chatId = currentChatId ?: return
        val cleanText = text.trim()

        if (cleanText.isBlank() || isGenerating) return

        viewModelScope.launch {
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

            isGenerating = true

            delay(700)

            messageDao.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    text = "Это тестовый ответ ассистента на сообщение: \"$cleanText\". Здесь позже будет ответ GigaChat.",
                    isUser = false,
                    createdAt = System.currentTimeMillis()
                )
            )

            isGenerating = false
        }
    }
}