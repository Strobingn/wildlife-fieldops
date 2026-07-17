package com.strobingn.wildlifefieldops

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.strobingn.wildlifefieldops.data.remote.SupabaseService
import com.strobingn.wildlifefieldops.ui.theme.WildlifeFieldOpsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject lateinit var supabaseService: SupabaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WildlifeFieldOpsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthGate(
                        service = supabaseService,
                        onAuthenticated = ::openApp
                    )
                }
            }
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
private fun AuthGate(
    service: SupabaseService,
    onAuthenticated: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (service.isAuthenticated()) {
            onAuthenticated()
        } else {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wildlife FieldOps", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (createAccount) "Create your field operations account" else "Sign in to continue",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        if (!service.isConfigured) {
            Text(
                "Supabase is not configured in this build. Install a preproduction build with the project URL and anonymous key.",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onAuthenticated, modifier = Modifier.fillMaxWidth()) {
                Text("Continue in offline mode")
            }
            return@Column
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !loading,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                error = null
                if (email.isBlank() || password.length < 8) {
                    error = "Enter a valid email and a password of at least 8 characters."
                    return@Button
                }
                scope.launch {
                    loading = true
                    val success = if (createAccount) {
                        service.signUp(email, password)
                    } else {
                        service.signIn(email, password)
                    }
                    loading = false
                    if (success && service.isAuthenticated()) {
                        onAuthenticated()
                    } else if (success && createAccount) {
                        error = "Account created. Check your email if confirmation is required, then sign in."
                        createAccount = false
                    } else {
                        error = "Authentication failed. Check your credentials and connection."
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(strokeWidth = 2.dp)
            else Text(if (createAccount) "Create account" else "Sign in")
        }

        TextButton(
            onClick = {
                createAccount = !createAccount
                error = null
            },
            enabled = !loading
        ) {
            Text(if (createAccount) "Already have an account? Sign in" else "Create an account")
        }
    }
}
