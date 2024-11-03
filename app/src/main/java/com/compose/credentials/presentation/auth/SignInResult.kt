package com.compose.credentials.presentation.auth

data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
) {

}

data class UserData(
    val userId: String,
    val username: String?,
    val profilePict: String?
)