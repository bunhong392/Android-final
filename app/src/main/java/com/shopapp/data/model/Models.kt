package com.shopapp.data.model


data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "user",       // "user" | "admin" | "staff"
    val employeeCode: String = "",
    val store: String = "",
    val createdAt: Long = 0
)

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val category: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val popular: Boolean = false      // field name "popular" matches Firestore document
)

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    var quantity: Int = 1
) {
    val totalPrice: Double get() = price * quantity
}

data class Order(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val deliveryAddress: String = "",
    val paymentMethod: String = "",
    val status: String = "pending",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Admin(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "admin",
    val store: String = "",
    val employeeCode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)