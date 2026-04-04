package com.alfoll.aiassistant.ui.auth

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var uiState by mutableStateOf(
        AuthUiState(
            isAuthorized = auth.currentUser != null
        )
    )
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(
            email = value,
            emailError = validateEmail(value),
            errorMessage = null,
            showRetryAction = false
        )
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(
            password = value,
            passwordError = validatePassword(value),
            errorMessage = null,
            showRetryAction = false
        )

        if (!uiState.isLoginMode && uiState.confirmPassword.isNotBlank()) {
            uiState = uiState.copy(
                confirmPasswordError = validateConfirmPassword(
                    password = value,
                    confirmPassword = uiState.confirmPassword
                )
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        uiState = uiState.copy(
            confirmPassword = value,
            confirmPasswordError = validateConfirmPassword(
                password = uiState.password,
                confirmPassword = value
            ),
            errorMessage = null,
            showRetryAction = false
        )
    }

    fun switchMode() {
        val newMode = !uiState.isLoginMode
        uiState = uiState.copy(
            isLoginMode = newMode,
            confirmPassword = "",
            confirmPasswordError = null,
            errorMessage = null,
            showRetryAction = false
        )
    }

    fun clearError() {
        uiState = uiState.copy(
            errorMessage = null,
            showRetryAction = false
        )
    }

    fun submit() {
        if (uiState.isLoading) return

        val emailError = validateEmail(uiState.email)
        val passwordError = validatePassword(uiState.password)
        val confirmPasswordError = if (!uiState.isLoginMode) {
            validateConfirmPassword(uiState.password, uiState.confirmPassword)
        } else {
            null
        }

        uiState = uiState.copy(
            emailError = emailError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )

        val hasErrors = emailError != null || passwordError != null || confirmPasswordError != null
        if (hasErrors) return

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null,
            showRetryAction = false
        )

        val email = uiState.email.trim()
        val password = uiState.password

        val task = if (uiState.isLoginMode) {
            auth.signInWithEmailAndPassword(email, password)
        } else {
            auth.createUserWithEmailAndPassword(email, password)
        }

        task.addOnCompleteListener { result ->
            if (result.isSuccessful) {
                uiState = uiState.copy(
                    isLoading = false,
                    isAuthorized = true,
                    password = "",
                    confirmPassword = "",
                    passwordError = null,
                    confirmPasswordError = null
                )
            } else {
                val exception = result.exception
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = mapError(exception),
                    showRetryAction = exception is FirebaseNetworkException
                )
            }
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "E-mail не может быть пустым"
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Введите корректный e-mail"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Пароль не может быть пустым"
            password.length < 6 -> "Пароль должен содержать минимум 6 символов"
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Подтвердите пароль"
            password != confirmPassword -> "Пароли не совпадают"
            else -> null
        }
    }

    private fun mapError(error: Exception?): String {
        return when (error) {
            is FirebaseNetworkException -> {
                "Нет сети. Проверьте подключение и попробуйте снова."
            }

            is FirebaseAuthInvalidCredentialsException -> {
                if (uiState.isLoginMode) {
                    "Неверный e-mail или пароль"
                } else {
                    "Некорректный e-mail или пароль"
                }
            }

            is FirebaseAuthInvalidUserException -> {
                "Пользователь с таким e-mail не найден"
            }

            is FirebaseAuthUserCollisionException -> {
                "Пользователь с таким e-mail уже существует"
            }

            is FirebaseTooManyRequestsException -> {
                "Слишком много попыток. Попробуйте позже"
            }

            else -> {
                error?.localizedMessage ?: "Не удалось выполнить авторизацию"
            }
        }
    }
}