package com.alfoll.aiassistant.ui.chatlist

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alfoll.aiassistant.data.local.AppDatabase
import com.alfoll.aiassistant.data.local.ChatEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {

    private val chatDao = AppDatabase.getDatabase(application).chatDao()

    private var observeJob: Job? = null
    private var appliedQuery: String = ""

    var uiState by mutableStateOf(ChatListUiState())
        private set

    init {
        observeChats()
    }

    fun onSearchQueryChange(value: String) {
        uiState = uiState.copy(searchQuery = value)
    }

    fun onSearchClick() {
        appliedQuery = uiState.searchQuery.trim()
        observeChats()
    }

    fun createNewChat(onCreated: (String) -> Unit) {
        val newId = System.currentTimeMillis().toString()

        viewModelScope.launch {
            chatDao.insertChat(
                ChatEntity(
                    id = newId,
                    title = "Новый чат",
                    createdAt = System.currentTimeMillis()
                )
            )
            onCreated(newId)
        }
    }

    private fun observeChats() {
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            chatDao.observeChats(appliedQuery).collectLatest { chats ->
                uiState = uiState.copy(
                    displayedChats = chats.map {
                        ChatListItemModel(
                            id = it.id,
                            title = it.title
                        )
                    },
                    isLoading = false
                )
            }
        }
    }
}