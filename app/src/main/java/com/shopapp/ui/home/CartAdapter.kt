package com.shopapp.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopapp.R
import com.shopapp.data.model.CartItem
import com.shopapp.databinding.ItemCartBinding

/**
 * @param onQuantityChange called with the new quantity (0 = remove)
 * @param onDelete         called when the delete button is tapped
 * @param stockMap         maps productId → available stock (refreshed from Repository)
 */
class CartAdapter(
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onDelete: (CartItem) -> Unit,
    private var stockMap: Map<String, Int> = emptyMap()
) : ListAdapter<CartItem, CartAdapter.ViewHolder>(DiffCallback()) {

    /** Call this when fresh stock data is available so the adapter can re-render. */
    fun updateStockMap(map: Map<String, Int>) {
        stockMap = map
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val b: ItemCartBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: CartItem) {
            b.tvName.text     = item.productName
            b.tvPrice.text    = "$${String.format("%.2f", item.price)}"
            b.tvQuantity.text = item.quantity.toString()
            b.tvSubtotal.text = "Subtotal: $${String.format("%.2f", item.totalPrice)}"

            Glide.with(b.ivProduct)
                .load(item.productImage)
                .placeholder(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(b.ivProduct)

            // ── Stock-aware increase button ──────────────────────────────────
            val availableStock = stockMap[item.productId] ?: Int.MAX_VALUE
            val atStockLimit   = item.quantity >= availableStock

            b.btnIncrease.isEnabled = !atStockLimit
            b.btnIncrease.alpha     = if (atStockLimit) 0.4f else 1f

            // Show a subtle stock warning if the user is at the limit
            if (atStockLimit && availableStock > 0) {
                b.tvSubtotal.text = "Max qty: only $availableStock in stock"
                b.tvSubtotal.setTextColor(Color.parseColor("#E65100")) // deep orange
            } else if (availableStock == 0) {
                b.tvSubtotal.text = "⚠ Out of stock — please remove"
                b.tvSubtotal.setTextColor(Color.RED)
                b.btnIncrease.isEnabled = false
                b.btnDecrease.isEnabled = false
            } else {
                b.tvSubtotal.setTextColor(Color.GRAY)
            }

            b.btnIncrease.setOnClickListener {
                if (item.quantity < availableStock) {
                    onQuantityChange(item, item.quantity + 1)
                }
            }
            b.btnDecrease.setOnClickListener {
                if (item.quantity > 1) onQuantityChange(item, item.quantity - 1)
            }
            b.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(old: CartItem, new: CartItem) = old.id == new.id
        override fun areContentsTheSame(old: CartItem, new: CartItem) = old == new
    }
}
