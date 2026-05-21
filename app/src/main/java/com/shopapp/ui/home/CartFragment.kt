package com.shopapp.ui.home

import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shopapp.data.model.CartItem
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentCartBinding
import com.shopapp.ui.payment.PaymentActivity
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val adapter = CartAdapter(
        onQuantityChange = { item, qty -> updateQuantity(item, qty) },
        onDelete         = { item -> removeItem(item) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCart.adapter = adapter

        binding.btnCheckout.setOnClickListener {
            if (adapter.currentList.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            proceedToCheckout()
        }

        loadCart()
    }

    override fun onResume() {
        super.onResume()
        loadCart()
    }

    // ── Load cart + stock ─────────────────────────────────────────────────────

    private fun loadCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cartResult = Repository.getCart()
            cartResult.onSuccess { items ->
                adapter.submitList(items)
                updateTotal(items)
                binding.tvEmpty.visibility =
                    if (items.isEmpty()) View.VISIBLE else View.GONE
                binding.btnCheckout.isEnabled = items.isNotEmpty()

                // Fetch live stock for every unique product in the cart
                if (items.isNotEmpty()) {
                    loadStockForItems(items)
                }
            }
        }
    }

    /**
     * For each distinct product in the cart, fetch its current stock from Firebase
     * and push the map to the adapter so it can enable/disable the + button and
     * show warnings.
     */
    private fun loadStockForItems(items: List<CartItem>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val stockMap = mutableMapOf<String, Int>()
            val productIds = items.map { it.productId }.distinct()

            for (productId in productIds) {
                Repository.getProduct(productId).onSuccess { product ->
                    stockMap[productId] = product.stock
                }
            }

            adapter.updateStockMap(stockMap)

            // If any item quantity exceeds stock, disable checkout and warn user
            val hasStockIssue = items.any { item ->
                val stock = stockMap[item.productId] ?: Int.MAX_VALUE
                item.quantity > stock || stock == 0
            }

            if (hasStockIssue) {
                binding.btnCheckout.isEnabled = false
                binding.btnCheckout.text = "Fix stock issues to checkout"
                Toast.makeText(
                    requireContext(),
                    "Some items exceed available stock. Please adjust quantities.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                binding.btnCheckout.isEnabled = true
                binding.btnCheckout.text = "Proceed to Checkout"
            }
        }
    }

    private fun updateTotal(items: List<CartItem>) {
        val total = items.sumOf { it.totalPrice }
        binding.tvTotal.text = "Total: $${String.format("%.2f", total)}"
    }

    // ── Quantity change with stock enforcement ────────────────────────────────

    private fun updateQuantity(item: CartItem, quantity: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Repository.updateCartQuantity(item.id, quantity)
            result.onSuccess {
                loadCart()
            }.onFailure { e ->
                // Repository already checks stock; show the error message
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Cannot increase quantity",
                    Toast.LENGTH_LONG
                ).show()
                loadCart() // Refresh to show correct state
            }
        }
    }

    private fun removeItem(item: CartItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            Repository.updateCartQuantity(item.id, 0)
            loadCart()
        }
    }

    // ── Checkout guard ────────────────────────────────────────────────────────

    /**
     * Final stock check before going to PaymentActivity.
     * Re-validates against live Firebase stock so no stale data causes overselling.
     */
    private fun proceedToCheckout() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = Repository.getCart().getOrNull() ?: emptyList()
            if (items.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@launch
            }

            var allGood = true
            for (item in items) {
                val product = Repository.getProduct(item.productId).getOrNull()
                if (product == null || product.stock < item.quantity) {
                    allGood = false
                    val available = product?.stock ?: 0
                    Toast.makeText(
                        requireContext(),
                        "Only $available ${item.productName}(s) available. Please adjust your cart.",
                        Toast.LENGTH_LONG
                    ).show()
                    loadCart()
                    break
                }
            }

            if (allGood) {
                startActivity(Intent(requireContext(), PaymentActivity::class.java))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
