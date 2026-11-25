package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.MbgLogoEmblem

@Composable
fun StoreOnboardingScreen(
    userEmail: String?,
    isSubmitting: Boolean,
    onCreateStore: (storeName: String, username: String, displayName: String, password: String) -> Unit,
    onCheckInvites: () -> Unit,
    onSignOutGoogle: () -> Unit
) {
    var storeName by remember { mutableStateOf("MBG STOCK Dépôt") }
    var displayName by remember { mutableStateOf("Propriétaire") }
    var username by remember { mutableStateOf("patron") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MbgLogoEmblem(size = 70.dp)
            Text(
                text = "Configuration de votre boutique",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Créez votre espace MBG STOCK et définissez les identifiants du profil Propriétaire.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nom de l'établissement") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_store_name")
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nom complet du Propriétaire") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_owner_name")
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Identifiant de connexion") },
                        supportingText = { Text("Ex : patron, direction") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_username")
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe (min. 4 caractères)") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Afficher le mot de passe"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_password")
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmer le mot de passe") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_confirm_password")
                    )

                    if (localError != null) {
                        Text(
                            text = localError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            localError = when {
                                storeName.trim().length < 2 -> "Entrez le nom de la boutique."
                                displayName.trim().length < 2 -> "Entrez le nom du propriétaire."
                                username.trim().length < 2 -> "Entrez un identifiant d'au moins 2 caractères."
                                password.length < 4 -> "Le mot de passe doit contenir au moins 4 caractères."
                                password != confirmPassword -> "Les mots de passe ne correspondent pas."
                                else -> null
                            }
                            if (localError == null) {
                                onCreateStore(storeName, username, displayName, password)
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("onboarding_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Créer ma boutique MBG STOCK")
                        }
                    }
                }
            }

            if (!userEmail.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onCheckInvites,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_invites_button")
                ) {
                    Text("Vérifier une invitation ($userEmail)")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSignOutGoogle) {
                    Text("Changer de compte Google")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
