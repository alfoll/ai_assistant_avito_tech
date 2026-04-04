package com.alfoll.aiassistant.ui.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,

    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthorized: Boolean = false,

    val errorMessage: String? = null,
    val showRetryAction: Boolean = false
)