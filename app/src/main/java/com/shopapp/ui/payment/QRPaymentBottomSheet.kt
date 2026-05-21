package com.shopapp.ui.payment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.collection.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class QRPaymentBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val REQUEST_KEY     = "qr_confirmed"
        const val RESULT_ORDER_ID = "order_id"

        private const val ARG_METHOD   = "method"
        private const val ARG_AMOUNT   = "amount"
        private const val ARG_ORDER_ID = "order_id"
        private const val QR_SIZE      = 700
        private const val TIMEOUT_SEC  = 300  // 5 minutes

        // Replace with your real merchant IDs from ABA / ACLEDA
        private const val ABA_MERCHANT_ID   = "000000001234"
        private const val ABA_MERCHANT_NAME = "ShopApp Store"
        private const val AC_MERCHANT_ID    = "AC9876543210"
        private const val AC_MERCHANT_NAME  = "ShopApp Store"

        fun buildQrPayload(method: String, amount: Double, orderId: String): String {
            val amt = String.format(Locale.US, "%.2f", amount)
            return when (method) {
                "ABA" ->
                    "https://pay.ababank.com/payment?" +
                            "merchantID=$ABA_MERCHANT_ID&amount=$amt&currency=USD" +
                            "&orderID=$orderId" +
                            "&merchantName=${ABA_MERCHANT_NAME.replace(" ", "+")}"
                "AC" ->
                    "https://unitypay.acleda.com.kh/qr?" +
                            "mid=$AC_MERCHANT_ID&amt=$amt&cur=USD" +
                            "&ref=$orderId" +
                            "&name=${AC_MERCHANT_NAME.replace(" ", "+")}"
                else -> ""
            }
        }

        fun newInstance(method: String, amount: Double, orderId: String) =
            QRPaymentBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_METHOD,   method)
                    putDouble(ARG_AMOUNT,   amount)
                    putString(ARG_ORDER_ID, orderId)
                }
            }
    }

    // ── Args ───────────────────────────────────────────────────────────────
    private val method  by lazy { requireArguments().getString(ARG_METHOD,   "ABA") }
    private val amount  by lazy { requireArguments().getDouble(ARG_AMOUNT,   0.0)  }
    private val orderId by lazy { requireArguments().getString(ARG_ORDER_ID, "")   }

    private var countdownJob: Job? = null

    // ── Colors as Int literals ─────────────────────────────────────────────
    private val colAbaRed       = 0xFFCC0000.toInt()
    private val colAcBlue       = 0xFF003087.toInt()
    private val colAbaCardBg    = 0xFFFFF5F5.toInt()
    private val colAcCardBg     = 0xFFF0F4FF.toInt()
    private val colAbaLogoBg    = 0xFFFFEEEE.toInt()
    private val colAcLogoBg     = 0xFFEEF3FF.toInt()
    private val colHandle       = 0xFFE0E0E0.toInt()
    private val colGray         = 0xFF888888.toInt()
    private val colLightGray    = 0xFFAAAAAA.toInt()
    private val colInstructions = 0xFF555555.toInt()
    private val colOrange       = 0xFFFF9800.toInt()
    private val colWhite        = 0xFFFFFFFF.toInt()
    private val colRed          = 0xFFFF0000.toInt()
    private val colBlack        = 0xFF000000.toInt()

    // ── Expand to full height ──────────────────────────────────────────────
    override fun onStart() {
        super.onStart()
        val sheet = (dialog as? BottomSheetDialog)
            ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return
        BottomSheetBehavior.from(sheet).apply {
            state         = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        sheet.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    // ── Build UI ───────────────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, state: Bundle?
    ): View {
        val ctx = requireContext()
        val dpF = ctx.resources.displayMetrics.density
        fun Int.dp() = (this * dpF).toInt()

        val isAba     = method == "ABA"
        val bankColor = if (isAba) colAbaRed    else colAcBlue
        val cardBg    = if (isAba) colAbaCardBg else colAcCardBg
        val logoBg    = if (isAba) colAbaLogoBg else colAcLogoBg
        val bankName  = if (isAba) "ABA Bank"   else "ACLEDA Bank"
        // Root
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colWhite)
            setPadding(24.dp(), 0, 24.dp(), 32.dp())
        }

        // Drag handle
        val handleFrame = FrameLayout(ctx)
        handleFrame.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 28.dp())
        val handleView = View(ctx)
        handleView.setBackgroundColor(colHandle)
        val handleLp = FrameLayout.LayoutParams(48.dp(), 4.dp())
        handleLp.gravity = android.view.Gravity.CENTER
        handleView.layoutParams = handleLp
        handleFrame.addView(handleView)
        root.addView(handleFrame)

        // Bank header row
        val headerRow = LinearLayout(ctx)
        headerRow.orientation = LinearLayout.HORIZONTAL
        headerRow.gravity     = android.view.Gravity.CENTER
        headerRow.setPadding(0, 0, 0, 12.dp())
        headerRow.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        val logoLp = LinearLayout.LayoutParams(56.dp(), 56.dp())
        logoLp.marginEnd = 12.dp()
        val logoView = ImageView(ctx)
        logoView.scaleType = ImageView.ScaleType.FIT_CENTER
        logoView.setBackgroundColor(logoBg)
        logoView.layoutParams = logoLp
        val logoRes = if (isAba) com.shopapp.R.drawable.aba else com.shopapp.R.drawable.ac
        logoView.setImageResource(logoRes)

        val nameCol = LinearLayout(ctx)
        nameCol.orientation = LinearLayout.VERTICAL

        val tvBankName = TextView(ctx)
        tvBankName.text     = bankName
        tvBankName.textSize = 18f
        tvBankName.setTextColor(bankColor)
        tvBankName.setTypeface(null, android.graphics.Typeface.BOLD)

        val tvSubtitle = TextView(ctx)
        tvSubtitle.text     = "Scan QR to pay"
        tvSubtitle.textSize = 12f
        tvSubtitle.setTextColor(colGray)

        nameCol.addView(tvBankName)
        nameCol.addView(tvSubtitle)
        headerRow.addView(logoView)
        headerRow.addView(nameCol)
        root.addView(headerRow)

        // Amount card
        val amountCard = LinearLayout(ctx)
        amountCard.orientation = LinearLayout.VERTICAL
        amountCard.gravity     = android.view.Gravity.CENTER
        amountCard.setBackgroundColor(cardBg)
        amountCard.setPadding(0, 14.dp(), 0, 14.dp())
        val amountLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        amountLp.bottomMargin = 16.dp()
        amountCard.layoutParams = amountLp

        val tvAmountLabel = TextView(ctx)
        tvAmountLabel.text     = "Amount to Pay"
        tvAmountLabel.textSize = 12f
        tvAmountLabel.setTextColor(colGray)
        tvAmountLabel.gravity  = android.view.Gravity.CENTER

        val tvAmount = TextView(ctx)
        tvAmount.text     = String.format(Locale.US, "$%.2f", amount)
        tvAmount.textSize = 30f
        tvAmount.setTextColor(bankColor)
        tvAmount.setTypeface(null, android.graphics.Typeface.BOLD)
        tvAmount.gravity  = android.view.Gravity.CENTER

        val tvOrderRef = TextView(ctx)
        tvOrderRef.text     = "Order #${orderId.take(8).uppercase(Locale.US)}"
        tvOrderRef.textSize = 11f
        tvOrderRef.setTextColor(colLightGray)
        tvOrderRef.gravity  = android.view.Gravity.CENTER

        amountCard.addView(tvAmountLabel)
        amountCard.addView(tvAmount)
        amountCard.addView(tvOrderRef)
        root.addView(amountCard)

        // QR image
        val qrLp = LinearLayout.LayoutParams(MATCH_PARENT, 260.dp())
        qrLp.bottomMargin = 10.dp()
        val ivQr = ImageView(ctx)
        ivQr.scaleType    = ImageView.ScaleType.FIT_CENTER
        ivQr.layoutParams = qrLp
        root.addView(ivQr)

        // Progress spinner
        val progressLp = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        progressLp.gravity      = android.view.Gravity.CENTER_HORIZONTAL
        progressLp.bottomMargin = 10.dp()
        val qrProgress = ProgressBar(ctx)
        qrProgress.layoutParams = progressLp
        qrProgress.indeterminateTintList =
            android.content.res.ColorStateList.valueOf(bankColor)
        root.addView(qrProgress)

        // Generate QR on IO thread
//        CoroutineScope(Dispatchers.IO).launch {
//            val bmp = generateQr(buildQrPayload(method, amount, orderId))
//            withContext(Dispatchers.Main) {
//                if (!isAdded) return@withContext
//                qrProgress.visibility = View.GONE
//                ivQr.setImageBitmap(bmp)
//            }
//        }

        val qrDrawable = if (isAba) com.shopapp.R.drawable.qr_aba else com.shopapp.R.drawable.qr_acleda
        ivQr.setImageResource(qrDrawable)
        qrProgress.visibility = View.GONE
        // Countdown timer
        val countdownLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        countdownLp.bottomMargin = 18.dp()
        val tvCountdown = TextView(ctx)
        tvCountdown.textSize     = 13f
        tvCountdown.setTextColor(colOrange)
        tvCountdown.gravity      = android.view.Gravity.CENTER
        tvCountdown.layoutParams = countdownLp
        root.addView(tvCountdown)

        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            var remaining = TIMEOUT_SEC
            while (remaining > 0) {
                val m = remaining / 60
                val s = remaining % 60
                tvCountdown.text = String.format(Locale.US, "⏱ QR expires in %02d:%02d", m, s)
                delay(1000)
                remaining--
            }
            tvCountdown.text = "⚠️ QR expired. Please try again."
            tvCountdown.setTextColor(colRed)
        }

        // Instructions
        val instrText = if (isAba)
            "1. Open ABA Mobile app\n2. Tap Pay → Scan QR\n3. Scan the code above\n4. Confirm payment in the app"
        else
            "1. Open ACLEDA Unity app\n2. Tap Scan & Pay\n3. Scan the code above\n4. Confirm payment in the app"

        val instrLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        instrLp.bottomMargin = 20.dp()
        val tvInstr = TextView(ctx)
        tvInstr.text         = instrText
        tvInstr.textSize     = 13f
        tvInstr.setTextColor(colInstructions)
        tvInstr.setLineSpacing(0f, 1.5f)   // setLineSpacing() avoids the val reassign warning
        tvInstr.layoutParams = instrLp
        root.addView(tvInstr)

        // Confirm button
        val btnConfirm = Button(ctx)
        btnConfirm.text     = "✅  I've Paid"
        btnConfirm.textSize = 15f
        btnConfirm.setTextColor(colWhite)
        btnConfirm.setBackgroundColor(bankColor)
        btnConfirm.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 52.dp())
        btnConfirm.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply { putString(RESULT_ORDER_ID, orderId) }
            )
            dismiss()
        }
        root.addView(btnConfirm)

        return root
    }

    // ── QR generation ──────────────────────────────────────────────────────
//    private fun generateQr(content: String): Bitmap {
//        val hints = mapOf(
//            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
//            EncodeHintType.MARGIN to 1
//        )
//        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)
//        val bmp = createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565)
//        for (x in 0 until QR_SIZE) {
//            for (y in 0 until QR_SIZE) {
//                bmp[x, y] = if (matrix[x, y]) colBlack else colWhite
//            }
//        }
//        return bmp
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownJob?.cancel()
    }
}