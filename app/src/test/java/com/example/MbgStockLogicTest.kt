package com.example

import com.example.model.MovementType
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.RolePermissions
import com.example.model.SaleLine
import com.example.model.SaleRecord
import com.example.model.StockAnalytics
import com.example.model.StockMovement
import com.example.model.StockStatus
import com.example.model.UserRole
import com.example.util.Formatters
import com.example.util.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MbgStockLogicTest {

    @Test
    fun `role permissions enforce strict separation across all three profiles`() {
        // PROPRIETAIRE: full access
        assertTrue(RolePermissions.canAccessStockEntry(UserRole.PROPRIETAIRE))
        assertTrue(RolePermissions.canMakeSale(UserRole.PROPRIETAIRE))
        assertTrue(RolePermissions.canAddProduct(UserRole.PROPRIETAIRE))
        assertTrue(RolePermissions.canEditProduct(UserRole.PROPRIETAIRE))
        assertTrue(RolePermissions.canDeleteProduct(UserRole.PROPRIETAIRE))
        assertTrue(RolePermissions.canViewReports(UserRole.PROPRIETAIRE))
        assertTrue(RolePermissions.canManageUsers(UserRole.PROPRIETAIRE))
        assertTrue(RolePermissions.canEditSettings(UserRole.PROPRIETAIRE))

        // GESTIONNAIRE: entries, sales, view stock, add products, reports/history; NO edit/delete/users/settings
        assertTrue(RolePermissions.canAccessStockEntry(UserRole.GESTIONNAIRE))
        assertTrue(RolePermissions.canMakeSale(UserRole.GESTIONNAIRE))
        assertTrue(RolePermissions.canAddProduct(UserRole.GESTIONNAIRE))
        assertFalse(RolePermissions.canEditProduct(UserRole.GESTIONNAIRE))
        assertFalse(RolePermissions.canDeleteProduct(UserRole.GESTIONNAIRE))
        assertTrue(RolePermissions.canViewReports(UserRole.GESTIONNAIRE))
        assertFalse(RolePermissions.canManageUsers(UserRole.GESTIONNAIRE))
        assertFalse(RolePermissions.canEditSettings(UserRole.GESTIONNAIRE))

        // GERANT: sales & view stock ONLY; NO entries, NO add/edit/delete, NO reports/users/settings
        assertFalse(RolePermissions.canAccessStockEntry(UserRole.GERANT))
        assertTrue(RolePermissions.canMakeSale(UserRole.GERANT))
        assertFalse(RolePermissions.canAddProduct(UserRole.GERANT))
        assertFalse(RolePermissions.canEditProduct(UserRole.GERANT))
        assertFalse(RolePermissions.canDeleteProduct(UserRole.GERANT))
        assertFalse(RolePermissions.canViewPurchasePrices(UserRole.GERANT))
        assertFalse(RolePermissions.canViewReports(UserRole.GERANT))
        assertFalse(RolePermissions.canManageUsers(UserRole.GERANT))
        assertFalse(RolePermissions.canEditSettings(UserRole.GERANT))
    }

    @Test
    fun `password hashing protects passwords with unique salts and verifies accurately`() {
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()
        assertNotEquals(salt1, salt2)

        val hash1 = PasswordHasher.hashPassword(salt1, "Gabon2026!")
        val hash2 = PasswordHasher.hashPassword(salt2, "Gabon2026!")
        assertEquals(64, hash1.length)
        assertNotEquals(hash1, hash2)
        assertNotEquals("Gabon2026!", hash1)

        assertTrue(PasswordHasher.verifyPassword("Gabon2026!", salt1, hash1))
        assertFalse(PasswordHasher.verifyPassword("WrongPass", salt1, hash1))
    }

    @Test
    fun `stock status distinguishes normal, minimum alert, and out of stock`() {
        val regab = Product(id = "1", name = "Régab", category = ProductCategory.BOISSONS.name, quantity = 10L, minThreshold = 10L)
        val castel = Product(id = "2", name = "Castel", category = ProductCategory.BOISSONS.name, quantity = 25L, minThreshold = 15L)
        val beaufort = Product(id = "3", name = "Beaufort", category = ProductCategory.BOISSONS.name, quantity = 0L, minThreshold = 5L)

        assertEquals(StockStatus.ALERTE, regab.status())
        assertEquals(StockStatus.NORMAL, castel.status())
        assertEquals(StockStatus.RUPTURE, beaufort.status())
    }

    @Test
    fun `dashboard stats and period reports compute KPIs accurately`() {
        val products = listOf(
            Product(id = "p1", name = "Régab", category = ProductCategory.BOISSONS.name, quantity = 8L, minThreshold = 10L, sellingPrice = 700L),
            Product(id = "p2", name = "Castel", category = ProductCategory.BOISSONS.name, quantity = 22L, minThreshold = 15L, sellingPrice = 700L),
            Product(id = "p3", name = "Beaufort", category = ProductCategory.BOISSONS.name, quantity = 0L, minThreshold = 5L, sellingPrice = 700L),
            Product(id = "p4", name = "Gobelets", category = ProductCategory.AUTRES.name, quantity = 40L, minThreshold = 10L, sellingPrice = 100L)
        )
        val now = 1_700_000_000_000L
        val sales = listOf(
            SaleRecord(
                id = "s1",
                totalAmount = 7000L,
                totalUnits = 10L,
                lines = listOf(
                    SaleLine("p1", "Régab", 5L, 700L, 3500L),
                    SaleLine("p2", "Castel", 3L, 700L, 2100L),
                    SaleLine("p3", "Beaufort", 2L, 700L, 1400L)
                ),
                saleDateMs = now
            )
        )
        val movements = listOf(
            StockMovement(
                id = "m1",
                type = MovementType.ENTREE.name,
                productId = "p2",
                productName = "Castel",
                quantity = 24L,
                totalAmount = 12000L,
                operationDateMs = now
            )
        )

        val stats = StockAnalytics.computeDashboardStats(products, sales)
        assertEquals(4, stats.totalProducts)
        assertEquals(70L, stats.totalAvailableQuantity)
        assertEquals(30L, stats.availableBeverageUnits)
        assertEquals(2, stats.availableBeverageProducts)
        assertEquals(1, stats.outOfStockCount)
        assertEquals(1, stats.lowStockAlertCount)
        assertEquals(7000L, stats.totalSalesAmount)
        assertEquals(2, stats.alertProducts.size)

        val report = StockAnalytics.computePeriodReport(sales, movements, now - 1000L, now + 1000L)
        assertEquals(1, report.salesCount)
        assertEquals(7000L, report.totalSalesAmount)
        assertEquals(10L, report.totalUnitsSold)
        assertEquals(1, report.entriesCount)
        assertEquals(24L, report.totalUnitsReceived)
        assertEquals(12000L, report.totalPurchaseAmount)
        assertEquals("Régab", report.topProducts.first().productName)

        assertEquals("7 000 FCFA", Formatters.formatFcfa(7000L))
    }
}
