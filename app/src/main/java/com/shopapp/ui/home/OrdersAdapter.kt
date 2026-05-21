package com.shopapp.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shopapp.data.model.Order
import com.shopapp.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.*

class OrdersAdapter : ListAdapter<Order, OrdersAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val b: ItemOrderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(order: Order) {
            b.tvOrderId.text  = "Order #${order.id.take(8).uppercase()}"
            b.tvItems.text    = "${order.items.size} item(s)"
            b.tvTotal.text    = "$${String.format("%.2f", order.totalAmount)}"
            b.tvPayment.text  = order.paymentMethod
            b.tvAddress.text  = order.deliveryAddress

            val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(Date(order.createdAt))
            b.tvDate.text = date

            b.tvStatus.text = order.status.uppercase()
            b.tvStatus.setTextColor(
                when (order.status) {
                    "pending"    -> Color.parseColor("#FF9800")
                    "confirmed"  -> Color.parseColor("#2196F3")
                    "delivering" -> Color.parseColor("#9C27B0")
                    "delivered"  -> Color.parseColor("#4CAF50")
                    else         -> Color.GRAY
                }
            )

            // ── Tap card → show receipt bottom sheet ──────────────────────
            b.root.setOnClickListener {
                val ctx = b.root.context
                val fragment = b.root.context
                // Find the FragmentManager via the context (Activity)
                val activity = ctx as? androidx.fragment.app.FragmentActivity ?: return@setOnClickListener
                OrderReceiptBottomSheet.newInstance(order)
                    .show(activity.supportFragmentManager, "receipt")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order) = a.id == b.id
        override fun areContentsTheSame(a: Order, b: Order) = a == b
    }
}