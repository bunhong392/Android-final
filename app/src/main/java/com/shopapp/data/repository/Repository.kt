package com.shopapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.getValue
import com.shopapp.data.model.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object Repository {

    // ── Firebase instances ────────────────────────────────────────────────────
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance().apply { setPersistenceEnabled(true) }
    }

    // ── SharedPreferences ─────────────────────────────────────────────────────
    private lateinit var prefs: SharedPreferences
    private const val PREF_NAME = "shopapp_session"
    private const val KEY_ROLE  = "user_role"

    // ── Database paths ────────────────────────────────────────────────────────
    private fun usersRef()    = db.getReference("users")
    private fun productsRef() = db.getReference("products")
    private fun ordersRef()   = db.getReference("orders")
    private fun cartRef(uid: String) = db.getReference("cart").child(uid)
    private fun countersRef() = db.getReference("counters")

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // ── Session helpers ───────────────────────────────────────────────────────
    val currentUserId: String? get() = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun logout() {
        auth.signOut()
        prefs.edit().remove(KEY_ROLE).apply()
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun loginUser(email: String, password: String): Result<User> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()

        val uid  = auth.currentUser!!.uid
        val snap = usersRef().child(uid).get().await()

        val user = snap.getValue<User>()?.copy(uid = uid)
            ?: throw Exception("User profile not found in database")

        prefs.edit().putString(KEY_ROLE, user.role).apply()
        user
    }

    suspend fun registerUser(
        name: String, email: String, password: String,
        phone: String, role: String = "user"
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid    = result.user!!.uid

        val user = User(
            uid       = uid,
            name      = name,
            email     = email,
            phone     = phone,
            role      = role,
            createdAt = System.currentTimeMillis()
        )
        usersRef().child(uid).setValue(user).await()

        prefs.edit().putString(KEY_ROLE, role).apply()
        user
    }

    suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return try {
            val snap = usersRef().child(uid).get().await()
            snap.getValue<User>()?.copy(uid = uid)
        } catch (e: Exception) { null }
    }

    suspend fun getCurrentUserResult(): Result<User?> = runCatching { getCurrentUser() }

    // ── Password Reset ────────────────────────────────────────────────────────

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    // ── Products ──────────────────────────────────────────────────────────────

    suspend fun getProducts(category: String = ""): Result<List<Product>> = runCatching {
        val snap = productsRef().get().await()
        val all = snap.children.mapNotNull { child ->
            child.getValue<Product>()?.copy(id = child.key ?: "")
        }.sortedByDescending { it.createdAt }

        if (category.isEmpty()) all
        else all.filter { it.category == category }
    }

    suspend fun getProduct(id: String): Result<Product> = runCatching {
        val snap = productsRef().child(id).get().await()
        snap.getValue<Product>()?.copy(id = id)
            ?: throw Exception("Product not found")
    }

    suspend fun addProduct(product: Product): Result<String> = runCatching {
        val productId = generateProductId()
        productsRef().child(productId)
            .setValue(product.copy(id = productId, createdAt = System.currentTimeMillis()))
            .await()
        productId
    }

    suspend fun updateProduct(product: Product): Result<Unit> = runCatching {
        productsRef().child(product.id).setValue(product).await()
    }

    suspend fun deleteProduct(id: String): Result<Unit> = runCatching {
        productsRef().child(id).removeValue().await()
    }

    // ── Popular Products ──────────────────────────────────────────────────────

    suspend fun getPopularProducts(): Result<List<Product>> = runCatching {
        val snap = productsRef().get().await()
        val all = snap.children.mapNotNull { child ->
            child.getValue<Product>()?.copy(id = child.key ?: "")
        }
        val popular = all.filter { it.popular }.sortedByDescending { it.createdAt }
        if (popular.isNotEmpty()) popular else all.sortedByDescending { it.createdAt }.take(6)
    }

    suspend fun setProductPopular(productId: String, popular: Boolean): Result<Unit> = runCatching {
        productsRef().child(productId).child("popular").setValue(popular).await()
    }

    // ── Cart ───────────────────────────────────────

    suspend fun getCart(): Result<List<CartItem>> = runCatching {
        val uid  = currentUserId ?: throw Exception("Not logged in")
        val snap = cartRef(uid).get().await()
        snap.children.mapNotNull { child ->
            child.getValue<CartItem>()?.copy(id = child.key ?: "")
        }
    }

    suspend fun addToCart(item: CartItem): Result<Unit> = runCatching {
        val uid  = currentUserId ?: throw Exception("Not logged in")

        // 1. Read current stock from Firebase
        val productSnap = productsRef().child(item.productId).get().await()
        val currentStock = productSnap.child("stock").getValue<Int>() ?: 0

        if (currentStock <= 0) {
            throw Exception("${item.productName} is out of stock")
        }

        // 2. Read cart to find existing qty for this product
        val cartSnap = cartRef(uid).get().await()
        val existingEntry = cartSnap.children.firstOrNull { child ->
            child.getValue<CartItem>()?.productId == item.productId
        }
        val cartQty = existingEntry?.getValue<CartItem>()?.quantity ?: 0

        // 3. Enforce stock cap
        if (cartQty >= currentStock) {
            throw Exception("Only $currentStock ${item.productName}(s) available in stock")
        }

        // 4. Update cart
        if (existingEntry != null) {
            cartRef(uid).child(existingEntry.key!!).child("quantity")
                .setValue(cartQty + 1).await()
        } else {
            val ref = cartRef(uid).push()
            val id  = ref.key ?: throw Exception("Failed to generate cart ID")
            ref.setValue(item.copy(id = id, quantity = 1)).await()
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, quantity: Int): Result<Unit> = runCatching {
        val uid = currentUserId ?: throw Exception("Not logged in")

        if (quantity <= 0) {
            cartRef(uid).child(cartItemId).removeValue().await()
            return@runCatching
        }

        // Find the cart item to get the productId
        val cartItemSnap = cartRef(uid).child(cartItemId).get().await()
        val cartItem = cartItemSnap.getValue<CartItem>()

        if (cartItem != null) {
            // Read current stock
            val productSnap = productsRef().child(cartItem.productId).get().await()
            val currentStock = productSnap.child("stock").getValue<Int>() ?: 0

            if (quantity > currentStock) {
                throw Exception("Only $currentStock ${cartItem.productName}(s) available in stock")
            }
        }

        cartRef(uid).child(cartItemId).child("quantity").setValue(quantity).await()
    }

    suspend fun clearCart(): Result<Unit> = runCatching {
        val uid = currentUserId ?: throw Exception("Not logged in")
        cartRef(uid).removeValue().await()
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    suspend fun placeOrder(order: Order): Result<String> = runCatching {

        // Step 1: Atomically deduct stock for each item
        for (item in order.items) {
            val stockRef = productsRef().child(item.productId).child("stock")
            val deductResult = atomicDeductStock(stockRef, item.quantity, item.productName)
            if (deductResult.isFailure) {
                throw deductResult.exceptionOrNull()
                    ?: Exception("Stock deduction failed for ${item.productName}")
            }
        }

        // Step 2: Generate sequential order ID like "ORD-0001"
        val orderId = generateOrderId()

        // Step 3: Save the order with the readable ID as the key
        ordersRef().child(orderId)
            .setValue(order.copy(id = orderId, createdAt = System.currentTimeMillis()))
            .await()

        // Step 4: Clear cart
        clearCart()

        orderId
    }

    // ── ID Generators ─────────────────────────────────────────────────────────
    private suspend fun generateProductId(): String = suspendCancellableCoroutine { cont ->
        val counterRef = countersRef().child("productCount")

        counterRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                currentData.value = current + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: com.google.firebase.database.DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (!cont.isActive) return
                when {
                    error != null -> cont.resume("PRD-${System.currentTimeMillis()}")
                    !committed    -> cont.resume("PRD-${System.currentTimeMillis()}")
                    else -> {
                        val count = snapshot?.getValue(Int::class.java) ?: 1
                        // Format: PRD-0001, PRD-0002, PRD-0100 ...
                        val formatted = "PRD-${count.toString().padStart(4, '0')}"
                        cont.resume(formatted)
                    }
                }
            }
        })
    }

    /**
     * Atomically increments the order counter and returns a formatted ID like "ORD-0001"
     */
    private suspend fun generateOrderId(): String = suspendCancellableCoroutine { cont ->
        val counterRef = countersRef().child("orderCount")

        counterRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                currentData.value = current + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: com.google.firebase.database.DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (!cont.isActive) return
                when {
                    error != null -> cont.resume("ORD-${System.currentTimeMillis()}")
                    !committed    -> cont.resume("ORD-${System.currentTimeMillis()}")
                    else -> {
                        val count = snapshot?.getValue(Int::class.java) ?: 1
                        // Format: ORD-0001, ORD-0002, ORD-0100 ...
                        val formatted = "ORD-${count.toString().padStart(4, '0')}"
                        cont.resume(formatted)
                    }
                }
            }
        })
    }

    private suspend fun atomicDeductStock(
        stockRef: com.google.firebase.database.DatabaseReference,
        qty: Int,
        productName: String
    ): Result<Unit> = suspendCancellableCoroutine { cont ->

        stockRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                if (current < qty) {
                    return Transaction.abort()
                }
                currentData.value = current - qty
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: com.google.firebase.database.DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (!cont.isActive) return
                when {
                    error != null -> cont.resume(
                        Result.failure(Exception("Stock update failed: ${error.message}"))
                    )
                    !committed -> cont.resume(
                        Result.failure(Exception("Not enough stock for $productName"))
                    )
                    else -> cont.resume(Result.success(Unit))
                }
            }
        })
    }

    suspend fun getMyOrders(): Result<List<Order>> = runCatching {
        val uid  = currentUserId ?: throw Exception("Not logged in")
        val snap = ordersRef().get().await()
        snap.children.mapNotNull { child ->
            child.getValue<Order>()?.copy(id = child.key ?: "")
        }.filter { it.userId == uid }.sortedByDescending { it.createdAt }
    }

    suspend fun getAllOrders(): Result<List<Order>> = runCatching {
        val snap = ordersRef().get().await()
        snap.children.mapNotNull { child ->
            child.getValue<Order>()?.copy(id = child.key ?: "")
        }.sortedByDescending { it.createdAt }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> = runCatching {
        ordersRef().child(orderId).child("status").setValue(status).await()
    }

    // ── Admin: Clear Data ─────────────────────────────────────────────────────

    suspend fun clearProductsOnly(): Result<Unit> = runCatching {
        val ref = productsRef()
        ref.removeValue().await()
        val snap = ref.get().await()
        if (snap.exists()) throw Exception("Products still exist — check your internet connection and try again")

        // Reset product counter back to 0
        countersRef().child("productCount").setValue(0).await()
    }

    suspend fun clearOrdersOnly(): Result<Unit> = runCatching {
        val ref = ordersRef()
        ref.removeValue().await()
        val snap = ref.get().await()
        if (snap.exists()) throw Exception("Orders still exist — check your internet connection and try again")

        // Reset order counter back to 0
        countersRef().child("orderCount").setValue(0).await()
    }

    suspend fun clearUsersOnly(): Result<Unit> = runCatching {
        usersRef().removeValue().await()
        auth.signOut()
        prefs.edit().remove(KEY_ROLE).apply()
    }

    suspend fun clearAllData(): Result<Unit> = runCatching {
        clearProductsOnly()
        clearOrdersOnly()
        clearUsersOnly()
        auth.signOut()
        prefs.edit().remove(KEY_ROLE).apply()
    }
}