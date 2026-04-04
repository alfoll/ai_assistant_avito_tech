package com.alfoll.aiassistant.ui.navigation

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val CHAT_LIST = "chat_list"
    const val PROFILE = "profile"
    const val CHAT = "chat/{chatId}"

    fun chat(chatId: String): String = "chat/$chatId"
}