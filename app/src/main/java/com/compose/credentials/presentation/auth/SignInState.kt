package com.compose.credentials.presentation.auth

data class SignInState(
    val isSuccessful: Boolean = false,
    val error: String? = null
)
