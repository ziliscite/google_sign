package com.compose.credentials.presentation.auth

import android.content.Context
import android.content.Intent
import com.compose.credentials.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

// Later use cred manager instead of SignInClient
class GoogleAuthUIClient(context: Context) {
    private val auth = Firebase.auth

    // Instantiate googleSignInClient once and reuse it
    private val googleSignInClient = GoogleSignIn.getClient(context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()
    )

    fun signInWithGoogleIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun signInWithIntentGoogle(intent: Intent): SignInResult {
        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        val account = task.await()
        val googleCred = GoogleAuthProvider.getCredential(account.idToken, null)

        return try {
            // Get firebase user
            val user = auth.signInWithCredential(googleCred).await().user
            val data = user?.let {
                UserData(
                    it.uid,
                    it.displayName,
                    it.photoUrl.toString()
                )
            }
            SignInResult(data, null)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(null, e.message)
        }
    }

    fun signOutGoogle() {
        googleSignInClient.signOut() // Also sign out from Google client
        auth.signOut() // Sign out from Firebase
    }

    suspend fun revokeAccess() {
        try {
            googleSignInClient.revokeAccess().await()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            // Handle specific ApiException codes or show an error message
        }
    }

    fun getSignedInUser(): UserData? {
        return auth.currentUser?.let {
            UserData(
                it.uid,
                it.displayName,
                it.photoUrl.toString()
            )
        }
    }
}

// That's it for the auth flow
