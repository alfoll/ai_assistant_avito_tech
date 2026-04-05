package com.alfoll.aiassistant.ui.profile

import android.net.Uri

data class ProfileUiState(
    val displayName: String = "Не указано",
    val email: String = "Не указан",
    val phoneNumber: String = "Не указан",
    val photoUri: Uri? = null,
    val avatarBytes: ByteArray? = null,
    val avatarText: String = "U",
    val totalTokens: Int = 0,
    val isLoading: Boolean = true,
    val isUpdatingPhoto: Boolean = false,
    val errorMessage: String? = null
)