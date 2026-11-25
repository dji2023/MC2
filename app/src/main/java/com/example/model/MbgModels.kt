package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String, val shortLabel: String) {
    PROPRIETAIRE("Propriétaire", "Propriétaire"),
    GESTIONNAIRE("Gestionnaire de stock", "Gestionnaire"),
    GERANT("Gérant / Employé", "Gérant");

    companion object {
        fun fromRaw(raw: String?): UserRole {
            return entries.firstOrNull { it.name == raw } ?: GERANT
        }
    }
}

enum class ProductCategory(val label: String) {
    BOISSONS("Boissons"),
    JUS("Jus"),
    EAU("Eau"),
    AUTRES("Autres marchandises");

    fun isBeverage(): Boolean = this == BOISSONS || this == JUS || this == EAU

    companion object {
        fun fromRaw(raw: String?): ProductCategory {
            return entries.firstOrNull { it.name == raw } ?: AUTRES
        }
    }
}

enum class MovementType(val label: String) {
    ENTREE("Entrée de stock"),
    SORTIE("Sortie / Vente"),
    AJUSTEMENT("Ajustement");

    companion object {
        fun fromRaw(raw: String?): MovementType {
            return entries.firstOrNull { it.name == raw } ?: SORTIE
        }
    }
}

enum class StockStatus(val label: String) {
    NORMAL("Disponible"),
    ALERTE("Stock faible"),
    RUPTURE("Rupture")
}

object RolePermissions {
    fun canAccessStockEntry(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE || role == UserRole.GESTIONNAIRE

    fun canMakeSale(role: UserRole): Boolean = true

    fun canAddProduct(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE || role == UserRole.GESTIONNAIRE

    fun canEditProduct(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE

    fun canDeleteProduct(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE

    fun canViewPurchasePrices(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE || role == UserRole.GESTIONNAIRE

    fun canViewAlerts(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE || role == UserRole.GESTIONNAIRE

    fun canViewReports(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE || role == UserRole.GESTIONNAIRE

    fun canViewHistory(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE || role == UserRole.GESTIONNAIRE

    fun canManageUsers(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE

    fun canEditSettings(role: UserRole): Boolean =
        role == UserRole.PROPRIETAIRE
}

@Keep
@IgnoreExtraProperties
data class Store(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val invitedEmails: List<String> = emptyList(),
    val storeName: String = "MBG STOCK",
    val currency: String = "FCFA",
    val defaultUnitsPerCrate: Long = 24L,
    val defaultMinThreshold: Long = 10L,
    val dataSaverEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StaffAccount(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val username: String = "",
    val displayName: String = "",
    val role: String = UserRole.GERANT.name,
    val passwordSalt: String = "",
    val passwordHash: String = "",
    val active: Boolean = true,
    val linkedEmail: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = UserRole.fromRaw(role)
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val category: String = ProductCategory.BOISSONS.name,
    val purchasePrice: Long = 0L,
    val sellingPrice: Long = 0L,
    val quantity: Long = 0L,
    val minThreshold: Long = 10L,
    val unitsPerCrate: Long = 24L,
    val createdByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): ProductCategory = ProductCategory.fromRaw(category)

    fun status(): StockStatus = when {
        quantity <= 0L -> StockStatus.RUPTURE
        quantity <= minThreshold -> StockStatus.ALERTE
        else -> StockStatus.NORMAL
    }

    fun cratesCount(): Long = if (unitsPerCrate > 0L) quantity / unitsPerCrate else 0L

    fun remainingBottles(): Long = if (unitsPerCrate > 0L) quantity % unitsPerCrate else quantity
}

@Keep
@IgnoreExtraProperties
data class StockMovement(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val type: String = MovementType.SORTIE.name,
    val productId: String = "",
    val productName: String = "",
    val cratesCount: Long = 0L,
    val extraBottles: Long = 0L,
    val unitsPerCrate: Long = 1L,
    val quantity: Long = 0L,
    val stockBefore: Long = 0L,
    val stockAfter: Long = 0L,
    val unitPrice: Long = 0L,
    val totalAmount: Long = 0L,
    val operatorId: String = "",
    val operatorName: String = "",
    val operatorRole: String = UserRole.GERANT.name,
    val referenceId: String = "",
    val operationDateMs: Long = 0L,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun typeEnum(): MovementType = MovementType.fromRaw(type)
    fun operatorRoleEnum(): UserRole = UserRole.fromRaw(operatorRole)
}

@Keep
@IgnoreExtraProperties
data class SaleLine(
    val productId: String = "",
    val productName: String = "",
    val quantity: Long = 0L,
    val unitPrice: Long = 0L,
    val lineTotal: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class SaleRecord(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val operatorId: String = "",
    val operatorName: String = "",
    val operatorRole: String = UserRole.GERANT.name,
    val totalAmount: Long = 0L,
    val totalUnits: Long = 0L,
    val itemCount: Long = 0L,
    val itemsSummary: String = "",
    val lines: List<SaleLine> = emptyList(),
    val saleDateMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun operatorRoleEnum(): UserRole = UserRole.fromRaw(operatorRole)
}

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val lineTotal: Long get() = product.sellingPrice * quantity.toLong()
}

data class DashboardStats(
    val totalProducts: Int = 0,
    val totalAvailableQuantity: Long = 0L,
    val availableBeverageUnits: Long = 0L,
    val availableBeverageProducts: Int = 0,
    val outOfStockCount: Int = 0,
    val lowStockAlertCount: Int = 0,
    val totalSalesAmount: Long = 0L,
    val totalSalesCount: Int = 0,
    val alertProducts: List<Product> = emptyList()
)

data class ProductSalesSummary(
    val productName: String,
    val unitsSold: Long,
    val revenue: Long
)

data class PeriodReport(
    val salesCount: Int = 0,
    val totalSalesAmount: Long = 0L,
    val totalUnitsSold: Long = 0L,
    val entriesCount: Int = 0,
    val totalUnitsReceived: Long = 0L,
    val totalPurchaseAmount: Long = 0L,
    val topProducts: List<ProductSalesSummary> = emptyList()
)

object StockAnalytics {
    fun computeDashboardStats(
        products: List<Product>,
        sales: List<SaleRecord>
    ): DashboardStats {
        val totalProducts = products.size
        val totalAvailableQuantity = products.sumOf { it.quantity.coerceAtLeast(0L) }
        val beverageProducts = products.filter { it.categoryEnum().isBeverage() && it.quantity > 0L }
        val availableBeverageUnits = beverageProducts.sumOf { it.quantity }
        val outOfStockCount = products.count { it.status() == StockStatus.RUPTURE }
        val lowStockProducts = products.filter { it.status() == StockStatus.ALERTE }
        val alertProducts = products
            .filter { it.status() == StockStatus.RUPTURE || it.status() == StockStatus.ALERTE }
            .sortedWith(compareBy<Product> { it.quantity }.thenBy { it.name })

        val totalSalesAmount = sales.sumOf { it.totalAmount }
        val totalSalesCount = sales.size

        return DashboardStats(
            totalProducts = totalProducts,
            totalAvailableQuantity = totalAvailableQuantity,
            availableBeverageUnits = availableBeverageUnits,
            availableBeverageProducts = beverageProducts.size,
            outOfStockCount = outOfStockCount,
            lowStockAlertCount = lowStockProducts.size,
            totalSalesAmount = totalSalesAmount,
            totalSalesCount = totalSalesCount,
            alertProducts = alertProducts
        )
    }

    fun computePeriodReport(
        sales: List<SaleRecord>,
        movements: List<StockMovement>,
        startMs: Long,
        endMs: Long
    ): PeriodReport {
        val periodSales = sales.filter { it.saleDateMs in startMs..endMs }
        val periodEntries = movements.filter {
            it.typeEnum() == MovementType.ENTREE && it.operationDateMs in startMs..endMs
        }

        val productTotals = mutableMapOf<String, Pair<Long, Long>>()
        for (sale in periodSales) {
            for (line in sale.lines) {
                val prev = productTotals[line.productName] ?: (0L to 0L)
                productTotals[line.productName] = (prev.first + line.quantity) to (prev.second + line.lineTotal)
            }
        }

        val topProducts = productTotals.entries
            .map { (name, pair) -> ProductSalesSummary(name, pair.first, pair.second) }
            .sortedByDescending { it.revenue }
            .take(8)

        return PeriodReport(
            salesCount = periodSales.size,
            totalSalesAmount = periodSales.sumOf { it.totalAmount },
            totalUnitsSold = periodSales.sumOf { it.totalUnits },
            entriesCount = periodEntries.size,
            totalUnitsReceived = periodEntries.sumOf { it.quantity },
            totalPurchaseAmount = periodEntries.sumOf { it.totalAmount },
            topProducts = topProducts
        )
    }
}
