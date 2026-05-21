package com.shopapp.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shopapp.R
import com.shopapp.data.model.Product
import com.shopapp.databinding.ItemAdminProductBinding

class AdminProductAdapter(
    private val onEdit:    (Product) -> Unit,
    private val onDelete:  (Product) -> Unit,
    private val onPopular: (Product, Boolean) -> Unit   // ← NEW callback
) : ListAdapter<Product, AdminProductAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val b: ItemAdminProductBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(product: Product) {
            b.tvName.text     = product.name
            b.tvPrice.text    = "$${String.format("%.2f", product.price)}"
            b.tvCategory.text = product.category
            b.tvStock.text    = "Stock: ${product.stock}"

            Glide.with(b.ivProduct)
                .load(
                    if (product.imageUrl.startsWith("/")) java.io.File(product.imageUrl)
                    else product.imageUrl
                )
                .placeholder(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(b.ivProduct)

            // ── Popular state ─────────────────────────────────────────────────
            updatePopularUI(product.popular)

            b.btnEdit.setOnClickListener   { onEdit(product) }
            b.btnDelete.setOnClickListener { onDelete(product) }

            b.btnPopular.setOnClickListener {
                val newState = !product.popular
                onPopular(product, newState)
            }
        }

        /** Keeps the star button and badge in sync with the popular flag. */
        private fun updatePopularUI(isPopular: Boolean) {
            b.btnPopular.text          = if (isPopular) "★" else "☆"
            b.tvPopularBadge.visibility = if (isPopular) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemAdminProductBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
        override fun areContentsTheSame(a: Product, b: Product) = a == b
    }
}