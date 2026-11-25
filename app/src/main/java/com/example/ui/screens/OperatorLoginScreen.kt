package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.StaffAccount
import com.example.model.Store
import com.example.ui.components.DataSaverPill
import com.example.ui.components.MbgLogoEmblem
import com.example.ui.theme.MbgGreenDark
import com.example.ui.theme.MbgGreenPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperatorLoginScreen(
    store: Store,
    staffList: List<StaffAccount>,
    loginError: String?,
    onLogin: (username: String, password: String) -> Unit,
    onClearError: () -> Unit,
    onSignOutGoogle: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MbgGreenDark, MbgGreenPrimary.copy(alpha = 0.9f), MaterialTheme.colorScheme.background)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            MbgLogoEmblem(size = 84.dp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MBG STOCK",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = store.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                DataSaverPill(enabled = store.dataSaverEnabled)
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Connexion Opérateur",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Saisissez votre identifiant et votre mot de passe pour accéder à vos fonctions autorisées.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (staffList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Comptes disponibles :",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                staffList.filter { it.active }.forEach { account ->
                                    AssistChip(
                                        onClick = {
                                            username = account.username
                                            onClearError()
                                        },
                                        label = {
                                            Text("${account.username} (${account.roleEnum().shortLabel})")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            onClearError()
                        },
                        label = { Text("Identifiant") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("operator_username_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        label = { Text("Mot de passe") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (username.isNotBlank() && password.isNotBlank()) {
                                    onLogin(username, password)
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Afficher ou masquer"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("operator_password_input")
                    )

                    if (loginError != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = loginError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { onLogin(username, password) },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("operator_login_button")
                    ) {
                        Text(
                            text = "Se connecter",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            TextButton(onClick = onSignOutGoogle) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Déconnecter le compte Google",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
