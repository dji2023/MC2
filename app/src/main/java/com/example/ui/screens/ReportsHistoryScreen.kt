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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MovementType
import com.example.model.SaleRecord
import com.example.model.StockAnalytics
import com.example.model.StockMovement
import com.example.model.Store
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.RoleBadge
import com.example.ui.theme.MbgBlueAccent
import com.example.ui.theme.MbgBlueContainer
import com.example.ui.theme.MbgGreenContainer
import com.example.ui.theme.MbgGreenPrimary
import com.example.ui.theme.MbgYellowAccent
import com.example.ui.theme.MbgYellowContainer
import com.example.util.Formatters
import java.util.Calendar

private enum class ReportTab(val label: String) {
    REPORTS("Rapports"),
    HISTORY("Historique")
}

private enum class PeriodMode(val label: String) {
    DAILY("Journalier"),
    MONTHLY("Mensuel")
}

@Composable
fun ReportsHistoryScreen(
    store: Store,
    movements: List<StockMovement>,
    sales: List<SaleRecord>
) {
    var selectedTab by remember { mutableStateOf(ReportTab.REPORTS) }
    var periodMode by remember { mutableStateOf(PeriodMode.DAILY) }
    var periodOffset by remember { mutableIntStateOf(0) }
    var historyFilter by remember { mutableStateOf<MovementType?>(null) }

    val (startMs, endMs, periodLabel) = remember(periodMode, periodOffset) {
        val cal = Calendar.getInstance()
        if (periodMode == PeriodMode.DAILY) {
            cal.add(Calendar.DAY_OF_YEAR, periodOffset)
            val (s, e) = Formatters.dayRangeMs(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            Triple(s, e, Formatters.formatDate(s))
        } else {
            cal.add(Calendar.MONTH, periodOffset)
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val (s, e) = Formatters.monthRangeMs(y, m)
            Triple(s, e, Formatters.formatMonthYear(y, m))
        }
    }

    val report = remember(sales, movements, startMs, endMs) {
        StockAnalytics.computePeriodReport(sales, movements, startMs, endMs)
    }

    val filteredMovements = remember(movements, historyFilter) {
        if (historyFilter == null) movements
        else movements.filter { it.typeEnum() == historyFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Rapports & Historique",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ReportTab.entries.forEachIndexed { idx, tab ->
                SegmentedButton(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    shape = SegmentedButtonDefaults.itemShape(idx, ReportTab.entries.size)
                ) {
                    Text(tab.label)
                }
            }
        }

        if (selectedTab == ReportTab.REPORTS) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PeriodMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = periodMode == mode,
                        onClick = {
                            periodMode = mode
                            periodOffset = 0
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, PeriodMode.entries.size)
                    ) {
                        Text(mode.label)
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { periodOffset-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Période précédente")
                    }
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { if (periodOffset < 0) periodOffset++ },
                        enabled = periodOffset < 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Période suivante")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        KpiMetricCard(
                            title = "Ventes (${report.salesCount})",
                            value = Formatters.formatFcfa(report.totalSalesAmount, store.currency),
                            subtitle = "${report.totalUnitsSold} unités",
                            icon = Icons.Default.Assessment,
                            accentColor = MbgGreenPrimary,
                            containerColor = MbgGreenContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Achats / Entrées (${report.entriesCount})",
                            value = Formatters.formatFcfa(report.totalPurchaseAmount, store.currency),
                            subtitle = "${report.totalUnitsReceived} unités",
                            icon = Icons.Default.ArrowDownward,
                            accentColor = MbgBlueAccent,
                            containerColor = MbgBlueContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Produits les plus vendus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (report.topProducts.isEmpty()) {
                                Text(
                                    text = "Aucune vente enregistrée sur cette période.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                report.topProducts.forEachIndexed { index, summary ->
                                    if (index > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${index + 1}. ${summary.productName}",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "${summary.unitsSold} unité(s) vendue(s)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = Formatters.formatFcfa(summary.revenue, store.currency),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MbgGreenPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = historyFilter == null,
                    onClick = { historyFilter = null },
                    label = { Text("Tout (${movements.size})") }
                )
                FilterChip(
                    selected = historyFilter == MovementType.ENTREE,
                    onClick = { historyFilter = if (historyFilter == MovementType.ENTREE) null else MovementType.ENTREE },
                    label = { Text("Entrées") }
                )
                FilterChip(
                    selected = historyFilter == MovementType.SORTIE,
                    onClick = { historyFilter = if (historyFilter == MovementType.SORTIE) null else MovementType.SORTIE },
                    label = { Text("Ventes / Sorties") }
                )
            }

            if (filteredMovements.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune opération enregistrée.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMovements, key = { it.id }) { mov ->
                        MovementHistoryCard(mov = mov, currency = store.currency)
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementHistoryCard(mov: StockMovement, currency: String) {
    val isEntry = mov.typeEnum() == MovementType.ENTREE
    val accent = if (isEntry) MbgBlueAccent else MbgYellowAccent
    val badgeBg = if (isEntry) MbgBlueContainer else MbgYellowContainer
    val icon = if (isEntry) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = badgeBg
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = mov.typeEnum().label,
                    tint = accent,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mov.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Formatters.formatFcfa(mov.totalAmount, currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isEntry) MbgBlueAccent else MbgGreenPrimary
                    )
                }
                val sign = if (isEntry) "+" else "-"
                Text(
                    text = "$sign${mov.quantity} unité(s) • Stock : ${mov.stockBefore} → ${mov.stockAfter}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Formatters.formatDateTime(mov.operationDateMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = mov.operatorName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RoleBadge(role = mov.operatorRoleEnum())
                }
            }
        }
    }
}
