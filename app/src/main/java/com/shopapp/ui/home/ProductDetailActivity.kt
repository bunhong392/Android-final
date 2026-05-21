package com.shopapp.ui.home

import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.shopapp.R
import com.shopapp.data.model.CartItem
import com.shopapp.data.model.Product
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.ActivityProductDetailBinding
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private var product: Product? = null

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.shopapp.util.LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productId = intent.getStringExtra("product_id") ?: return finish()

        binding.btnBack.setOnClickListener { finish() }

        loadProduct(productId)

        binding.btnAddToCart.setOnClickListener {
            addToCart()
        }
    }

    /** Re-fetch fresh stock every time the screen becomes visible (e.g. after back-press). */
    override fun onResume() {
        super.onResume()
        product?.let { loadProduct(it.id) }
    }

    private fun loadProduct(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = Repository.getProduct(id)
            binding.progressBar.visibility = View.GONE
            result.onSuccess { p ->
                product = p
                displayProduct(p)
            }.onFailure {
                Toast.makeText(
                    this@ProductDetailActivity,
                    "Failed to load product", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun displayProduct(p: Product) {
        binding.tvName.text        = p.name
        binding.tvPrice.text       = "$${String.format("%.2f", p.price)}"
        binding.tvDescription.text = p.description
        binding.tvCategory.text    = p.category

        // ── Stock label ──────────────────────────────────────────────────────
        when {
            p.stock <= 0 -> {
                binding.tvStock.text = "❌ Out of Stock"
                binding.tvStock.setTextColor(getColor(android.R.color.holo_red_dark))
                binding.btnAddToCart.isEnabled = false
                binding.btnAddToCart.alpha = 0.5f
                binding.btnAddToCart.text = "Out of Stock"
            }
            p.stock <= 5 -> {
                binding.tvStock.text = "⚠️ Only ${p.stock} left in stock!"
                binding.tvStock.setTextColor(getColor(android.R.color.holo_orange_dark))
                binding.btnAddToCart.isEnabled = true
                binding.btnAddToCart.alpha = 1f
                binding.btnAddToCart.text = "Add to Cart"
            }
            else -> {
                binding.tvStock.text = "✅ In Stock: ${p.stock}"
                binding.tvStock.setTextColor(getColor(android.R.color.holo_green_dark))
                binding.btnAddToCart.isEnabled = true
                binding.btnAddToCart.alpha = 1f
                binding.btnAddToCart.text = "Add to Cart"
            }
        }

        Glide.with(this)
            .load(p.imageUrl)
            .placeholder(R.drawable.ic_product_placeholder)
            .into(binding.ivProduct)
    }

    private fun addToCart() {
        val p = product ?: return

        if (p.stock <= 0) {
            Toast.makeText(this, "Sorry, this item is out of stock", Toast.LENGTH_SHORT).show()
            return
        }

        val cartItem = CartItem(
            productId    = p.id,
            productName  = p.name,
            productImage = p.imageUrl,
            price        = p.price,
            quantity     = 1
        )

        binding.btnAddToCart.isEnabled = false

        lifecycleScope.launch {
            val result = Repository.addToCart(cartItem)
            result.onSuccess {
                Toast.makeText(
                    this@ProductDetailActivity,
                    "✓ ${p.name} added to cart!", Toast.LENGTH_SHORT
                ).show()
                binding.btnAddToCart.text = "✓ Added to Cart"
                // Reload to reflect potentially updated stock view
                loadProduct(p.id)
            }.onFailure { e ->
                binding.btnAddToCart.isEnabled = true
                // Show the exact stock error (e.g. "Only 2 Apples available in stock")
                Toast.makeText(
                    this@ProductDetailActivity,
                    e.message ?: "Failed to add to cart", Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
