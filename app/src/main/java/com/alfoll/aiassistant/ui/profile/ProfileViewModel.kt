package com.alfoll.aiassistant.ui.profile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alfoll.aiassistant.data.local.TokenUsageStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val tokenUsageStorage = TokenUsageStorage(application)

    var uiState by mutableStateOf(ProfileUiState())
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        val user = auth.currentUser

        if (user == null) {
            uiState = ProfileUiState(
                isLoading = false,
                errorMessage = "Пользователь не найден"
            )
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null
        )

        firestore.collection(PROFILES_COLLECTION)
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val avatarBlob = document.getBlob(FIELD_AVATAR_BYTES)

                uiState = ProfileUiState(
                    displayName = user.displayName ?: "Не указано",
                    email = user.email ?: "Не указан",
                    phoneNumber = user.phoneNumber ?: "Не указан",
                    photoUri = user.photoUrl,
                    avatarBytes = avatarBlob?.toBytes(),
                    avatarText = user.email?.firstOrNull()?.uppercase() ?: "U",
                    totalTokens = tokenUsageStorage.getTotalTokens(),
                    isLoading = false,
                    isUpdatingPhoto = false,
                    errorMessage = null
                )
            }
            .addOnFailureListener { error ->
                uiState = ProfileUiState(
                    displayName = user.displayName ?: "Не указано",
                    email = user.email ?: "Не указан",
                    phoneNumber = user.phoneNumber ?: "Не указан",
                    photoUri = user.photoUrl,
                    avatarBytes = null,
                    avatarText = user.email?.firstOrNull()?.uppercase() ?: "U",
                    totalTokens = tokenUsageStorage.getTotalTokens(),
                    isLoading = false,
                    isUpdatingPhoto = false,
                    errorMessage = error.localizedMessage ?: "Не удалось загрузить профиль"
                )
            }
    }

    fun onPhotoPicked(sourceUri: Uri?) {
        val user = auth.currentUser

        if (sourceUri == null) return

        if (user == null) {
            uiState = uiState.copy(
                errorMessage = "Пользователь не найден"
            )
            return
        }

        uiState = uiState.copy(
            isUpdatingPhoto = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val avatarBytes = withContext(Dispatchers.IO) {
                    prepareAvatarBytes(sourceUri)
                }

                val data = mapOf(
                    FIELD_AVATAR_BYTES to Blob.fromBytes(avatarBytes),
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                )

                firestore.collection(PROFILES_COLLECTION)
                    .document(user.uid)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        uiState = uiState.copy(
                            avatarBytes = avatarBytes,
                            isUpdatingPhoto = false,
                            errorMessage = null
                        )
                    }
                    .addOnFailureListener { error ->
                        uiState = uiState.copy(
                            isUpdatingPhoto = false,
                            errorMessage = error.localizedMessage
                                ?: "Не удалось сохранить фото в Firebase"
                        )
                    }
            } catch (error: Throwable) {
                uiState = uiState.copy(
                    isUpdatingPhoto = false,
                    errorMessage = error.message ?: "Не удалось обработать изображение"
                )
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    private fun prepareAvatarBytes(sourceUri: Uri): ByteArray {
        val resolver = getApplication<Application>().contentResolver

        val originalBitmap = resolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalStateException("Не удалось открыть выбранное изображение")

        val scaledBitmap = scaleBitmapIfNeeded(originalBitmap)

        var quality = 85
        var result: ByteArray

        do {
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            result = outputStream.toByteArray()
            quality -= 10
        } while (result.size > MAX_AVATAR_BYTES && quality >= 35)

        if (result.size > MAX_AVATAR_BYTES) {
            if (scaledBitmap !== originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()
            throw IllegalStateException("Изображение слишком большое. Выберите фото поменьше.")
        }

        if (scaledBitmap !== originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()

        return result
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_SIDE && bitmap.height <= MAX_SIDE) {
            return bitmap
        }

        val scale = minOf(
            MAX_SIDE.toFloat() / bitmap.width,
            MAX_SIDE.toFloat() / bitmap.height
        )

        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    companion object {
        private const val PROFILES_COLLECTION = "user_profiles"
        private const val FIELD_AVATAR_BYTES = "avatarBytes"
        private const val FIELD_UPDATED_AT = "updatedAt"

        private const val MAX_SIDE = 512
        private const val MAX_AVATAR_BYTES = 700 * 1024
    }
}