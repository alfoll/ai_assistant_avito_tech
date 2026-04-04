package com.alfoll.aiassistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alfoll.aiassistant.ui.auth.AuthScreen
import com.alfoll.aiassistant.ui.chat.ChatScreen
import com.alfoll.aiassistant.ui.chatlist.ChatListScreen
import com.alfoll.aiassistant.ui.profile.ProfileScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.CHAT_LIST
    } else {
        Routes.AUTH
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.CHAT_LIST) {
                        popUpTo(Routes.AUTH) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.CHAT) {
            ChatScreen()
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}