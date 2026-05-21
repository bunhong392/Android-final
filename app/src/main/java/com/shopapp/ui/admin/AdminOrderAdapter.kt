package com.shopapp.ui.admin

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shopapp.data.model.Order
import com.shopapp.databinding.ItemAdminOrderBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminOrderAdapter(
    private val onStatusChange: (Order, String) -> Unit
) : ListAdapter<Order, AdminOrderAdapter.ViewHolder>(DiffCallback()) {

    private val statuses = listOf("pending", "confirmed", "delivering", "delivered")

    inner class ViewHolder(private val b: ItemAdminOrderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(order: Order) {
            b.tvOrderId.text = "#${order.id.take(8).uppercase()}"
            b.tvCustomer.text = "Customer: ${order.userName}"
            b.tvPhone.text = "Phone: ${order.userPhone}"
            b.tvAddress.text = "Address: ${order.deliveryAddress}"
            b.tvTotal.text = "Total: $${String.format("%.2f", order.totalAmount)}"
            b.tvPayment.text = "Payment: ${order.paymentMethod}"
            b.tvItems.text = "${order.items.size} item(s)"

            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(order.createdAt))
            b.tvDate.text = date

            // Status color
            b.tvStatus.text = order.status.uppercase()
            b.tvStatus.setTextColor(when (order.status) {
                "pending" -> Color.parseColor("#FF9800")
                "confirmed" -> Color.parseColor("#2196F3")
                "delivering" -> Color.parseColor("#9C27B0")
                "delivered" -> Color.parseColor("#4CAF50")
                else -> Color.GRAY
            })

            // Status spinner
            val adapter = ArrayAdapter(b.root.context,
                android.R.layout.simple_spinner_item, statuses)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            b.spinnerStatus.adapter = adapter
            b.spinnerStatus.setSelection(statuses.indexOf(order.status))

            b.btnUpdateStatus.setOnClickListener {
                val newStatus = b.spinnerStatus.selectedItem.toString()
                onStatusChange(order, newStatus)
            }

            // ── View Delivery Location (Staff & Admin only) ──────────
            b.btnViewLocation.setOnClickListener {
                val ctx = b.root.context
                val intent = Intent(ctx, ViewOrderLocationActivity::class.java).apply {
                    putExtra(ViewOrderLocationActivity.EXTRA_LAT,      order.latitude)
                    putExtra(ViewOrderLocationActivity.EXTRA_LNG,      order.longitude)
                    putExtra(ViewOrderLocationActivity.EXTRA_ADDRESS,  order.deliveryAddress)
                    putExtra(ViewOrderLocationActivity.EXTRA_ORDER_ID, order.id)
                }
                ctx.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAdminOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order) = a.id == b.id
        override fun areContentsTheSame(a: Order, b: Order) = a == b
    }
}
