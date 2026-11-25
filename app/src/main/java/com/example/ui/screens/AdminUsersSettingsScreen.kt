package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.StaffAccount
import com.example.model.Store
import com.example.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.theme.MbgRedAlert

@Composable
fun AdminUsersSettingsScreen(
    store: Store,
    activeOperator: StaffAccount,
    staffList: List<StaffAccount>,
    isSubmitting: Boolean,
    onSaveStaff: (
        existingStaffId: String?,
        username: String,
        displayName: String,
        role: UserRole,
        newPassword: String?,
        active: Boolean,
        linkedEmail: String?,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteStaff: (StaffAccount) -> Unit,
    onUpdateSettings: (
        storeName: String,
        defaultUnitsPerCrate: Long,
        defaultMinThreshold: Long,
        dataSaverEnabled: Boolean
    ) -> Unit,
    onSignOutGoogle: () -> Unit
) {
    var showStaffDialog by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffAccount?>(null) }

    var storeName by remember(store.storeName) { mutableStateOf(store.storeName) }
    var defaultCrateStr by remember(store.defaultUnitsPerCrate) { mutableStateOf(store.defaultUnitsPerCrate.toString()) }
    var defaultThresholdStr by remember(store.defaultMinThreshold) { mutableStateOf(store.defaultMinThreshold.toString()) }
    var dataSaver by remember(store.dataSaverEnabled) { mutableStateOf(store.dataSaverEnabled) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Utilisateurs & Paramètres",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = {
                        editingStaff = null
                        showStaffDialog = true
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_staff_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajouter")
                }
            }
        }

        items(staffList, key = { it.id }) { account ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = account.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            RoleBadge(role = account.roleEnum())
                        }
                        Text(
                            text = "Identifiant : ${account.username} • ${if (account.active) "Actif" else "Désactivé"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!account.linkedEmail.isNullOrBlank()) {
                            Text(
                                text = "Email sync : ${account.linkedEmail}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                editingStaff = account
                                showStaffDialog = true
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier ${account.displayName}")
                        }
                        if (account.id != activeOperator.id) {
                            IconButton(
                                onClick = { onDeleteStaff(account) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Supprimer ${account.displayName}",
                                    tint = MbgRedAlert
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text(
                    text = "Paramètres de la boutique",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nom de la boutique") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = defaultCrateStr,
                            onValueChange = { defaultCrateStr = it.filter(Char::isDigit) },
                            label = { Text("Unités / casier défaut") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = defaultThresholdStr,
                            onValueChange = { defaultThresholdStr = it.filter(Char::isDigit) },
                            label = { Text("Seuil alerte défaut") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode économie de données",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Cache local persistant 32 Mo et synchronisation incrémentielle (~5 Mo / 3-5h)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dataSaver,
                            onCheckedChange = { dataSaver = it }
                        )
                    }

                    Button(
                        onClick = {
                            val crate = (defaultCrateStr.toLongOrNull() ?: 24L).coerceIn(1L, 200L)
                            val threshold = (defaultThresholdStr.toLongOrNull() ?: 10L).coerceIn(0L, 10000L)
                            onUpdateSettings(storeName, crate, threshold, dataSaver)
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_button")
                    ) {
                        Text("Enregistrer les paramètres")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOutGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("google_sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Déconnecter le compte Google")
            }
        }
    }

    if (showStaffDialog) {
        StaffFormDialog(
            existing = editingStaff,
            isSubmitting = isSubmitting,
            onDismiss = { showStaffDialog = false },
            onConfirm = { username, displayName, role, password, active, email ->
                onSaveStaff(
                    editingStaff?.id,
                    username,
                    displayName,
                    role,
                    password,
                    active,
                    email
                ) {
                    showStaffDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffFormDialog(
    existing: StaffAccount?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        username: String,
        displayName: String,
        role: UserRole,
        newPassword: String?,
        active: Boolean,
        linkedEmail: String?
    ) -> Unit
) {
    var displayName by remember { mutableStateOf(existing?.displayName ?: "") }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var role by remember { mutableStateOf(existing?.roleEnum() ?: UserRole.GERANT) }
    var roleExpanded by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var linkedEmail by remember { mutableStateOf(existing?.linkedEmail ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(if (existing == null) "Nouveau compte utilisateur" else "Modifier ${existing.displayName}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nom complet") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_display_name_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Identifiant de connexion") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_username_input")
                )

                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Profil / Droits") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        UserRole.entries.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.label) },
                                onClick = {
                                    role = r
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(if (existing == null) "Mot de passe (min. 4 car.)" else "Nouveau mot de passe (optionnel)")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_password_input")
                )

                OutlinedTextField(
                    value = linkedEmail,
                    onValueChange = { linkedEmail = it },
                    label = { Text("Email Google multi-appareils (optionnel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Compte actif")
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorMsg = when {
                        displayName.trim().length < 2 -> "Nom complet requis (min. 2 car.)."
                        username.trim().length < 2 -> "Identifiant requis (min. 2 car.)."
                        existing == null && password.trim().length < 4 -> "Mot de passe requis (min. 4 car.)."
                        existing != null && password.isNotBlank() && password.trim().length < 4 -> "Minimum 4 caractères."
                        else -> null
                    }
                    if (errorMsg == null) {
                        onConfirm(
                            username,
                            displayName,
                            role,
                            password.takeIf { it.isNotBlank() },
                            active,
                            linkedEmail.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.testTag("staff_save_button")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Annuler")
            }
        }
    )
}
