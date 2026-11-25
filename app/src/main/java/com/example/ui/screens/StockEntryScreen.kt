package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.Product
import com.example.model.Store
import com.example.ui.theme.MbgBlueAccent
import com.example.ui.theme.MbgBlueContainer
import com.example.ui.theme.MbgGreenPrimary
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEntryScreen(
    store: Store,
    products: List<Product>,
    preselectedProductId: String?,
    isSubmitting: Boolean,
    onSubmitEntry: (
        productId: String,
        cratesCount: Long,
        extraBottles: Long,
        unitsPerCrate: Long,
        purchasePrice: Long,
        receptionDateMs: Long,
        note: String?,
        onDone: () -> Unit
    ) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(preselectedProductId ?: products.firstOrNull()?.id.orEmpty()) }
    val selectedProduct = remember(products, selectedProductId) {
        products.firstOrNull { it.id == selectedProductId } ?: products.firstOrNull()
    }

    var productExpanded by remember { mutableStateOf(false) }
    var cratesStr by remember { mutableStateOf("0") }
    var extraBottlesStr by remember { mutableStateOf("0") }
    var unitsPerCrateStr by remember { mutableStateOf((selectedProduct?.unitsPerCrate ?: store.defaultUnitsPerCrate).toString()) }
    var purchasePriceStr by remember { mutableStateOf((selectedProduct?.purchasePrice ?: 0L).toString()) }
    var receptionDateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(preselectedProductId) {
        if (preselectedProductId != null) {
            selectedProductId = preselectedProductId
        }
    }

    LaunchedEffect(selectedProduct?.id) {
        if (selectedProduct != null) {
            unitsPerCrateStr = selectedProduct.unitsPerCrate.toString()
            purchasePriceStr = selectedProduct.purchasePrice.toString()
        }
    }

    val crates = cratesStr.toLongOrNull() ?: 0L
    val extraBottles = extraBottlesStr.toLongOrNull() ?: 0L
    val unitsPerCrate = (unitsPerCrateStr.toLongOrNull() ?: 1L).coerceAtLeast(1L)
    val purchasePrice = purchasePriceStr.toLongOrNull() ?: 0L
    val totalReceived = (crates * unitsPerCrate) + extraBottles
    val projectedStock = (selectedProduct?.quantity ?: 0L) + totalReceived
    val totalPurchaseCost = totalReceived * purchasePrice

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.AddBox, contentDescription = null, tint = MbgBlueAccent)
                Text(
                    text = "Entrée de stock (Réception)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (products.isEmpty()) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aucun produit enregistré. Ajoutez d'abord un produit dans l'onglet Produits avant d'enregistrer une réception.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(18.dp)
                    )
                }
                return
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = !productExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.let { "${it.name} (Stock : ${it.quantity})" } ?: "Sélectionner un produit",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Produit concerné") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("entry_product_picker")
                        )
                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false }
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.name} • Stock actuel : ${prod.quantity}") },
                                    onClick = {
                                        selectedProductId = prod.id
                                        productExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cratesStr,
                            onValueChange = { cratesStr = it.filter(Char::isDigit); localError = null },
                            label = { Text("Nombre de casiers") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entry_crates_input")
                        )
                        OutlinedTextField(
                            value = extraBottlesStr,
                            onValueChange = { extraBottlesStr = it.filter(Char::isDigit); localError = null },
                            label = { Text("Bouteilles suppl.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entry_bottles_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = unitsPerCrateStr,
                            onValueChange = { unitsPerCrateStr = it.filter(Char::isDigit); localError = null },
                            label = { Text("Unités / casier") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entry_units_per_crate_input")
                        )
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it.filter(Char::isDigit); localError = null },
                            label = { Text("Prix achat unitaire (${store.currency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entry_purchase_price_input")
                        )
                    }

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("entry_date_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date de réception : ${Formatters.formatDate(receptionDateMs)}")
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note / Fournisseur (optionnel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Live Calculation Summary (Addition to existing stock)
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MbgBlueContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MbgBlueAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Calcul automatique",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MbgBlueAccent
                    )
                    Text(
                        text = "Quantité reçue : ($crates × $unitsPerCrate) + $extraBottles = $totalReceived unité(s)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selectedProduct != null) {
                        Text(
                            text = "Cumul : ${selectedProduct.quantity} (existant) + $totalReceived = $projectedStock unité(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MbgGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Montant d'achat total : ${Formatters.formatFcfa(totalPurchaseCost, store.currency)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (localError != null) {
                Text(
                    text = localError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    val prod = selectedProduct
                    val parsedPerCrate = unitsPerCrateStr.toLongOrNull() ?: 0L
                    localError = when {
                        prod == null -> "Sélectionnez un produit."
                        parsedPerCrate !in 1L..200L -> "Unités par casier doit être entre 1 et 200."
                        totalReceived <= 0L -> "La quantité reçue doit être supérieure à zéro."
                        else -> null
                    }
                    if (localError == null && prod != null) {
                        onSubmitEntry(
                            prod.id,
                            crates,
                            extraBottles,
                            parsedPerCrate,
                            purchasePrice,
                            receptionDateMs,
                            note
                        ) {
                            cratesStr = "0"
                            extraBottlesStr = "0"
                            note = ""
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MbgGreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_stock_entry_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter au stock existant")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = receptionDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { receptionDateMs = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
