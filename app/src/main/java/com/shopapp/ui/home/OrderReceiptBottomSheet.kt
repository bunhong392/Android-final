package com.shopapp.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shopapp.data.model.Order
import java.text.SimpleDateFormat
import java.util.*

/**
 * OrderReceiptBottomSheet
 * ─────────────────────────────────────────────────────────────
 * Shows a styled receipt when the user taps any order card.
 * Built entirely in Kotlin (no extra XML layout needed).
 */
class OrderReceiptBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_ORDER_ID      = "order_id"
        private const val ARG_USER_NAME     = "user_name"
        private const val ARG_USER_PHONE    = "user_phone"
        private const val ARG_ADDRESS       = "address"
        private const val ARG_PAYMENT       = "payment"
        private const val ARG_STATUS        = "status"
        private const val ARG_TOTAL         = "total"
        private const val ARG_CREATED_AT    = "created_at"
        private const val ARG_ITEM_NAMES    = "item_names"
        private const val ARG_ITEM_QTYS     = "item_qtys"
        private const val ARG_ITEM_PRICES   = "item_prices"

        fun newInstance(order: Order): OrderReceiptBottomSheet {
            val args = Bundle().apply {
                putString(ARG_ORDER_ID,    order.id)
                putString(ARG_USER_NAME,   order.userName)
                putString(ARG_USER_PHONE,  order.userPhone)
                putString(ARG_ADDRESS,     order.deliveryAddress)
                putString(ARG_PAYMENT,     order.paymentMethod)
                putString(ARG_STATUS,      order.status)
                putDouble(ARG_TOTAL,       order.totalAmount)
                putLong(ARG_CREATED_AT,    order.createdAt)
                putStringArrayList(ARG_ITEM_NAMES,
                    ArrayList(order.items.map { it.productName }))
                putIntegerArrayList(ARG_ITEM_QTYS,
                    ArrayList(order.items.map { it.quantity }))
                putStringArrayList(ARG_ITEM_PRICES,
                    ArrayList(order.items.map { "$${String.format("%.2f", it.totalPrice)}" }))
            }
            return OrderReceiptBottomSheet().apply { arguments = args }
        }
    }

    // ── Build the entire receipt UI in code ───────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val a   = requireArguments()

        val dp  = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * dp).toInt()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
            setBackgroundColor(Color.WHITE)
        }

        // ── Header ────────────────────────────────────────────────────────
        root.addView(centeredText(ctx, "🧾 Receipt", 20f, Color.parseColor("#1A1A2E"), true))
        root.addView(spacer(ctx, dp(4)))

        val orderId = a.getString(ARG_ORDER_ID, "").take(8).uppercase()
        root.addView(centeredText(ctx, "Order #$orderId", 13f, Color.parseColor("#888888")))
        root.addView(centeredText(ctx,
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(Date(a.getLong(ARG_CREATED_AT))),
            12f, Color.parseColor("#AAAAAA")))

        root.addView(spacer(ctx, dp(16)))
        root.addView(divider(ctx))
        root.addView(spacer(ctx, dp(12)))

        // ── Customer info ─────────────────────────────────────────────────
        root.addView(sectionLabel(ctx, "CUSTOMER"))
        root.addView(spacer(ctx, dp(6)))
        root.addView(row(ctx, " Name",  a.getString(ARG_USER_NAME,  "")))
        root.addView(row(ctx, " Phone", a.getString(ARG_USER_PHONE, "")))
        root.addView(row(ctx, " Address", a.getString(ARG_ADDRESS,  "")))

        root.addView(spacer(ctx, dp(12)))
        root.addView(divider(ctx))
        root.addView(spacer(ctx, dp(12)))

        // ── Items ─────────────────────────────────────────────────────────
        root.addView(sectionLabel(ctx, "ITEMS"))
        root.addView(spacer(ctx, dp(6)))

        val names  = a.getStringArrayList(ARG_ITEM_NAMES)  ?: arrayListOf()
        val qtys   = a.getIntegerArrayList(ARG_ITEM_QTYS)  ?: arrayListOf()
        val prices = a.getStringArrayList(ARG_ITEM_PRICES) ?: arrayListOf()

        names.forEachIndexed { i, name ->
            val qty   = qtys.getOrElse(i) { 1 }
            val price = prices.getOrElse(i) { "" }
            root.addView(row(ctx, "• $name  ×$qty", price))
        }

        root.addView(spacer(ctx, dp(12)))
        root.addView(divider(ctx))
        root.addView(spacer(ctx, dp(12)))

        // ── Payment & total ───────────────────────────────────────────────
        root.addView(sectionLabel(ctx, "PAYMENT"))
        root.addView(spacer(ctx, dp(6)))
        root.addView(row(ctx, "💳 Method", a.getString(ARG_PAYMENT, "")))

        val statusStr = a.getString(ARG_STATUS, "pending")
        val statusColor = when (statusStr) {
            "pending"    -> Color.parseColor("#FF9800")
            "confirmed"  -> Color.parseColor("#2196F3")
            "delivering" -> Color.parseColor("#9C27B0")
            "delivered"  -> Color.parseColor("#4CAF50")
            else         -> Color.GRAY
        }
        root.addView(row(ctx, "📦 Status", statusStr.uppercase(), valueColor = statusColor))

        root.addView(spacer(ctx, dp(12)))
        root.addView(divider(ctx, height = 2, color = Color.parseColor("#E0E0E0")))
        root.addView(spacer(ctx, dp(10)))

        // Total row — bold + bigger
        root.addView(
            row(ctx,
                "TOTAL",
                "$${String.format("%.2f", a.getDouble(ARG_TOTAL))}",
                keyBold = true, valueBold = true,
                keyColor = Color.parseColor("#1A1A2E"),
                valueColor = Color.parseColor("#1A73E8"),
                textSizeSp = 16f
            )
        )

        root.addView(spacer(ctx, dp(20)))

        // Thank-you note
        root.addView(centeredText(ctx, "Thank you for your order! 🎉", 13f,
            Color.parseColor("#888888")))

        return root
    }

    // ── DSL helpers ───────────────────────────────────────────────────────

    private fun centeredText(
        ctx: android.content.Context,
        text: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(ctx).apply {
        this.text = text
        textSize = sizeSp
        setTextColor(color)
        gravity = android.view.Gravity.CENTER
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun sectionLabel(ctx: android.content.Context, text: String) =
        TextView(ctx).apply {
            this.text = text
            textSize = 10f
            setTextColor(Color.parseColor("#AAAAAA"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            letterSpacing = 0.15f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

    private fun row(
        ctx: android.content.Context,
        key: String,
        value: String,
        keyBold: Boolean = false,
        valueBold: Boolean = false,
        keyColor: Int = Color.parseColor("#555555"),
        valueColor: Int = Color.parseColor("#1A1A2E"),
        textSizeSp: Float = 13f
    ): LinearLayout {
        val dp = ctx.resources.displayMetrics.density
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (3 * dp).toInt(), 0, (3 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(TextView(ctx).apply {
                text = key
                textSize = textSizeSp
                setTextColor(keyColor)
                if (keyBold) setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(ctx).apply {
                this.text = value
                textSize = textSizeSp
                setTextColor(valueColor)
                if (valueBold) setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })
        }
    }

    private fun divider(
        ctx: android.content.Context,
        height: Int = 1,
        color: Int = Color.parseColor("#F0F0F0")
    ) = View(ctx).apply {
        setBackgroundColor(color)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (height * ctx.resources.displayMetrics.density).toInt()
        )
    }

    private fun spacer(ctx: android.content.Context, heightPx: Int) =
        View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightPx
            )
        }
}
