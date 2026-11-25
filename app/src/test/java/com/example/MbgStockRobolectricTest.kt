package com.example

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.StaffAccount
import com.example.model.Store
import com.example.model.UserRole
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OperatorLoginScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.theme.MbgStockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MbgStockRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleStore = Store(
        id = "store_1",
        ownerUid = "uid_1",
        memberUids = listOf("uid_1"),
        storeName = "MBG STOCK Libreville",
        currency = "FCFA"
    )

    @Test
    fun `operator login screen displays logo, brand name, and submits credentials`() {
        var submittedUser = ""
        var submittedPass = ""
        val staff = listOf(
            StaffAccount(id = "s1", username = "patron", displayName = "Patron", role = UserRole.PROPRIETAIRE.name, active = true)
        )

        composeTestRule.setContent {
            MbgStockTheme {
                OperatorLoginScreen(
                    store = sampleStore,
                    staffList = staff,
                    loginError = null,
                    onLogin = { u, p ->
                        submittedUser = u
                        submittedPass = p
                    },
                    onClearError = {},
                    onSignOutGoogle = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("MBG STOCK").assertExists()
        composeTestRule.onNodeWithTag("operator_username_input").performTextInput("patron")
        composeTestRule.onNodeWithTag("operator_password_input").performTextInput("1234")
        composeTestRule.onNodeWithTag("operator_login_button").performClick()

        assertEquals("patron", submittedUser)
        assertEquals("1234", submittedPass)
    }

    @Test
    fun `dashboard hides stock entry and reports quick actions for gerant role`() {
        val gerant = StaffAccount(
            id = "g1",
            username = "gerant1",
            displayName = "Jean M.",
            role = UserRole.GERANT.name,
            active = true
        )

        composeTestRule.setContent {
            MbgStockTheme {
                DashboardScreen(
                    store = sampleStore,
                    operator = gerant,
                    products = emptyList(),
                    sales = emptyList(),
                    onGoToSales = {},
                    onGoToStockEntry = {},
                    onGoToProducts = {},
                    onGoToReports = {},
                    onSwitchOperator = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quick_action_sale").assertExists()
        composeTestRule.onNodeWithTag("quick_action_products").assertExists()
        composeTestRule.onNodeWithTag("quick_action_entry").assertDoesNotExist()
        composeTestRule.onNodeWithTag("quick_action_reports").assertDoesNotExist()
    }

    @Test
    fun `sales screen blocks zero stock product while allowing minimum-alert product`() {
        val products = listOf(
            Product(
                id = "regab",
                name = "Régab 65cl",
                category = ProductCategory.BOISSONS.name,
                sellingPrice = 700L,
                quantity = 5L, // <= minThreshold 10, still sellable!
                minThreshold = 10L
            ),
            Product(
                id = "beaufort",
                name = "Beaufort",
                category = ProductCategory.BOISSONS.name,
                sellingPrice = 700L,
                quantity = 0L, // Out of stock, must be disabled!
                minThreshold = 5L
            )
        )

        composeTestRule.setContent {
            MbgStockTheme {
                SalesScreen(
                    store = sampleStore,
                    products = products,
                    cartMap = emptyMap(),
                    isSubmitting = false,
                    onIncrement = {},
                    onDecrement = {},
                    onClearCart = {},
                    onSubmitSale = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_to_cart_regab").assertIsEnabled()
        composeTestRule.onNodeWithTag("add_to_cart_beaufort").assertIsNotEnabled()
    }
}
