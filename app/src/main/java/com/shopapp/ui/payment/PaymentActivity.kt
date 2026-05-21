package com.shopapp.ui.payment

import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shopapp.data.model.Order
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.ActivityPaymentBinding
import com.shopapp.util.LocaleHelper
import kotlinx.coroutines.launch

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private var selectedPayment = "ABA"

    private var pickedLat: Double? = null
    private var pickedLng: Double? = null

    private var pendingOrderAddress: String = ""
    private var pendingOrderTotal:   Double = 0.0

    companion object {
        private const val REQUEST_MAP_PICK = 1001
        const val EXTRA_LAT  = "picked_lat"
        const val EXTRA_LNG  = "picked_lng"
        const val EXTRA_ADDR = "picked_address"
    }

    private data class PaymentCard(val card: FrameLayout, val check: ImageView, val method: String)
    private lateinit var paymentCards: List<PaymentCard>

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        setupPaymentCards()
        loadCartSummary()

        binding.btnPickMap.setOnClickListener {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(this, MapPickerActivity::class.java), REQUEST_MAP_PICK)
        }

        binding.btnPay.setOnClickListener { onPayNowClicked() }

        // After QR sheet → user taps "I've Paid" → save the order
        supportFragmentManager.setFragmentResultListener(
            QRPaymentBottomSheet.REQUEST_KEY, this
        ) { _, _ ->
            saveOrderAndProceed()
        }
    }

    // ── Pay Now ───────────────────────────────────────────────────────────────
    private fun onPayNowClicked() {
        val address = binding.etAddress.text.toString().trim()
        if (address.isEmpty()) {
            Toast.makeText(
                this, getString(com.shopapp.R.string.enter_address), Toast.LENGTH_SHORT
            ).show()
            return
        }
        pendingOrderAddress = address

        if (selectedPayment == "ABA" || selectedPayment == "AC") {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnPay.isEnabled = false
            lifecycleScope.launch {
                val items = Repository.getCart().getOrNull()
                if (items.isNullOrEmpty()) {
                    resetButton()
                    Toast.makeText(this@PaymentActivity, "Your cart is empty", Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }
                pendingOrderTotal = items.sumOf { it.totalPrice }
                resetButton()
                QRPaymentBottomSheet
                    .newInstance(
                        selectedPayment,
                        pendingOrderTotal,
                        java.util.UUID.randomUUID().toString()
                    )
                    .show(supportFragmentManager, "qr_payment")
            }
        } else {
            saveOrderAndProceed()
        }
    }

    // ── Save Order ────────────────────────────────────────────────────────────
    /**
     * Performs a final live stock check, then calls Repository.placeOrder which
     * uses Firebase atomic transactions to deduct stock.  If any product has
     * run out between the cart-load and this moment, the order is rejected with
     * a friendly message.
     */
    private fun saveOrderAndProceed() {
        val address =
            pendingOrderAddress.ifEmpty { binding.etAddress.text.toString().trim() }
        if (address.isEmpty()) {
            Toast.makeText(
                this, getString(com.shopapp.R.string.enter_address), Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnPay.isEnabled = false
        binding.btnPay.text = getString(com.shopapp.R.string.processing)

        lifecycleScope.launch {
            try {
                val items = Repository.getCart().getOrNull()
                val user  = Repository.getCurrentUser()

                if (items.isNullOrEmpty()) {
                    resetButton()
                    Toast.makeText(this@PaymentActivity, "Cart is empty", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (user == null) {
                    resetButton()
                    Toast.makeText(
                        this@PaymentActivity, "Please login first", Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // ── Pre-flight stock check ────────────────────────────────────
                for (item in items) {
                    val product = Repository.getProduct(item.productId).getOrNull()
                    val available = product?.stock ?: 0
                    if (available < item.quantity) {
                        resetButton()
                        val msg = if (available == 0)
                            "${item.productName} is now out of stock. Please remove it from your cart."
                        else
                            "Only $available ${item.productName}(s) left. Please update your cart."
                        Toast.makeText(this@PaymentActivity, msg, Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                val total = if (pendingOrderTotal > 0) pendingOrderTotal
                            else items.sumOf { it.totalPrice }

                val order = Order(
                    userId        = user.uid,
                    userName      = user.name,
                    userPhone     = user.phone,
                    items         = items,
                    totalAmount   = total,
                    deliveryAddress = address,
                    paymentMethod = selectedPayment,
                    status        = "pending",
                    latitude      = pickedLat ?: 11.5564,
                    longitude     = pickedLng ?: 104.9282
                )

                // Repository.placeOrder atomically deducts stock via Firebase transaction
                val result = Repository.placeOrder(order)

                if (result.isFailure) {
                    resetButton()
                    val errMsg = result.exceptionOrNull()?.message ?: "Order failed"
                    // Could be "Not enough stock for Apple" from the transaction
                    Toast.makeText(this@PaymentActivity, errMsg, Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Success → navigate to order tracking
                startActivity(
                    Intent(this@PaymentActivity, OrderTrackingActivity::class.java).apply {
                        putExtra("order_id",       result.getOrNull() ?: "")
                        putExtra("total_amount",   total)
                        putExtra("payment_method", selectedPayment)
                        putExtra("dest_lat",        pickedLat ?: 11.5564)
                        putExtra("dest_lng",        pickedLng ?: 104.9282)
                    }
                )
                finish()

            } catch (e: Exception) {
                resetButton()
                Toast.makeText(this@PaymentActivity, "Error: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    // ── Payment Cards ─────────────────────────────────────────────────────────
    private fun setupPaymentCards() {
        paymentCards = listOf(
            PaymentCard(binding.cardAba,      binding.icAbaCheck, "ABA"),
            PaymentCard(binding.cardAc,       binding.icAcCheck,  "AC"),
            PaymentCard(binding.cardDelivery, binding.icCodCheck, "Delivery")
        )
        paymentCards.forEach { e -> e.card.setOnClickListener { selectPayment(e.method) } }
        selectPayment("ABA")
    }

    private fun selectPayment(method: String) {
        selectedPayment = method
        paymentCards.forEach { e ->
            val on = e.method == method
            e.card.isSelected  = on
            e.check.visibility = if (on) View.VISIBLE else View.GONE
            if (on) e.card.animate().scaleX(1.04f).scaleY(1.04f).setDuration(120)
                .withEndAction {
                    e.card.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }.start()
        }
        binding.btnPay.text = when (method) {
            "ABA", "AC" -> "🔲  Pay with QR"
            else         -> getString(com.shopapp.R.string.pay_now)
        }
    }

    // ── Cart / Map / Helpers ──────────────────────────────────────────────────
    private fun loadCartSummary() {
        lifecycleScope.launch {
            Repository.getCart().onSuccess { items ->
                val total = items.sumOf { it.totalPrice }
                binding.tvItemCount.text   = "${items.size} item(s)"
                binding.tvTotalAmount.text = "$${String.format("%.2f", total)}"
                binding.tvTotalMoney.text  = "$${String.format("%.2f", total)}"
                pendingOrderTotal = total
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MAP_PICK && resultCode == RESULT_OK && data != null) {
            pickedLat = data.getDoubleExtra(EXTRA_LAT, 0.0)
            pickedLng = data.getDoubleExtra(EXTRA_LNG, 0.0)
            val addr  = data.getStringExtra(EXTRA_ADDR) ?: ""
            binding.etAddress.setText(addr)
            binding.tvMapCoords.visibility = View.VISIBLE
            binding.tvMapCoords.text = "📌 %.5f, %.5f".format(pickedLat, pickedLng)
        }
    }

    private fun resetButton() {
        binding.progressBar.visibility = View.GONE
        binding.btnPay.isEnabled = true
        binding.btnPay.text = when (selectedPayment) {
            "ABA", "AC" -> "🔲  Pay with QR"
            else         -> getString(com.shopapp.R.string.pay_now)
        }
    }
}
