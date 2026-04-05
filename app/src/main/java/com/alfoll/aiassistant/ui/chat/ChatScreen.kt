package com.alfoll.aiassistant.ui.chat

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alfoll.aiassistant.data.local.MessageEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by rememberSaveable(chatId) { mutableStateOf("") }

    LaunchedEffect(chatId) {
        viewModel.start(chatId)
    }

    LaunchedEffect(viewModel.errorMessage, viewModel.showRetryAction) {
        val message = viewModel.errorMessage ?: return@LaunchedEffect
        val canRetry = viewModel.showRetryAction

        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (canRetry) "Повторить" else null,
            withDismissAction = true
        )

        viewModel.clearError()

        if (result == SnackbarResult.ActionPerformed && canRetry) {
            viewModel.retryLastMessage()
        }
    }

    fun shareMessage(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        context.startActivity(
            Intent.createChooser(intent, "Поделиться ответом")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.chatTitle) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Введите сообщение") },
                    enabled = !viewModel.isGenerating,
                    minLines = 1,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isBlank() || viewModel.isGenerating) return@Button

                        viewModel.sendMessage(text)
                        inputText = ""
                    },
                    enabled = !viewModel.isGenerating
                ) {
                    if (viewModel.isGenerating) {
                        CircularProgressIndicator()
                    } else {
                        Text("Отправить")
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            viewModel.messages.isEmpty() && !viewModel.isGenerating -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Сообщений пока нет")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = viewModel.messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageCard(
                            message = message,
                            onShare = { shareMessage(message.text) }
                        )
                    }

                    if (viewModel.isGenerating) {
                        item {
                            Text("Ассистент печатает...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageCard(
    message: MessageEntity,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(message.text)

                if (!message.isUser) {
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onShare) {
                        Text("Поделиться")
                    }
                }
            }
        }
    }
}