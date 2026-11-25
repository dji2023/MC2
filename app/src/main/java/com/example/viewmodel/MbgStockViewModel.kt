package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.SessionPreferences
import com.example.data.MbgStockRepository
import com.example.model.CartItem
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.RolePermissions
import com.example.model.SaleRecord
import com.example.model.StaffAccount
import com.example.model.StockMovement
import com.example.model.Store
import com.example.model.UserRole
import com.example.util.PasswordHasher
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class FeedbackMessage(
    val text: String,
    val isError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MbgStockViewModel(
    private val repository: MbgStockRepository,
    private val sessionPreferences: SessionPreferences,
    private val currentUserId: String,
    private val currentUserEmail: String?
) : ViewModel() {

    private val _selectedStoreId = MutableStateFlow<String?>(null)
    private val _activeOperatorId = MutableStateFlow<String?>(null)
    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _isSubmitting = MutableStateFlow(false)
    private val _feedback = MutableStateFlow<FeedbackMessage?>(null)
    private val _loginError = MutableStateFlow<String?>(null)

    private var pendingSaleId: String = UUID.randomUUID().toString()

    val cart: StateFlow<Map<String, Int>> = _cart.asStateFlow()
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()
    val feedback: StateFlow<FeedbackMessage?> = _feedback.asStateFlow()
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    val storesState: StateFlow<UiState<List<Store>>> = repository.observeStoresForUser(currentUserId)
        .asUiState("Impossible de charger votre boutique.")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeStore: StateFlow<Store?> = combine(storesState, _selectedStoreId) { state, selectedId ->
        val stores = (state as? UiState.Success)?.data ?: return@combine null
        if (selectedId != null) stores.firstOrNull { it.id == selectedId } ?: stores.firstOrNull()
        else stores.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    val staffState: StateFlow<UiState<List<StaffAccount>>> = activeStore
        .flatMapLatest { store ->
            if (store == null) flowOf(UiState.Success(emptyList()))
            else repository.observeStaff(store.id, currentUserId)
                .asUiState("Impossible de charger les comptes utilisateurs.")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val productsState: StateFlow<UiState<List<Product>>> = activeStore
        .flatMapLatest { store ->
            if (store == null) flowOf(UiState.Success(emptyList()))
            else repository.observeProducts(store.id, currentUserId)
                .asUiState("Impossible de charger les produits.")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val movementsState: StateFlow<UiState<List<StockMovement>>> = activeStore
        .flatMapLatest { store ->
            if (store == null) flowOf(UiState.Success(emptyList()))
            else repository.observeRecentMovements(store.id, currentUserId)
                .asUiState("Impossible de charger l'historique.")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val salesState: StateFlow<UiState<List<SaleRecord>>> = activeStore
        .flatMapLatest { store ->
            if (store == null) flowOf(UiState.Success(emptyList()))
            else repository.observeRecentSales(store.id, currentUserId)
                .asUiState("Impossible de charger les ventes.")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeOperator: StateFlow<StaffAccount?> = combine(staffState, _activeOperatorId) { state, opId ->
        val list = (state as? UiState.Success)?.data ?: return@combine null
        if (opId == null) null else list.firstOrNull { it.id == opId && it.active }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            val (savedStoreId, savedOpId) = sessionPreferences.savedSession.first()
            if (savedStoreId != null) _selectedStoreId.value = savedStoreId
            if (savedOpId != null) _activeOperatorId.value = savedOpId
            // Check if current Google account has a pending invitation to an existing store
            repository.findAndAcceptInvitedStores(currentUserEmail)
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun selectStore(storeId: String) {
        _selectedStoreId.value = storeId
        _activeOperatorId.value = null
        _cart.value = emptyMap()
    }

    fun checkInvites() {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.findAndAcceptInvitedStores(currentUserEmail)
            _isSubmitting.value = false
            res.onSuccess { list ->
                if (list.isEmpty()) {
                    _feedback.value = FeedbackMessage("Aucune invitation trouvée pour ${currentUserEmail ?: "cet email"}.", true)
                } else {
                    _feedback.value = FeedbackMessage("Boutique rejointe avec succès !")
                }
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Erreur lors de la vérification.", true)
            }
        }
    }

    fun createStoreAndOwner(
        storeName: String,
        ownerUsername: String,
        ownerDisplayName: String,
        ownerPassword: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createStoreWithOwner(
                storeName = storeName,
                ownerUsername = ownerUsername,
                ownerDisplayName = ownerDisplayName,
                ownerPassword = ownerPassword
            )
            _isSubmitting.value = false
            result.onSuccess { store ->
                _selectedStoreId.value = store.id
                _activeOperatorId.value = "owner_primary"
                sessionPreferences.saveActiveOperator(store.id, "owner_primary")
                _feedback.value = FeedbackMessage("Espace ${store.storeName} créé avec succès !")
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Erreur de création.", true)
            }
        }
    }

    fun loginOperator(username: String, password: String) {
        val cleanUser = username.trim().lowercase()
        val staffList = (staffState.value as? UiState.Success)?.data ?: emptyList()
        val account = staffList.firstOrNull { it.username.equals(cleanUser, ignoreCase = true) }
        if (account == null) {
            _loginError.value = "Identifiant inconnu."
            return
        }
        if (!account.active) {
            _loginError.value = "Ce compte est désactivé. Contactez le propriétaire."
            return
        }
        if (!PasswordHasher.verifyPassword(password, account.passwordSalt, account.passwordHash)) {
            _loginError.value = "Mot de passe incorrect."
            return
        }
        _loginError.value = null
        _activeOperatorId.value = account.id
        _cart.value = emptyMap()
        val storeId = activeStore.value?.id ?: return
        viewModelScope.launch {
            sessionPreferences.saveActiveOperator(storeId, account.id)
        }
    }

    fun logoutOperator() {
        _activeOperatorId.value = null
        _cart.value = emptyMap()
        _loginError.value = null
        viewModelScope.launch {
            sessionPreferences.clearActiveOperator()
        }
    }

    fun clearSessionOnSignOut() {
        _activeOperatorId.value = null
        _cart.value = emptyMap()
        viewModelScope.launch {
            sessionPreferences.clearAll()
        }
    }

    // --- Cart & Multi-Product Sales ---

    fun incrementCart(product: Product) {
        if (product.quantity <= 0L) {
            _feedback.value = FeedbackMessage("${product.name} est en rupture de stock.", true)
            return
        }
        val currentQty = _cart.value[product.id] ?: 0
        if (currentQty.toLong() >= product.quantity) {
            _feedback.value = FeedbackMessage("Stock maximum atteint pour ${product.name} (${product.quantity}).", true)
            return
        }
        if (currentQty == 0 && _cart.value.size >= 50) {
            _feedback.value = FeedbackMessage("Maximum 50 lignes par vente.", true)
            return
        }
        _cart.value = _cart.value + (product.id to (currentQty + 1))
    }

    fun decrementCart(productId: String) {
        val currentQty = _cart.value[productId] ?: 0
        if (currentQty <= 1) {
            _cart.value = _cart.value - productId
        } else {
            _cart.value = _cart.value + (productId to (currentQty - 1))
        }
    }

    fun setCartQuantity(product: Product, desiredQty: Int) {
        if (desiredQty <= 0) {
            _cart.value = _cart.value - product.id
            return
        }
        if (product.quantity <= 0L) {
            _cart.value = _cart.value - product.id
            _feedback.value = FeedbackMessage("${product.name} est en rupture de stock.", true)
            return
        }
        val clamped = desiredQty.toLong().coerceAtMost(product.quantity).toInt()
        if (desiredQty.toLong() > product.quantity) {
            _feedback.value = FeedbackMessage("Stock insuffisant : ${product.quantity} disponible(s) pour ${product.name}.", true)
        }
        _cart.value = _cart.value + (product.id to clamped)
    }

    fun clearCart() {
        _cart.value = emptyMap()
        pendingSaleId = UUID.randomUUID().toString()
    }

    fun submitSale(onSuccess: (SaleRecord) -> Unit = {}) {
        val store = activeStore.value ?: return
        val operator = activeOperator.value ?: return
        val products = (productsState.value as? UiState.Success)?.data ?: return
        if (_isSubmitting.value) return

        val cartItems = _cart.value.mapNotNull { (prodId, qty) ->
            val product = products.firstOrNull { it.id == prodId } ?: return@mapNotNull null
            if (qty > 0) CartItem(product, qty) else null
        }
        if (cartItems.isEmpty()) {
            _feedback.value = FeedbackMessage("Ajoutez au moins un produit au panier.", true)
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.recordSale(
                store = store,
                actor = operator,
                saleId = pendingSaleId,
                cartItems = cartItems
            )
            _isSubmitting.value = false
            result.onSuccess { sale ->
                _cart.value = emptyMap()
                pendingSaleId = UUID.randomUUID().toString()
                _feedback.value = FeedbackMessage("Vente validée : ${sale.itemsSummary}")
                onSuccess(sale)
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Échec de la vente.", true)
            }
        }
    }

    // --- Stock Entry ---

    fun submitStockEntry(
        productId: String,
        cratesCount: Long,
        extraBottles: Long,
        unitsPerCrate: Long,
        purchasePrice: Long,
        receptionDateMs: Long,
        note: String?,
        onSuccess: () -> Unit
    ) {
        val store = activeStore.value ?: return
        val operator = activeOperator.value ?: return
        if (!RolePermissions.canAccessStockEntry(operator.roleEnum())) {
            _feedback.value = FeedbackMessage("Accès refusé : réservé au Propriétaire ou Gestionnaire.", true)
            return
        }
        if (_isSubmitting.value) return

        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.recordStockEntry(
                store = store,
                actor = operator,
                productId = productId,
                cratesCount = cratesCount,
                extraBottles = extraBottles,
                unitsPerCrate = unitsPerCrate,
                purchasePrice = purchasePrice,
                receptionDateMs = receptionDateMs,
                note = note
            )
            _isSubmitting.value = false
            result.onSuccess { newStock ->
                _feedback.value = FeedbackMessage("Entrée enregistrée ! Nouveau stock : $newStock unités.")
                onSuccess()
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Erreur lors de l'entrée de stock.", true)
            }
        }
    }

    // --- Products ---

    fun addProduct(
        name: String,
        category: ProductCategory,
        purchasePrice: Long,
        sellingPrice: Long,
        initialQuantity: Long,
        minThreshold: Long,
        unitsPerCrate: Long,
        onSuccess: () -> Unit
    ) {
        val store = activeStore.value ?: return
        val operator = activeOperator.value ?: return
        if (!RolePermissions.canAddProduct(operator.roleEnum())) {
            _feedback.value = FeedbackMessage("Accès refusé : vous ne pouvez pas ajouter de produit.", true)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.addProduct(
                store = store,
                actor = operator,
                name = name,
                category = category,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                initialQuantity = initialQuantity,
                minThreshold = minThreshold,
                unitsPerCrate = unitsPerCrate
            )
            _isSubmitting.value = false
            res.onSuccess {
                _feedback.value = FeedbackMessage("Produit « $name » ajouté.")
                onSuccess()
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Erreur lors de l'ajout.", true)
            }
        }
    }

    fun updateProduct(
        product: Product,
        name: String,
        category: ProductCategory,
        purchasePrice: Long,
        sellingPrice: Long,
        quantity: Long,
        minThreshold: Long,
        unitsPerCrate: Long,
        onSuccess: () -> Unit
    ) {
        val store = activeStore.value ?: return
        val operator = activeOperator.value ?: return
        if (!RolePermissions.canEditProduct(operator.roleEnum())) {
            _feedback.value = FeedbackMessage("Accès refusé : seul le Propriétaire peut modifier un produit.", true)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.updateProduct(
                store = store,
                actor = operator,
                product = product,
                name = name,
                category = category,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                quantity = quantity,
                minThreshold = minThreshold,
                unitsPerCrate = unitsPerCrate
            )
            _isSubmitting.value = false
            res.onSuccess {
                _feedback.value = FeedbackMessage("Produit « $name » mis à jour.")
                onSuccess()
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Erreur de mise à jour.", true)
            }
        }
    }

    fun deleteProduct(product: Product) {
        val store = activeStore.value ?: return
        val operator = activeOperator.value ?: return
        if (!RolePermissions.canDeleteProduct(operator.roleEnum())) {
            _feedback.value = FeedbackMessage("Accès refusé : seul le Propriétaire peut supprimer un produit.", true)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.deleteProduct(store, operator, product.id)
            _isSubmitting.value = false
            res.onSuccess {
                _feedback.value = FeedbackMessage("Produit « ${product.name} » supprimé.")
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Erreur de suppression.", true)
            }
        }
    }

    // --- Users & Store Settings ---

    fun saveStaffAccount(
        existingStaffId: String?,
        username: String,
        displayName: String,
        role: UserRole,
        newPassword: String?,
        active: Boolean,
        linkedEmail: String?,
        onSuccess: () -> Unit
    ) {
        val store = activeStore.value ?: return
        val operator = activeOperator.value ?: return
        if (!RolePermissions.canManageUsers(operator.roleEnum())) {
            _feedback.value = FeedbackMessage("Accès refusé : réservé au Propriétaire.", true)
            return
        }
        val staffList = (staffState.value as? UiState.Success)?.data ?: emptyList()
        val duplicate = staffList.any {
            it.username.equals(username.trim(), ignoreCase = true) && it.id != existingStaffId
        }
        if (duplicate) {
            _feedback.value = FeedbackMessage("Cet identifiant est déjà utilisé.", true)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.saveStaffAccount(
                store = store,
                actorRole = operator.roleEnum(),
                existingStaffId = existingStaffId,
                username = username,
                displayName = displayName,
                role = role,
                newPassword = newPassword,
                active = active,
                linkedEmail = linkedEmail
            )
            _isSubmitting.value = false
            res.onSuccess {
                _feedback.value = FeedbackMessage("Compte utilisateur enregistré.")
                onSuccess()
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Erreur d'enregistrement.", true)
            }
        }
    }

    fun deleteStaffAccount(staff: StaffAccount) {
        val store = activeStore.value ?: return
        val operator = activeOperator.value ?: return
        val staffList = (staffState.value as? UiState.Success)?.data ?: emptyList()
        if (!RolePermissions.canManageUsers(operator.roleEnum())) {
            _feedback.value = FeedbackMessage("Accès refusé.", true)
            return
        }
        if (staff.id == operator.id) {
            _feedback.value = FeedbackMessage("Vous ne pouvez pas supprimer votre propre session active.", true)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.deleteStaffAccount(store, operator.roleEnum(), staff, staffList)
            _isSubmitting.value = false
            res.onSuccess {
                _feedback.value = FeedbackMessage("Compte ${staff.displayName} supprimé.")
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Suppression impossible.", true)
            }
        }
    }

    fun updateStoreSettings(
        storeName: String,
        defaultUnitsPerCrate: Long,
        defaultMinThreshold: Long,
        dataSaverEnabled: Boolean
    ) {
        val store = activeStore.value ?: return
        val operator = activeOperator.value ?: return
        if (!RolePermissions.canEditSettings(operator.roleEnum())) {
            _feedback.value = FeedbackMessage("Accès refusé : réservé au Propriétaire.", true)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.updateStoreSettings(
                storeId = store.id,
                actorRole = operator.roleEnum(),
                storeName = storeName,
                defaultUnitsPerCrate = defaultUnitsPerCrate,
                defaultMinThreshold = defaultMinThreshold,
                dataSaverEnabled = dataSaverEnabled
            )
            _isSubmitting.value = false
            res.onSuccess {
                _feedback.value = FeedbackMessage("Paramètres mis à jour.")
            }.onFailure { err ->
                _feedback.value = FeedbackMessage(err.localizedMessage ?: "Erreur de mise à jour.", true)
            }
        }
    }

    private fun <T> Flow<T>.asUiState(defaultErrorMsg: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream error", rootError)
                emit(UiState.Error(rootError.message ?: defaultErrorMsg))
            }

    private companion object {
        const val TAG = "MbgStockVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
