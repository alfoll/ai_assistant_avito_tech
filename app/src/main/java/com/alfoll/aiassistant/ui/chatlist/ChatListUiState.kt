package com.alfoll.aiassistant.ui.chatlist

data class ChatListItemModel(
    val id: String,
    val title: String
)

data class ChatListUiState(
    val searchQuery: String = "",
    val displayedChats: List<ChatListItemModel> = emptyList(),
    val isLoading: Boolean = true
)