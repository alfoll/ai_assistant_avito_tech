package com.alfoll.aiassistant.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alfoll.aiassistant.ui.auth.AuthScreen
import com.alfoll.aiassistant.ui.chat.ChatScreen
import com.alfoll.aiassistant.ui.chatlist.ChatListScreen
import com.alfoll.aiassistant.ui.chatlist.ChatListViewModel
import com.alfoll.aiassistant.ui.profile.ProfileScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val rootNavController = rememberNavController()

    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.HOME
    } else {
        Routes.AUTH
    }

    NavHost(
        navController = rootNavController,
        startDestination = startDestination
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    rootNavController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.HOME) {
            AuthorizedArea(
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onLogout = {
                    rootNavController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthorizedArea(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val innerNavController = rememberNavController()
    val chatListViewModel: ChatListViewModel = viewModel()

    val currentBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AppDrawerContent(
                    currentRoute = currentRoute,
                    searchQuery = chatListViewModel.uiState.searchQuery,
                    onSearchQueryChange = chatListViewModel::onSearchQueryChange,
                    onSearchClick = {
                        chatListViewModel.onSearchClick()
                        innerNavController.navigate(Routes.CHAT_LIST) {
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    onCreateNewChat = {
                        chatListViewModel.createNewChat { newChatId ->
                            innerNavController.navigate(Routes.chat(newChatId))
                            scope.launch { drawerState.close() }
                        }
                    },
                    onOpenChatList = {
                        innerNavController.navigate(Routes.CHAT_LIST) {
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    onOpenProfile = {
                        innerNavController.navigate(Routes.PROFILE) {
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        NavHost(
            navController = innerNavController,
            startDestination = Routes.CHAT_LIST
        ) {
            composable(Routes.CHAT_LIST) {
                ChatListScreen(
                    viewModel = chatListViewModel,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onOpenChat = { chatId ->
                        innerNavController.navigate(Routes.chat(chatId))
                    }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onLogout = onLogout
                )
            }

            composable(
                route = Routes.CHAT,
                arguments = listOf(
                    navArgument("chatId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId").orEmpty()

                ChatScreen(
                    chatId = chatId,
                    onBack = { innerNavController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AppDrawerContent(
    currentRoute: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCreateNewChat: () -> Unit,
    onOpenChatList: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск по чатам") },
            singleLine = true
        )

        TextButton(onClick = onSearchClick) {
            Text("Найти")
        }

        NavigationDrawerItem(
            label = { Text("Новый чат") },
            selected = false,
            onClick = onCreateNewChat
        )

        NavigationDrawerItem(
            label = { Text("Главная / список чатов") },
            selected = currentRoute == Routes.CHAT_LIST,
            onClick = onOpenChatList
        )

        NavigationDrawerItem(
            label = { Text("Профиль") },
            selected = currentRoute == Routes.PROFILE,
            onClick = onOpenProfile
        )
    }
}