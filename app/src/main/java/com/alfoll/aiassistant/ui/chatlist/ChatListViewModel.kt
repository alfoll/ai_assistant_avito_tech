package com.alfoll.aiassistant.ui.chatlist

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.alfoll.aiassistant.data.local.AppDatabase
import com.alfoll.aiassistant.data.local.ChatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {

    private val chatDao = AppDatabase.getDatabase(application).chatDao()

    private val appliedQueryFlow = MutableStateFlow("")

    var uiState by mutableStateOf(ChatListUiState())
        private set

    val pagedChatsFlow: Flow<PagingData<ChatListItemModel>> =
        appliedQueryFlow
            .flatMapLatest { query ->
                Pager(
                    config = PagingConfig(
                        pageSize = 20,
                        prefetchDistance = 5,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        chatDao.pagingSource(query)
                    }
                ).flow
            }
            .map { pagingData ->
                pagingData.map { chat: ChatEntity ->
                    ChatListItemModel(
                        id = chat.id,
                        title = chat.title
                    )
                }
            }
            .cachedIn(viewModelScope)

    fun onSearchQueryChange(value: String) {
        uiState = uiState.copy(searchQuery = value)
    }

    fun onSearchClick() {
        val newQuery = uiState.searchQuery.trim()

        if (newQuery == uiState.appliedSearchQuery) return

        uiState = uiState.copy(appliedSearchQuery = newQuery)
        appliedQueryFlow.value = newQuery
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
}