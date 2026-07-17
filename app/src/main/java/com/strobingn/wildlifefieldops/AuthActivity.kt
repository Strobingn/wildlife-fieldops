package com.strobingn.wildlifefieldops

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.strobingn.wildlifefieldops.data.repository.Organization
import com.strobingn.wildlifefieldops.data.repository.OrganizationRepository
import com.strobingn.wildlifefieldops.ui.theme.WildlifeFieldOpsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject lateinit var supabaseService: SupabaseService
    @Inject lateinit var organizationRepository: OrganizationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WildlifeFieldOpsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthAndOrganizationGate(
                        service = supabaseService,
                        organizationRepository = organizationRepository,
                        onReady = ::openApp
                    )
                }
            }
        }
    }

    private fun openApp(organization: Organization?) {
        val preferences = getSharedPreferences("tenant", MODE_PRIVATE)
        if (organization == null) {
            preferences.edit().remove("organization_id").remove("organization_name").apply()
        } else {
            preferences.edit()
                .putString("organization_id", organization.id)
                .putString("organization_name", organization.name)
                .apply()
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

private enum class GateStage { AUTH, ORGANIZATION }

@Composable
private fun AuthAndOrganizationGate(
    service: SupabaseService,
    organizationRepository: OrganizationRepository,
    onReady: (Organization?) -> Unit
) {
    var stage by remember { mutableStateOf(GateStage.AUTH) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var organizations by remember { mutableStateOf<List<Organization>>(emptyList()) }
    var selectedOrganizationId by remember { mutableStateOf<String?>(null) }
    var newOrganizationName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun loadOrganizations() {
        loading = true
        error = null
        organizationRepository.listForCurrentUser()
            .onSuccess { result ->
                organizations = result
                selectedOrganizationId = result.singleOrNull()?.id ?: selectedOrganizationId
                stage = GateStage.ORGANIZATION
            }
            .onFailure { throwable ->
                error = throwable.message ?: "Unable to load organizations."
                stage = GateStage.ORGANIZATION
            }
        loading = false
    }

    LaunchedEffect(Unit) {
        if (service.isAuthenticated()) loadOrganizations() else loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wildlife FieldOps", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (!service.isConfigured) {
            Text(
                "Supabase is not configured in this build. Cloud authentication and tenant security are unavailable.",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { onReady(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("Continue in offline mode")
            }
            return@Column
        }

        if (stage == GateStage.AUTH) {
            Text(
                if (createAccount) "Create your field operations account" else "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
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
                        val success = if (createAccount) service.signUp(email, password)
                        else service.signIn(email, password)
                        if (success && service.isAuthenticated()) {
                            loadOrganizations()
                        } else if (success && createAccount) {
                            loading = false
                            error = "Account created. Confirm your email if required, then sign in."
                            createAccount = false
                        } else {
                            loading = false
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
                onClick = { createAccount = !createAccount; error = null },
                enabled = !loading
            ) {
                Text(if (createAccount) "Already have an account? Sign in" else "Create an account")
            }
        } else {
            Text("Select your organization", style = MaterialTheme.typography.titleLarge)
            Text("All cloud records are isolated to the selected organization.")
            Spacer(Modifier.height(20.dp))

            if (loading) {
                CircularProgressIndicator()
            } else {
                organizations.forEach { organization ->
                    Card(
                        onClick = { selectedOrganizationId = organization.id },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOrganizationId == organization.id,
                                onClick = { selectedOrganizationId = organization.id }
                            )
                            Text(organization.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                if (organizations.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            organizations.firstOrNull { it.id == selectedOrganizationId }?.let(onReady)
                        },
                        enabled = selectedOrganizationId != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue") }
                }

                Spacer(Modifier.height(24.dp))
                Text("Create a new organization", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newOrganizationName,
                    onValueChange = { newOrganizationName = it },
                    label = { Text("Organization name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            organizationRepository.create(newOrganizationName)
                                .onSuccess(onReady)
                                .onFailure { error = it.message ?: "Unable to create organization." }
                            loading = false
                        }
                    },
                    enabled = newOrganizationName.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create organization") }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = {
                scope.launch {
                    service.signOut()
                    stage = GateStage.AUTH
                    organizations = emptyList()
                    selectedOrganizationId = null
                    error = null
                }
            }) { Text("Sign out") }
        }
    }
}
