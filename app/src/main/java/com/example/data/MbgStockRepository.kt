package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CartItem
import com.example.model.MovementType
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.RolePermissions
import com.example.model.SaleLine
import com.example.model.SaleRecord
import com.example.model.StaffAccount
import com.example.model.StockMovement
import com.example.model.Store
import com.example.model.UserRole
import com.example.util.Formatters
import com.example.util.PasswordHasher
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MbgStockRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(createConfiguredFirestore(context.applicationContext))

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Connexion Google requise avant d'accéder aux données.")
    }

    fun observeStoresForUser(userId: String): Flow<List<Store>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Utilisateur modifié." }
        val path = "stores"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Store::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun findAndAcceptInvitedStores(email: String?): Result<List<Store>> {
        if (email.isNullOrBlank()) return Result.success(emptyList())
        val normalized = email.trim().lowercase()
        return try {
            val uid = requireUserId()
            val querySnap = db.collection("stores")
                .whereArrayContains("invitedEmails", normalized)
                .get()
                .await()
            val joined = mutableListOf<Store>()
            for (doc in querySnap.documents) {
                val store = doc.toObject(Store::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: continue
                if (uid !in store.memberUids && store.memberUids.size < 25) {
                    val updatedMembers = (store.memberUids + uid).distinct()
                    doc.reference.update(
                        mapOf(
                            "memberUids" to updatedMembers,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                joined.add(store)
            }
            Result.success(joined)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "stores")
            Result.failure(e)
        }
    }

    suspend fun createStoreWithOwner(
        storeName: String,
        ownerUsername: String,
        ownerDisplayName: String,
        ownerPassword: String
    ): Result<Store> {
        var path = "stores"
        return try {
            val uid = requireUserId()
            val cleanStoreName = storeName.trim().take(60)
            val cleanUsername = ownerUsername.trim().lowercase().take(30)
            val cleanDisplayName = ownerDisplayName.trim().take(50)
            require(cleanStoreName.length in 2..60) { "Le nom du magasin doit contenir entre 2 et 60 caractères." }
            require(cleanUsername.length in 2..30) { "L'identifiant doit contenir entre 2 et 30 caractères." }
            require(cleanDisplayName.length in 2..50) { "Le nom complet doit contenir entre 2 et 50 caractères." }
            require(ownerPassword.length >= 4) { "Le mot de passe doit contenir au moins 4 caractères." }

            val storeRef = db.collection("stores").document(uid)
            path = storeRef.path
            val staffRef = storeRef.collection("staff").document("owner_primary")

            val salt = PasswordHasher.generateSalt()
            val hash = PasswordHasher.hashPassword(salt, ownerPassword)

            val storePayload = mapOf(
                "ownerUid" to uid,
                "memberUids" to listOf(uid),
                "invitedEmails" to emptyList<String>(),
                "storeName" to cleanStoreName,
                "currency" to "FCFA",
                "defaultUnitsPerCrate" to 24L,
                "defaultMinThreshold" to 10L,
                "dataSaverEnabled" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val staffPayload = mapOf(
                "storeId" to uid,
                "ownerUid" to uid,
                "memberUids" to listOf(uid),
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "role" to UserRole.PROPRIETAIRE.name,
                "passwordSalt" to salt,
                "passwordHash" to hash,
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.runBatch { batch ->
                batch.set(storeRef, storePayload)
                batch.set(staffRef, staffPayload)
            }.await()

            val created = Store(
                id = uid,
                ownerUid = uid,
                memberUids = listOf(uid),
                storeName = cleanStoreName
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStoreSettings(
        storeId: String,
        actorRole: UserRole,
        storeName: String,
        defaultUnitsPerCrate: Long,
        defaultMinThreshold: Long,
        dataSaverEnabled: Boolean
    ): Result<Unit> {
        val path = "stores/$storeId"
        return try {
            requireUserId()
            check(RolePermissions.canEditSettings(actorRole)) {
                "Accès refusé : seul le propriétaire peut modifier les paramètres."
            }
            val cleanName = storeName.trim().take(60)
            require(cleanName.length in 2..60) { "Nom du magasin invalide (2 à 60 caractères)." }
            require(defaultUnitsPerCrate in 1L..200L) { "Unités par casier entre 1 et 200." }
            require(defaultMinThreshold in 0L..10000L) { "Seuil minimum entre 0 et 10000." }

            db.collection("stores").document(storeId).update(
                mapOf(
                    "storeName" to cleanName,
                    "defaultUnitsPerCrate" to defaultUnitsPerCrate,
                    "defaultMinThreshold" to defaultMinThreshold,
                    "dataSaverEnabled" to dataSaverEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeStaff(storeId: String, userId: String): Flow<List<StaffAccount>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "stores/$storeId/staff"
        emitAll(
            db.collection("stores").document(storeId).collection("staff")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StaffAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveStaffAccount(
        store: Store,
        actorRole: UserRole,
        existingStaffId: String?,
        username: String,
        displayName: String,
        role: UserRole,
        newPassword: String?,
        active: Boolean,
        linkedEmail: String?
    ): Result<Unit> {
        val docId = existingStaffId ?: UUID.randomUUID().toString()
        val path = "stores/${store.id}/staff/$docId"
        return try {
            val uid = requireUserId()
            check(RolePermissions.canManageUsers(actorRole)) {
                "Accès refusé : seul le propriétaire peut gérer les utilisateurs."
            }
            check(store.ownerUid == uid) {
                "Seul le propriétaire principal de la boutique peut créer ou modifier les comptes."
            }

            val cleanUsername = username.trim().lowercase().take(30)
            val cleanDisplay = displayName.trim().take(50)
            val cleanEmail = linkedEmail?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.take(120)

            require(cleanUsername.length in 2..30) { "L'identifiant doit contenir 2 à 30 caractères." }
            require(cleanDisplay.length in 2..50) { "Le nom complet doit contenir 2 à 50 caractères." }

            val staffRef = db.collection("stores").document(store.id).collection("staff").document(docId)
            if (existingStaffId == null) {
                val pwd = newPassword?.trim() ?: ""
                require(pwd.length >= 4) { "Le mot de passe doit contenir au moins 4 caractères." }
                val salt = PasswordHasher.generateSalt()
                val hash = PasswordHasher.hashPassword(salt, pwd)
                val payload = buildMap<String, Any> {
                    put("storeId", store.id)
                    put("ownerUid", store.ownerUid)
                    put("memberUids", store.memberUids)
                    put("username", cleanUsername)
                    put("displayName", cleanDisplay)
                    put("role", role.name)
                    put("passwordSalt", salt)
                    put("passwordHash", hash)
                    put("active", active)
                    if (cleanEmail != null) put("linkedEmail", cleanEmail)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }

                staffRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "role" to role.name,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!newPassword.isNullOrBlank()) {
                    require(newPassword.trim().length >= 4) { "Le mot de passe doit contenir au moins 4 caractères." }
                    val salt = PasswordHasher.generateSalt()
                    val hash = PasswordHasher.hashPassword(salt, newPassword.trim())
                    updates["passwordSalt"] = salt
                    updates["passwordHash"] = hash
                }
                if (cleanEmail != null) {
                    updates["linkedEmail"] = cleanEmail
                }
                staffRef.update(updates).await()
            }

            if (cleanEmail != null && cleanEmail !in store.invitedEmails && store.invitedEmails.size < 25) {
                db.collection("stores").document(store.id).update(
                    mapOf(
                        "invitedEmails" to (store.invitedEmails + cleanEmail).distinct(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStaffAccount(
        store: Store,
        actorRole: UserRole,
        staff: StaffAccount,
        allStaff: List<StaffAccount>
    ): Result<Unit> {
        val path = "stores/${store.id}/staff/${staff.id}"
        return try {
            val uid = requireUserId()
            check(RolePermissions.canManageUsers(actorRole) && store.ownerUid == uid) {
                "Accès refusé : suppression non autorisée."
            }
            if (staff.roleEnum() == UserRole.PROPRIETAIRE) {
                val activeOwners = allStaff.count { it.roleEnum() == UserRole.PROPRIETAIRE && it.active && it.id != staff.id }
                require(activeOwners >= 1) {
                    "Impossible de supprimer le dernier compte Propriétaire actif."
                }
            }
            db.collection("stores").document(store.id).collection("staff").document(staff.id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeProducts(storeId: String, userId: String): Flow<List<Product>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "stores/$storeId/products"
        emitAll(
            db.collection("stores").document(storeId).collection("products")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addProduct(
        store: Store,
        actor: StaffAccount,
        name: String,
        category: ProductCategory,
        purchasePrice: Long,
        sellingPrice: Long,
        initialQuantity: Long,
        minThreshold: Long,
        unitsPerCrate: Long
    ): Result<Unit> {
        val productId = UUID.randomUUID().toString()
        val path = "stores/${store.id}/products/$productId"
        return try {
            requireUserId()
            check(RolePermissions.canAddProduct(actor.roleEnum())) {
                "Accès refusé : vous n'êtes pas autorisé à ajouter un produit."
            }
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Le nom du produit est obligatoire." }
            require(purchasePrice in 0L..100_000_000L) { "Prix d'achat invalide." }
            require(sellingPrice in 0L..100_000_000L) { "Prix de vente invalide." }
            require(initialQuantity in 0L..10_000_000L) { "Quantité initiale invalide." }
            require(minThreshold in 0L..100_000L) { "Seuil minimum invalide." }
            require(unitsPerCrate in 1L..200L) { "Unités par casier entre 1 et 200." }

            val payload = mapOf(
                "storeId" to store.id,
                "ownerUid" to store.ownerUid,
                "memberUids" to store.memberUids,
                "name" to cleanName,
                "category" to category.name,
                "purchasePrice" to purchasePrice,
                "sellingPrice" to sellingPrice,
                "quantity" to initialQuantity,
                "minThreshold" to minThreshold,
                "unitsPerCrate" to unitsPerCrate,
                "createdByName" to actor.displayName.take(50).ifBlank { actor.username },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("stores").document(store.id).collection("products").document(productId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(
        store: Store,
        actor: StaffAccount,
        product: Product,
        name: String,
        category: ProductCategory,
        purchasePrice: Long,
        sellingPrice: Long,
        quantity: Long,
        minThreshold: Long,
        unitsPerCrate: Long
    ): Result<Unit> {
        val path = "stores/${store.id}/products/${product.id}"
        return try {
            requireUserId()
            check(RolePermissions.canEditProduct(actor.roleEnum())) {
                "Accès refusé : seul le propriétaire peut modifier un produit."
            }
            require(product.id.isNotBlank()) { "ID produit manquant." }
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Le nom du produit est obligatoire." }
            require(purchasePrice in 0L..100_000_000L) { "Prix d'achat invalide." }
            require(sellingPrice in 0L..100_000_000L) { "Prix de vente invalide." }
            require(quantity in 0L..10_000_000L) { "Quantité invalide." }
            require(minThreshold in 0L..100_000L) { "Seuil minimum invalide." }
            require(unitsPerCrate in 1L..200L) { "Unités par casier entre 1 et 200." }

            db.collection("stores").document(store.id).collection("products").document(product.id)
                .update(
                    mapOf(
                        "name" to cleanName,
                        "category" to category.name,
                        "purchasePrice" to purchasePrice,
                        "sellingPrice" to sellingPrice,
                        "quantity" to quantity,
                        "minThreshold" to minThreshold,
                        "unitsPerCrate" to unitsPerCrate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(
        store: Store,
        actor: StaffAccount,
        productId: String
    ): Result<Unit> {
        val path = "stores/${store.id}/products/$productId"
        return try {
            requireUserId()
            check(RolePermissions.canDeleteProduct(actor.roleEnum())) {
                "Accès refusé : seul le propriétaire peut supprimer un produit."
            }
            require(productId.isNotBlank())
            db.collection("stores").document(store.id).collection("products").document(productId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordStockEntry(
        store: Store,
        actor: StaffAccount,
        productId: String,
        cratesCount: Long,
        extraBottles: Long,
        unitsPerCrate: Long,
        purchasePrice: Long,
        receptionDateMs: Long,
        note: String?
    ): Result<Long> {
        val movementId = UUID.randomUUID().toString()
        val path = "stores/${store.id}/products/$productId"
        return try {
            requireUserId()
            check(RolePermissions.canAccessStockEntry(actor.roleEnum())) {
                "Accès refusé : vous n'êtes pas autorisé à enregistrer des entrées de stock."
            }
            require(cratesCount >= 0L && extraBottles >= 0L) { "Quantités invalides." }
            require(unitsPerCrate in 1L..200L) { "Unités par casier entre 1 et 200." }
            require(purchasePrice in 0L..100_000_000L) { "Prix d'achat invalide." }
            val totalAdded = (cratesCount * unitsPerCrate) + extraBottles
            require(totalAdded > 0L) { "La quantité totale reçue doit être supérieure à zéro." }

            val storeRef = db.collection("stores").document(store.id)
            val productRef = storeRef.collection("products").document(productId)
            val movementRef = storeRef.collection("movements").document(movementId)

            val newStock = db.runTransaction { tx ->
                val productSnap = tx.get(productRef)
                val currentProduct = productSnap.toObject(Product::class.java)
                    ?: throw IllegalStateException("Produit introuvable.")
                val stockBefore = currentProduct.quantity.coerceAtLeast(0L)
                val stockAfter = stockBefore + totalAdded

                tx.update(
                    productRef,
                    mapOf(
                        "quantity" to stockAfter,
                        "purchasePrice" to purchasePrice,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(200)
                val movementPayload = buildMap<String, Any> {
                    put("storeId", store.id)
                    put("ownerUid", store.ownerUid)
                    put("memberUids", store.memberUids)
                    put("type", MovementType.ENTREE.name)
                    put("productId", productId)
                    put("productName", currentProduct.name)
                    put("cratesCount", cratesCount)
                    put("extraBottles", extraBottles)
                    put("unitsPerCrate", unitsPerCrate)
                    put("quantity", totalAdded)
                    put("stockBefore", stockBefore)
                    put("stockAfter", stockAfter)
                    put("unitPrice", purchasePrice)
                    put("totalAmount", totalAdded * purchasePrice)
                    put("operatorId", actor.id)
                    put("operatorName", actor.displayName.take(50).ifBlank { actor.username })
                    put("operatorRole", actor.roleEnum().name)
                    put("referenceId", movementId)
                    put("operationDateMs", receptionDateMs)
                    if (cleanNote != null) put("note", cleanNote)
                    put("createdAt", FieldValue.serverTimestamp())
                }

                tx.set(movementRef, movementPayload)
                stockAfter
            }.await()

            Result.success(newStock)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordSale(
        store: Store,
        actor: StaffAccount,
        saleId: String,
        cartItems: List<CartItem>
    ): Result<SaleRecord> {
        val path = "stores/${store.id}/sales/$saleId"
        return try {
            requireUserId()
            check(RolePermissions.canMakeSale(actor.roleEnum())) {
                "Accès refusé : profil non autorisé."
            }
            require(saleId.isNotBlank()) { "Identifiant de vente invalide." }
            require(cartItems.isNotEmpty() && cartItems.size <= 50) {
                "Le panier doit contenir entre 1 et 50 produits."
            }
            cartItems.forEach { item ->
                require(item.quantity > 0) { "Quantité invalide pour ${item.product.name}." }
            }

            val storeRef = db.collection("stores").document(store.id)
            val saleRef = storeRef.collection("sales").document(saleId)
            val nowMs = System.currentTimeMillis()

            val recordedSale = db.runTransaction { tx ->
                // 1. All reads FIRST (Firestore transaction requirement)
                val existingSale = tx.get(saleRef)
                if (existingSale.exists()) {
                    throw IllegalStateException("Cette vente a déjà été enregistrée.")
                }

                val productSnapshots = cartItems.map { item ->
                    val ref = storeRef.collection("products").document(item.product.id)
                    item to tx.get(ref)
                }

                val verifiedLines = mutableListOf<Triple<CartItem, Product, Long>>()
                for ((item, snap) in productSnapshots) {
                    val liveProduct = snap.toObject(Product::class.java)
                        ?: throw IllegalStateException("Produit introuvable : ${item.product.name}")
                    if (liveProduct.quantity <= 0L) {
                        throw IllegalStateException("${liveProduct.name} est en rupture de stock.")
                    }
                    if (item.quantity.toLong() > liveProduct.quantity) {
                        throw IllegalStateException(
                            "Stock insuffisant pour ${liveProduct.name} (${liveProduct.quantity} disponible(s))."
                        )
                    }
                    verifiedLines.add(Triple(item, liveProduct, liveProduct.quantity - item.quantity.toLong()))
                }

                val saleLines = verifiedLines.map { (item, liveProduct, _) ->
                    val qty = item.quantity.toLong()
                    SaleLine(
                        productId = liveProduct.id,
                        productName = liveProduct.name,
                        quantity = qty,
                        unitPrice = liveProduct.sellingPrice,
                        lineTotal = qty * liveProduct.sellingPrice
                    )
                }

                val totalAmount = saleLines.sumOf { it.lineTotal }
                val totalUnits = saleLines.sumOf { it.quantity }
                val summary = Formatters.buildSaleItemsSummary(saleLines)
                val operatorDisplay = actor.displayName.take(50).ifBlank { actor.username }

                // 2. All writes AFTER reads
                for ((item, liveProduct, stockAfter) in verifiedLines) {
                    val productRef = storeRef.collection("products").document(liveProduct.id)
                    tx.update(
                        productRef,
                        mapOf(
                            "quantity" to stockAfter,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )

                    val movId = "${saleId}_${liveProduct.id}".take(120)
                    val movRef = storeRef.collection("movements").document(movId)
                    val qty = item.quantity.toLong()
                    tx.set(
                        movRef,
                        mapOf(
                            "storeId" to store.id,
                            "ownerUid" to store.ownerUid,
                            "memberUids" to store.memberUids,
                            "type" to MovementType.SORTIE.name,
                            "productId" to liveProduct.id,
                            "productName" to liveProduct.name,
                            "cratesCount" to 0L,
                            "extraBottles" to qty,
                            "unitsPerCrate" to liveProduct.unitsPerCrate.coerceAtLeast(1L),
                            "quantity" to qty,
                            "stockBefore" to liveProduct.quantity,
                            "stockAfter" to stockAfter,
                            "unitPrice" to liveProduct.sellingPrice,
                            "totalAmount" to (qty * liveProduct.sellingPrice),
                            "operatorId" to actor.id,
                            "operatorName" to operatorDisplay,
                            "operatorRole" to actor.roleEnum().name,
                            "referenceId" to saleId,
                            "operationDateMs" to nowMs,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val linesPayload = saleLines.map { line ->
                    mapOf(
                        "productId" to line.productId,
                        "productName" to line.productName,
                        "quantity" to line.quantity,
                        "unitPrice" to line.unitPrice,
                        "lineTotal" to line.lineTotal
                    )
                }

                tx.set(
                    saleRef,
                    mapOf(
                        "storeId" to store.id,
                        "ownerUid" to store.ownerUid,
                        "memberUids" to store.memberUids,
                        "operatorId" to actor.id,
                        "operatorName" to operatorDisplay,
                        "operatorRole" to actor.roleEnum().name,
                        "totalAmount" to totalAmount,
                        "totalUnits" to totalUnits,
                        "itemCount" to saleLines.size.toLong(),
                        "itemsSummary" to summary,
                        "lines" to linesPayload,
                        "saleDateMs" to nowMs,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                SaleRecord(
                    id = saleId,
                    storeId = store.id,
                    ownerUid = store.ownerUid,
                    memberUids = store.memberUids,
                    operatorId = actor.id,
                    operatorName = operatorDisplay,
                    operatorRole = actor.roleEnum().name,
                    totalAmount = totalAmount,
                    totalUnits = totalUnits,
                    itemCount = saleLines.size.toLong(),
                    itemsSummary = summary,
                    lines = saleLines,
                    saleDateMs = nowMs
                )
            }.await()

            Result.success(recordedSale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeRecentMovements(storeId: String, userId: String, maxItems: Long = 120L): Flow<List<StockMovement>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "stores/$storeId/movements"
        emitAll(
            db.collection("stores").document(storeId).collection("movements")
                .whereArrayContains("memberUids", uid)
                .limit(maxItems)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StockMovement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.operationDateMs }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeRecentSales(storeId: String, userId: String, maxItems: Long = 120L): Flow<List<SaleRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "stores/$storeId/sales"
        emitAll(
            db.collection("stores").document(storeId).collection("sales")
                .whereArrayContains("memberUids", uid)
                .limit(maxItems)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SaleRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.saleDateMs }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfSignedIn(
        error: Throwable,
        opType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, opType, path)
        }
    }

    companion object {
        fun createConfiguredFirestore(context: Context): FirebaseFirestore {
            val dbId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            try {
                // Enable persistent local cache (32 MB) to minimize internet data usage
                val settings = firestoreSettings {
                    setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(32L * 1024L * 1024L)
                            .build()
                    )
                }
                db.firestoreSettings = settings
            } catch (_: IllegalStateException) {
                // Firestore instance already configured in this process; safe to continue.
            }
            return db
        }
    }
}
