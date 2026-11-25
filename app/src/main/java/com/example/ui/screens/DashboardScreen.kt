package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Product
import com.example.model.RolePermissions
import com.example.model.SaleRecord
import com.example.model.StaffAccount
import com.example.model.StockAnalytics
import com.example.model.StockStatus
import com.example.model.Store
import com.example.ui.components.DataSaverPill
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MbgLogoEmblem
import com.example.ui.components.RoleBadge
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.MbgBlueAccent
import com.example.ui.theme.MbgBlueContainer
import com.example.ui.theme.MbgGreenContainer
import com.example.ui.theme.MbgGreenDark
import com.example.ui.theme.MbgGreenPrimary
import com.example.ui.theme.MbgRedAlert
import com.example.ui.theme.MbgRedContainer
import com.example.ui.theme.MbgYellowAccent
import com.example.ui.theme.MbgYellowContainer
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    store: Store,
    operator: StaffAccount,
    products: List<Product>,
    sales: List<SaleRecord>,
    onGoToSales: () -> Unit,
    onGoToStockEntry: (preselectedProductId: String?) -> Unit,
    onGoToProducts: () -> Unit,
    onGoToReports: () -> Unit,
    onSwitchOperator: () -> Unit
) {
    val role = operator.roleEnum()
    val stats = remember(products, sales) {
        StockAnalytics.computeDashboardStats(products, sales)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Branded Header Banner with MBG STOCK Logo
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MbgGreenDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                MbgLogoEmblem(size = 52.dp)
                                Column {
                                    Text(
                                        text = "MBG STOCK",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = store.storeName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onSwitchOperator,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.testTag("switch_operator_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Changer de profil",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Changer")
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = operator.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            RoleBadge(role = role)
                            Spacer(modifier = Modifier.weight(1f))
                            DataSaverPill(enabled = store.dataSaverEnabled)
                        }
                    }
                }
            }

            // Quick Actions (strictly permission-gated)
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onGoToSales,
                        colors = ButtonDefaults.buttonColors(containerColor = MbgGreenPrimary),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("quick_action_sale")
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nouvelle vente")
                    }

                    if (RolePermissions.canAccessStockEntry(role)) {
                        FilledTonalButton(
                            onClick = { onGoToStockEntry(null) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MbgBlueContainer,
                                contentColor = MbgBlueAccent
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("quick_action_entry")
                        ) {
                            Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Entrée de stock")
                        }
                    }

                    FilledTonalButton(
                        onClick = onGoToProducts,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("quick_action_products")
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Produits")
                    }

                    if (RolePermissions.canViewReports(role)) {
                        FilledTonalButton(
                            onClick = onGoToReports,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MbgYellowContainer,
                                contentColor = MbgYellowAccent
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("quick_action_reports")
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rapports")
                        }
                    }
                }
            }

            // KPI Grid (2 columns on phone, 3 columns on tablet/desktop)
            item {
                val cards = listOf<@Composable (Modifier) -> Unit>(
                    { mod ->
                        KpiMetricCard(
                            title = "Total des produits",
                            value = Formatters.formatNumber(stats.totalProducts.toLong()),
                            subtitle = "Références",
                            icon = Icons.Default.Inventory2,
                            accentColor = MbgGreenPrimary,
                            containerColor = MbgGreenContainer,
                            modifier = mod
                        )
                    },
                    { mod ->
                        KpiMetricCard(
                            title = "Quantité totale disponible",
                            value = Formatters.formatNumber(stats.totalAvailableQuantity),
                            subtitle = "Unités",
                            icon = Icons.Default.Warehouse,
                            accentColor = MbgBlueAccent,
                            containerColor = MbgBlueContainer,
                            modifier = mod
                        )
                    },
                    { mod ->
                        KpiMetricCard(
                            title = "Boissons disponibles",
                            value = Formatters.formatNumber(stats.availableBeverageUnits),
                            subtitle = "${stats.availableBeverageProducts} réf.",
                            icon = Icons.Default.LocalBar,
                            accentColor = MbgGreenPrimary,
                            containerColor = MbgGreenContainer,
                            modifier = mod
                        )
                    },
                    { mod ->
                        KpiMetricCard(
                            title = "Produits en rupture",
                            value = Formatters.formatNumber(stats.outOfStockCount.toLong()),
                            subtitle = "Stock = 0",
                            icon = Icons.Default.ErrorOutline,
                            accentColor = MbgRedAlert,
                            containerColor = MbgRedContainer,
                            modifier = mod
                        )
                    },
                    { mod ->
                        KpiMetricCard(
                            title = "À réapprovisionner",
                            value = Formatters.formatNumber(stats.lowStockAlertCount.toLong()),
                            subtitle = "Seuil min.",
                            icon = Icons.Default.WarningAmber,
                            accentColor = MbgYellowAccent,
                            containerColor = MbgYellowContainer,
                            modifier = mod
                        )
                    },
                    { mod ->
                        KpiMetricCard(
                            title = "Total des ventes enregistrées",
                            value = Formatters.formatFcfa(stats.totalSalesAmount, store.currency),
                            subtitle = "${stats.totalSalesCount} ventes",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            accentColor = MbgBlueAccent,
                            containerColor = MbgBlueContainer,
                            modifier = mod
                        )
                    }
                )

                val cols = if (isWide) 3 else 2
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cards.chunked(cols).forEach { rowCards ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowCards.forEach { card ->
                                card(Modifier.weight(1f))
                            }
                            repeat(cols - rowCards.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Stock Minimum & Rupture Alerts (for Propriétaire & Gestionnaire)
            if (RolePermissions.canViewAlerts(role)) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Alertes de stock (${stats.alertProducts.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (stats.alertProducts.isNotEmpty()) {
                            Text(
                                text = "Ventes bloquées uniquement à 0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (stats.alertProducts.isEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    tint = MbgGreenPrimary
                                )
                                Text(
                                    text = if (products.isEmpty()) {
                                        "Aucun produit enregistré. Ajoutez vos premiers articles pour suivre les alertes."
                                    } else {
                                        "Tous les produits sont au-dessus de leur seuil minimum d'alerte."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(stats.alertProducts, key = { it.id }) { product ->
                        AlertProductCard(
                            product = product,
                            onRestock = { onGoToStockEntry(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertProductCard(
    product: Product,
    onRestock: () -> Unit
) {
    val status = product.status()
    val borderColor = if (status == StockStatus.RUPTURE) MbgRedAlert.copy(alpha = 0.5f) else MbgYellowAccent.copy(alpha = 0.5f)

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StockStatusBadge(status = status)
                }
                Text(
                    text = "Stock restant : ${product.quantity} btl • Seuil min : ${product.minThreshold}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onRestock,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MbgBlueContainer,
                    contentColor = MbgBlueAccent
                ),
                modifier = Modifier.height(44.dp)
            ) {
                Text("Réappro.")
            }
        }
    }
}
