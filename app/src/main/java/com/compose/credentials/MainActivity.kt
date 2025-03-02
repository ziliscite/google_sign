package com.compose.credentials

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.compose.credentials.presentation.auth.GoogleAuthUIClient
import com.compose.credentials.presentation.auth.SignInScreen
import com.compose.credentials.presentation.auth.SignInViewModel
import com.compose.credentials.presentation.profile.ProfileScreen
import com.compose.credentials.ui.theme.CredentialsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val googleAuthUIClient by lazy {
        GoogleAuthUIClient(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CredentialsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "sign_in") {
                        composable(route = "sign_in") {
                            val viewModel = viewModel<SignInViewModel>()
                            val state by viewModel.state.collectAsStateWithLifecycle()

                            // If already logged in, we go here instead of signed up screen
                            // Unit mean it will launch once when composable is composed
                            LaunchedEffect(Unit) {
                                if(googleAuthUIClient.getSignedInUser() != null) {
                                    navController.navigate("profile")
                                }
                            }

                            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                                if (it.resultCode == RESULT_OK) { lifecycleScope.launch {
                                    val result = googleAuthUIClient.signInWithIntentGoogle(it.data ?: return@launch)
                                    viewModel.onSignInResult(result)
                                }}
                            }

                            LaunchedEffect(state.isSuccessful) {
                                if (state.isSuccessful) {
                                    Toast.makeText(applicationContext, "Success", Toast.LENGTH_SHORT).show()
                                    navController.navigate("profile")
                                    viewModel.resetState()
                                }
                            }

                            SignInScreen(
                                state = state
                            ) {
                                lifecycleScope.launch {
                                    val signInIntentSender = googleAuthUIClient.signInWithGoogleIntent()
                                    launcher.launch(signInIntentSender)
                                }
                            }
                        }
                        composable(route = "profile") {
                            ProfileScreen(googleAuthUIClient.getSignedInUser()) {
                                googleAuthUIClient.signOutGoogle()
                                lifecycleScope.launch {
                                    try {
                                        googleAuthUIClient.revokeAccess()
                                        Toast.makeText(applicationContext, "Signed out", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(applicationContext, "Failed to revoke access. Try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}