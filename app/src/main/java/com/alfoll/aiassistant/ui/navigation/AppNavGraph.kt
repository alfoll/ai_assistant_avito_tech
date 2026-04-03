package com.alfoll.aiassistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alfoll.aiassistant.ui.auth.AuthScreen
import com.alfoll.aiassistant.ui.chat.ChatScreen
import com.alfoll.aiassistant.ui.chatlist.ChatListScreen
import com.alfoll.aiassistant.ui.profile.ProfileScreen


@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            AuthScreen()
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen()
        }
        composable(Routes.CHAT) {
            ChatScreen()
        }
        composable(Routes.PROFILE) {
            ProfileScreen()
        }
    }
}