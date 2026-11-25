package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.MbgLogoEmblem
import com.example.ui.theme.MbgBlueAccent
import com.example.ui.theme.MbgGreenDark
import com.example.ui.theme.MbgGreenPrimary
import com.example.ui.theme.MbgYellowAccent

@Composable
fun GoogleAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MbgGreenDark, MbgGreenPrimary, MaterialTheme.colorScheme.background)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MbgLogoEmblem(size = 88.dp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MBG STOCK",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gestion professionnelle des entrées, sorties et ventes de boissons",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
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
                        text = "Connexion Cloud Sécurisée",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Connectez votre compte Google pour synchroniser votre boutique en temps réel avec une consommation minimale de données.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FeatureHighlightRow(
                        icon = Icons.Default.CloudDone,
                        tint = MbgGreenPrimary,
                        text = "Synchronisation cloud et cache local hors-ligne"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Lock,
                        tint = MbgBlueAccent,
                        text = "Accès sécurisé par profils (Propriétaire, Gestionnaire, Gérant)"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Speed,
                        tint = MbgYellowAccent,
                        text = "Mode éco-données optimisé pour le terrain"
                    )

                    if (errorMessage != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MbgGreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connexion en cours...")
                        } else {
                            Text(
                                text = "Continuer avec Google",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
