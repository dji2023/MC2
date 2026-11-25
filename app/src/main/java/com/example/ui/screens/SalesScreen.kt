package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CartItem
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.StockStatus
import com.example.model.Store
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.MbgGreenPrimary
import com.example.ui.theme.MbgRedAlert
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    store: Store,
    products: List<Product>,
    cartMap: Map<String, Int>,
    isSubmitting: Boolean,
    onIncrement: (Product) -> Unit,
    onDecrement: (String) -> Unit,
    onClearCart: () -> Unit,
    onSubmitSale: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchesCat = selectedCategory == null || prod.categoryEnum() == selectedCategory
            val matchesSearch = searchQuery.isBlank() || prod.name.contains(searchQuery.trim(), ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    val cartItems = remember(products, cartMap) {
        cartMap.mapNotNull { (id, qty) ->
            val p = products.firstOrNull { it.id == id } ?: return@mapNotNull null
            if (qty > 0) CartItem(p, qty) else null
        }.sortedBy { it.product.name }
    }
    val totalUnits = cartItems.sumOf { it.quantity }
    val totalAmount = cartItems.sumOf { it.lineTotal }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 780.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SalesFilterHeader(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )
                    SalesProductList(
                        products = filteredProducts,
                        cartMap = cartMap,
                        currency = store.currency,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                        modifier = Modifier.weight(1f)
                    )
                }

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CartCheckoutContent(
                        cartItems = cartItems,
                        totalUnits = totalUnits,
                        totalAmount = totalAmount,
                        currency = store.currency,
                        isSubmitting = isSubmitting,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                        onClearCart = onClearCart,
                        onSubmitSale = onSubmitSale,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SalesFilterHeader(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )
                }

                SalesProductList(
                    products = filteredProducts,
                    cartMap = cartMap,
                    currency = store.currency,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )

                if (cartItems.isNotEmpty()) {
                    Surface(
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "$totalUnits article(s) • ${cartItems.size} ligne(s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatFcfa(totalAmount, store.currency),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MbgGreenPrimary
                                )
                            }
                            Button(
                                onClick = { showCartSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MbgGreenPrimary),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("open_checkout_button")
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Valider")
                            }
                        }
                    }
                }
            }

            if (showCartSheet) {
                ModalBottomSheet(onDismissRequest = { if (!isSubmitting) showCartSheet = false }) {
                    CartCheckoutContent(
                        cartItems = cartItems,
                        totalUnits = totalUnits,
                        totalAmount = totalAmount,
                        currency = store.currency,
                        isSubmitting = isSubmitting,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                        onClearCart = {
                            onClearCart()
                            showCartSheet = false
                        },
                        onSubmitSale = {
                            onSubmitSale()
                            showCartSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SalesFilterHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: ProductCategory?,
    onSelectCategory: (ProductCategory?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Sortie de stock & Ventes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Rechercher un produit (Régab, Castel...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sales_search_input")
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("Tout") }
                )
            }
            items(ProductCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                    label = { Text(cat.label) }
                )
            }
        }
    }
}

@Composable
private fun SalesProductList(
    products: List<Product>,
    cartMap: Map<String, Int>,
    currency: String,
    onIncrement: (Product) -> Unit,
    onDecrement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Aucun produit disponible",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ajoutez des produits ou modifiez votre filtre.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(products, key = { it.id }) { product ->
            val qtyInCart = cartMap[product.id] ?: 0
            val status = product.status()
            val outOfStock = product.quantity <= 0L

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (outOfStock) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = if (qtyInCart > 0) 1.5.dp else 1.dp,
                    color = if (qtyInCart > 0) MbgGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                ),
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
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            StockStatusBadge(status = status)
                        }
                        Text(
                            text = "${Formatters.formatFcfa(product.sellingPrice, currency)} • Stock : ${product.quantity} btl",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (outOfStock) MbgRedAlert else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (qtyInCart > 0) {
                            Text(
                                text = "Sous-total : ${Formatters.formatFcfa(qtyInCart.toLong() * product.sellingPrice, currency)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MbgGreenPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (qtyInCart > 0) {
                            IconButton(
                                onClick = { onDecrement(product.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Retirer 1")
                            }
                            Text(
                                text = "$qtyInCart",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { onIncrement(product) },
                            enabled = !outOfStock && qtyInCart.toLong() < product.quantity,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("add_to_cart_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ajouter 1 ${product.name}",
                                tint = if (!outOfStock && qtyInCart.toLong() < product.quantity) MbgGreenPrimary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartCheckoutContent(
    cartItems: List<CartItem>,
    totalUnits: Int,
    totalAmount: Long,
    currency: String,
    isSubmitting: Boolean,
    onIncrement: (Product) -> Unit,
    onDecrement: (String) -> Unit,
    onClearCart: () -> Unit,
    onSubmitSale: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ticket de vente",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart, enabled = !isSubmitting) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vider")
                }
            }
        }

        if (cartItems.isEmpty()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sélectionnez un ou plusieurs produits pour composer la vente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.quantity} × ${Formatters.formatFcfa(item.product.sellingPrice, currency)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onDecrement(item.product.id) },
                                enabled = !isSubmitting,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuer")
                            }
                            IconButton(
                                onClick = { onIncrement(item.product) },
                                enabled = !isSubmitting && item.quantity.toLong() < item.product.quantity,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Augmenter")
                            }
                            Text(
                                text = Formatters.formatFcfa(item.lineTotal, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MbgGreenPrimary
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total à payer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$totalUnits unité(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = Formatters.formatFcfa(totalAmount, currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Button(
                onClick = onSubmitSale,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MbgGreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_sale_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Validation...")
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Valider la vente")
                }
            }
        }
    }
}
