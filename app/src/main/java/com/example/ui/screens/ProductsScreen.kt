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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.RolePermissions
import com.example.model.StaffAccount
import com.example.model.StockStatus
import com.example.model.Store
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.MbgGreenPrimary
import com.example.ui.theme.MbgRedAlert
import com.example.util.Formatters

@Composable
fun ProductsScreen(
    store: Store,
    operator: StaffAccount,
    products: List<Product>,
    isSubmitting: Boolean,
    onAddProduct: (
        name: String,
        category: ProductCategory,
        purchasePrice: Long,
        sellingPrice: Long,
        initialQuantity: Long,
        minThreshold: Long,
        unitsPerCrate: Long,
        onDone: () -> Unit
    ) -> Unit,
    onUpdateProduct: (
        product: Product,
        name: String,
        category: ProductCategory,
        purchasePrice: Long,
        sellingPrice: Long,
        quantity: Long,
        minThreshold: Long,
        unitsPerCrate: Long,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    val role = operator.roleEnum()
    val canAdd = RolePermissions.canAddProduct(role)
    val canEdit = RolePermissions.canEditProduct(role)
    val canDelete = RolePermissions.canDeleteProduct(role)
    val showPurchasePrice = RolePermissions.canViewPurchasePrices(role)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var onlyAlerts by remember { mutableStateOf(false) }

    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }

    val filtered = remember(products, searchQuery, selectedCategory, onlyAlerts) {
        products.filter { prod ->
            val matchCat = selectedCategory == null || prod.categoryEnum() == selectedCategory
            val matchSearch = searchQuery.isBlank() || prod.name.contains(searchQuery.trim(), ignoreCase = true)
            val matchAlert = !onlyAlerts || prod.status() != StockStatus.NORMAL
            matchCat && matchSearch && matchAlert
        }
    }

    Scaffold(
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    containerColor = MbgGreenPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un produit")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Catalogue & Stock (${products.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher un produit...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("products_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && !onlyAlerts,
                        onClick = {
                            selectedCategory = null
                            onlyAlerts = false
                        },
                        label = { Text("Tout") }
                    )
                }
                item {
                    FilterChip(
                        selected = onlyAlerts,
                        onClick = { onlyAlerts = !onlyAlerts },
                        label = { Text("En alerte / Rupture") }
                    )
                }
                items(ProductCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (products.isEmpty()) "Aucun produit dans la boutique." else "Aucun produit ne correspond aux filtres.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { product ->
                        ProductRowCard(
                            product = product,
                            currency = store.currency,
                            showPurchasePrice = showPurchasePrice,
                            canEdit = canEdit,
                            canDelete = canDelete,
                            onEdit = {
                                editingProduct = product
                                showProductDialog = true
                            },
                            onDelete = { deletingProduct = product }
                        )
                    }
                }
            }
        }
    }

    if (showProductDialog) {
        ProductFormDialog(
            store = store,
            existing = editingProduct,
            isSubmitting = isSubmitting,
            onDismiss = { showProductDialog = false },
            onConfirm = { name, cat, purchase, selling, qty, threshold, perCrate ->
                val target = editingProduct
                if (target == null) {
                    onAddProduct(name, cat, purchase, selling, qty, threshold, perCrate) {
                        showProductDialog = false
                    }
                } else {
                    onUpdateProduct(target, name, cat, purchase, selling, qty, threshold, perCrate) {
                        showProductDialog = false
                    }
                }
            }
        )
    }

    val toDelete = deletingProduct
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { Text("Supprimer le produit ?") },
            text = {
                Text("Voulez-vous vraiment supprimer « ${toDelete.name} » du catalogue ? L'historique existant sera conservé.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(toDelete)
                        deletingProduct = null
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun ProductRowCard(
    product: Product,
    currency: String,
    showPurchasePrice: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StockStatusBadge(status = product.status())
                    }
                    Text(
                        text = "${product.categoryEnum().label} • ${product.unitsPerCrate} btl/casier",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (canEdit || canDelete) {
                    Row {
                        if (canEdit) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier ${product.name}")
                            }
                        }
                        if (canDelete) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Supprimer ${product.name}",
                                    tint = MbgRedAlert
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Stock : ${product.quantity} unité(s)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (product.quantity == 0L) MbgRedAlert else MbgGreenPrimary
                    )
                    Text(
                        text = "${product.cratesCount()} casier(s) + ${product.remainingBottles()} btl • Seuil : ${product.minThreshold}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vente : ${Formatters.formatFcfa(product.sellingPrice, currency)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (showPurchasePrice) {
                        Text(
                            text = "Achat : ${Formatters.formatFcfa(product.purchasePrice, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormDialog(
    store: Store,
    existing: Product?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        category: ProductCategory,
        purchasePrice: Long,
        sellingPrice: Long,
        quantity: Long,
        minThreshold: Long,
        unitsPerCrate: Long
    ) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.categoryEnum() ?: ProductCategory.BOISSONS) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var purchaseStr by remember { mutableStateOf(existing?.purchasePrice?.toString() ?: "") }
    var sellingStr by remember { mutableStateOf(existing?.sellingPrice?.toString() ?: "") }
    var qtyStr by remember { mutableStateOf(existing?.quantity?.toString() ?: "0") }
    var thresholdStr by remember { mutableStateOf((existing?.minThreshold ?: store.defaultMinThreshold).toString()) }
    var perCrateStr by remember { mutableStateOf((existing?.unitsPerCrate ?: store.defaultUnitsPerCrate).toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(if (existing == null) "Nouveau produit" else "Modifier ${existing.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du produit (ex : Régab 65cl)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ProductCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchaseStr,
                        onValueChange = { purchaseStr = it.filter(Char::isDigit) },
                        label = { Text("Prix achat (${store.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_purchase_input")
                    )
                    OutlinedTextField(
                        value = sellingStr,
                        onValueChange = { sellingStr = it.filter(Char::isDigit) },
                        label = { Text("Prix vente (${store.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_selling_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it.filter(Char::isDigit) },
                        label = { Text(if (existing == null) "Stock initial" else "Quantité") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_qty_input")
                    )
                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it.filter(Char::isDigit) },
                        label = { Text("Seuil alerte") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_threshold_input")
                    )
                }

                OutlinedTextField(
                    value = perCrateStr,
                    onValueChange = { perCrateStr = it.filter(Char::isDigit) },
                    label = { Text("Unités par casier") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_crate_input")
                )

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
                    val purchase = purchaseStr.toLongOrNull() ?: 0L
                    val selling = sellingStr.toLongOrNull() ?: 0L
                    val qty = qtyStr.toLongOrNull() ?: 0L
                    val threshold = thresholdStr.toLongOrNull() ?: 0L
                    val perCrate = perCrateStr.toLongOrNull() ?: 0L

                    errorMsg = when {
                        name.trim().isEmpty() -> "Le nom du produit est requis."
                        selling <= 0L -> "Saisissez un prix de vente valide."
                        perCrate !in 1L..200L -> "Unités par casier entre 1 et 200."
                        else -> null
                    }
                    if (errorMsg == null) {
                        onConfirm(name, category, purchase, selling, qty, threshold, perCrate)
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.testTag("product_save_button")
            ) {
                Text(if (isSubmitting) "Enregistrement..." else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Annuler")
            }
        }
    )
}
