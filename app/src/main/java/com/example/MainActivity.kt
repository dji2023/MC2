package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.SessionPreferences
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutGoogle
import com.example.data.MbgStockRepository
import com.example.model.RolePermissions
import com.example.model.StaffAccount
import com.example.model.Store
import com.example.model.UserRole
import com.example.ui.components.MbgLogoEmblem
import com.example.ui.screens.AdminUsersSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.OperatorLoginScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsHistoryScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.StockEntryScreen
import com.example.ui.screens.StoreOnboardingScreen
import com.example.ui.theme.MbgGreenPrimary
import com.example.ui.theme.MbgRedAlert
import com.example.ui.theme.MbgStockTheme
import com.example.viewmodel.MbgStockViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val tag: String
) {
    DASHBOARD("Accueil", Icons.Default.Dashboard, "nav_dashboard"),
    SALES("Ventes", Icons.Default.PointOfSale, "nav_sales"),
    STOCK_ENTRY("Entrées", Icons.Default.AddBox, "nav_entry"),
    PRODUCTS("Stock", Icons.Default.Inventory2, "nav_products"),
    REPORTS("Rapports", Icons.Default.Assessment, "nav_reports"),
    ADMIN("Gestion", Icons.Default.AdminPanelSettings, "nav_admin")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MbgStockTheme {
                MbgStockRoot()
            }
        }
    }
}

@Composable
fun MbgStockRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var autoSignInChecked by remember { mutableStateOf(Firebase.auth.currentUser != null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    autoSignInChecked = true
                },
                onUnauthenticated = {
                    autoSignInChecked = true
                },
                scope = scope
            )
        }
    }

    if (!autoSignInChecked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MbgLogoEmblem(size = 76.dp)
                CircularProgressIndicator(color = MbgGreenPrimary)
            }
        }
        return
    }

    val user = currentUser
    if (user == null) {
        GoogleAuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: MbgStockViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        MbgStockViewModel(
                            repository = MbgStockRepository(app),
                            sessionPreferences = SessionPreferences(app),
                            currentUserId = uid,
                            currentUserEmail = user.email
                        )
                    }
                }
            )
            AuthenticatedAppHost(
                viewModel = viewModel,
                userEmail = user.email,
                onSignOutGoogle = {
                    viewModel.clearSessionOnSignOut()
                    signOutGoogle(credentialManager, scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedAppHost(
    viewModel: MbgStockViewModel,
    userEmail: String?,
    onSignOutGoogle: () -> Unit
) {
    val storesState by viewModel.storesState.collectAsStateWithLifecycle()
    val activeStore by viewModel.activeStore.collectAsStateWithLifecycle()
    val staffState by viewModel.staffState.collectAsStateWithLifecycle()
    val activeOperator by viewModel.activeOperator.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val movementsState by viewModel.movementsState.collectAsStateWithLifecycle()
    val salesState by viewModel.salesState.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val msg = feedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg.text)
        viewModel.clearFeedback()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val isError = feedback?.isError == true
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isError) MbgRedAlert else MbgGreenPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val stores = storesState) {
                is UiState.Loading -> {
                    LoadingStateView("Chargement de votre espace MBG STOCK...")
                }

                is UiState.Error -> {
                    ErrorStateView(
                        message = stores.message,
                        onSignOut = onSignOutGoogle
                    )
                }

                is UiState.Success -> {
                    val store = activeStore
                    if (store == null) {
                        StoreOnboardingScreen(
                            userEmail = userEmail,
                            isSubmitting = isSubmitting,
                            onCreateStore = { sName, uName, dName, pwd ->
                                viewModel.createStoreAndOwner(sName, uName, dName, pwd)
                            },
                            onCheckInvites = { viewModel.checkInvites() },
                            onSignOutGoogle = onSignOutGoogle
                        )
                    } else {
                        val staffList = (staffState as? UiState.Success)?.data ?: emptyList()
                        val operator = activeOperator
                        if (staffState is UiState.Loading) {
                            LoadingStateView("Chargement des profils...")
                        } else if (operator == null) {
                            OperatorLoginScreen(
                                store = store,
                                staffList = staffList,
                                loginError = loginError,
                                onLogin = { username, password ->
                                    viewModel.loginOperator(username, password)
                                },
                                onClearError = { viewModel.clearLoginError() },
                                onSignOutGoogle = onSignOutGoogle
                            )
                        } else {
                            val products = (productsState as? UiState.Success)?.data ?: emptyList()
                            val movements = (movementsState as? UiState.Success)?.data ?: emptyList()
                            val sales = (salesState as? UiState.Success)?.data ?: emptyList()

                            AuthorizedWorkspace(
                                store = store,
                                operator = operator,
                                staffList = staffList,
                                products = products,
                                movements = movements,
                                sales = sales,
                                cartMap = cart,
                                isSubmitting = isSubmitting,
                                viewModel = viewModel,
                                onSignOutGoogle = onSignOutGoogle
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorizedWorkspace(
    store: Store,
    operator: StaffAccount,
    staffList: List<StaffAccount>,
    products: List<com.example.model.Product>,
    movements: List<com.example.model.StockMovement>,
    sales: List<com.example.model.SaleRecord>,
    cartMap: Map<String, Int>,
    isSubmitting: Boolean,
    viewModel: MbgStockViewModel,
    onSignOutGoogle: () -> Unit
) {
    val role = operator.roleEnum()
    val allowedTabs = remember(role) {
        buildList {
            add(AppTab.DASHBOARD)
            add(AppTab.SALES)
            if (RolePermissions.canAccessStockEntry(role)) add(AppTab.STOCK_ENTRY)
            add(AppTab.PRODUCTS)
            if (RolePermissions.canViewReports(role)) add(AppTab.REPORTS)
            if (RolePermissions.canManageUsers(role)) add(AppTab.ADMIN)
        }
    }

    var currentTab by remember(operator.id, role) { mutableStateOf(AppTab.DASHBOARD) }
    var preselectedEntryProductId by remember { mutableStateOf<String?>(null) }

    if (currentTab !in allowedTabs) {
        currentTab = AppTab.DASHBOARD
    }

    if (currentTab != AppTab.DASHBOARD) {
        BackHandler {
            currentTab = AppTab.DASHBOARD
        }
    }

    val navigateToTab: (AppTab) -> Unit = { tab ->
        if (tab in allowedTabs) {
            currentTab = tab
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            MbgLogoEmblem(size = 44.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "MBG",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MbgGreenPrimary
                            )
                        }
                    }
                ) {
                    allowedTabs.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab,
                            onClick = { navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TabContentHost(
                        currentTab = currentTab,
                        store = store,
                        operator = operator,
                        staffList = staffList,
                        products = products,
                        movements = movements,
                        sales = sales,
                        cartMap = cartMap,
                        isSubmitting = isSubmitting,
                        preselectedEntryProductId = preselectedEntryProductId,
                        viewModel = viewModel,
                        onNavigateToTab = navigateToTab,
                        onGoToStockEntry = { prodId ->
                            if (RolePermissions.canAccessStockEntry(role)) {
                                preselectedEntryProductId = prodId
                                currentTab = AppTab.STOCK_ENTRY
                            }
                        },
                        onSignOutGoogle = onSignOutGoogle
                    )
                }
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.navigationBars,
                bottomBar = {
                    NavigationBar {
                        allowedTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { navigateToTab(tab) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            ) { navPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(navPadding)
                ) {
                    TabContentHost(
                        currentTab = currentTab,
                        store = store,
                        operator = operator,
                        staffList = staffList,
                        products = products,
                        movements = movements,
                        sales = sales,
                        cartMap = cartMap,
                        isSubmitting = isSubmitting,
                        preselectedEntryProductId = preselectedEntryProductId,
                        viewModel = viewModel,
                        onNavigateToTab = navigateToTab,
                        onGoToStockEntry = { prodId ->
                            if (RolePermissions.canAccessStockEntry(role)) {
                                preselectedEntryProductId = prodId
                                currentTab = AppTab.STOCK_ENTRY
                            }
                        },
                        onSignOutGoogle = onSignOutGoogle
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContentHost(
    currentTab: AppTab,
    store: Store,
    operator: StaffAccount,
    staffList: List<StaffAccount>,
    products: List<com.example.model.Product>,
    movements: List<com.example.model.StockMovement>,
    sales: List<com.example.model.SaleRecord>,
    cartMap: Map<String, Int>,
    isSubmitting: Boolean,
    preselectedEntryProductId: String?,
    viewModel: MbgStockViewModel,
    onNavigateToTab: (AppTab) -> Unit,
    onGoToStockEntry: (String?) -> Unit,
    onSignOutGoogle: () -> Unit
) {
    val role = operator.roleEnum()

    when (currentTab) {
        AppTab.DASHBOARD -> DashboardScreen(
            store = store,
            operator = operator,
            products = products,
            sales = sales,
            onGoToSales = { onNavigateToTab(AppTab.SALES) },
            onGoToStockEntry = onGoToStockEntry,
            onGoToProducts = { onNavigateToTab(AppTab.PRODUCTS) },
            onGoToReports = { onNavigateToTab(AppTab.REPORTS) },
            onSwitchOperator = { viewModel.logoutOperator() }
        )

        AppTab.SALES -> SalesScreen(
            store = store,
            products = products,
            cartMap = cartMap,
            isSubmitting = isSubmitting,
            onIncrement = { viewModel.incrementCart(it) },
            onDecrement = { viewModel.decrementCart(it) },
            onClearCart = { viewModel.clearCart() },
            onSubmitSale = { viewModel.submitSale() }
        )

        AppTab.STOCK_ENTRY -> {
            if (RolePermissions.canAccessStockEntry(role)) {
                StockEntryScreen(
                    store = store,
                    products = products,
                    preselectedProductId = preselectedEntryProductId,
                    isSubmitting = isSubmitting,
                    onSubmitEntry = { prodId, crates, bottles, perCrate, price, dateMs, note, onDone ->
                        viewModel.submitStockEntry(
                            productId = prodId,
                            cratesCount = crates,
                            extraBottles = bottles,
                            unitsPerCrate = perCrate,
                            purchasePrice = price,
                            receptionDateMs = dateMs,
                            note = note,
                            onSuccess = onDone
                        )
                    }
                )
            }
        }

        AppTab.PRODUCTS -> ProductsScreen(
            store = store,
            operator = operator,
            products = products,
            isSubmitting = isSubmitting,
            onAddProduct = { name, cat, purchase, selling, qty, threshold, perCrate, onDone ->
                viewModel.addProduct(name, cat, purchase, selling, qty, threshold, perCrate, onDone)
            },
            onUpdateProduct = { product, name, cat, purchase, selling, qty, threshold, perCrate, onDone ->
                viewModel.updateProduct(product, name, cat, purchase, selling, qty, threshold, perCrate, onDone)
            },
            onDeleteProduct = { viewModel.deleteProduct(it) }
        )

        AppTab.REPORTS -> {
            if (RolePermissions.canViewReports(role)) {
                ReportsHistoryScreen(
                    store = store,
                    movements = movements,
                    sales = sales
                )
            }
        }

        AppTab.ADMIN -> {
            if (RolePermissions.canManageUsers(role) && role == UserRole.PROPRIETAIRE) {
                AdminUsersSettingsScreen(
                    store = store,
                    activeOperator = operator,
                    staffList = staffList,
                    isSubmitting = isSubmitting,
                    onSaveStaff = { existingId, username, displayName, staffRole, pwd, active, email, onDone ->
                        viewModel.saveStaffAccount(existingId, username, displayName, staffRole, pwd, active, email, onDone)
                    },
                    onDeleteStaff = { viewModel.deleteStaffAccount(it) },
                    onUpdateSettings = { name, perCrate, threshold, dataSaver ->
                        viewModel.updateStoreSettings(name, perCrate, threshold, dataSaver)
                    },
                    onSignOutGoogle = onSignOutGoogle
                )
            }
        }
    }
}

@Composable
private fun LoadingStateView(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MbgGreenPrimary)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorStateView(message: String, onSignOut: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Erreur de synchronisation",
                style = MaterialTheme.typography.titleLarge,
                color = MbgRedAlert
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onSignOut) {
                Text("Se déconnecter")
            }
        }
    }
}
