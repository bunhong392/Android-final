package com.shopapp.ui.payment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.shopapp.R
import com.shopapp.databinding.ActivityOrderTrackingBinding
import com.shopapp.util.LocaleHelper

class OrderTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityOrderTrackingBinding
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var map: GoogleMap
    private var driverMarker: Marker? = null
    private var destMarker: Marker? = null

    private val statusSteps = listOf(
        "✅ Order confirmed! Preparing your items...",
        "🧑‍🍳 Your order is being prepared...",
        "📦 Packing your items...",
        "🚗 Driver is on the way to you!"
    )
    private var currentStep = 0

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderId       = intent.getStringExtra("order_id") ?: ""
        val totalAmount   = intent.getDoubleExtra("total_amount", 0.0)
        val paymentMethod = intent.getStringExtra("payment_method") ?: "ABA"
        val destLat       = intent.getDoubleExtra("dest_lat", 11.5564)
        val destLng       = intent.getDoubleExtra("dest_lng", 104.9282)

        binding.tvOrderId.text       = "Order #${orderId.take(8).uppercase()}"
        binding.tvPaymentMethod.text = paymentMethod
        binding.tvTotalPaid.text     = "$${String.format("%.2f", totalAmount)}"
        binding.tvEstimate.text      = "30 – 45 min"
        binding.tvStatus.text        = statusSteps[0]

        // Store dest for map setup
        this.destLat = destLat
        this.destLng = destLng

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapTracking) as SupportMapFragment
        mapFragment.getMapAsync(this)

        animateStatus()

        binding.btnSuccess.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            goToSuccess(orderId)
        }

        handler.postDelayed({ goToSuccess(orderId) }, 8000)
    }

    private var destLat = 11.5564
    private var destLng = 104.9282

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true

        val dest = LatLng(destLat, destLng)

        // Driver starts ~500m north of destination
        val driverStart = LatLng(destLat + 0.005, destLng - 0.003)

        // Destination marker (red pin)
        destMarker = map.addMarker(
            MarkerOptions()
                .position(dest)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        // Driver marker (blue pin)
        driverMarker = map.addMarker(
            MarkerOptions()
                .position(driverStart)
                .title("Driver 🛵")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        // Draw route line
        map.addPolyline(
            PolylineOptions()
                .add(driverStart, dest)
                .width(6f)
                .color(0xFF2196F3.toInt())
        )

        // Zoom to show both points
        val midLat = (driverStart.latitude + dest.latitude) / 2
        val midLng = (driverStart.longitude + dest.longitude) / 2
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 14f))

        // Animate driver marker toward destination over 6 seconds (12 steps)
        animateDriver(driverStart, dest, steps = 12, delayMs = 500L)
    }

    /** Moves the driver marker step-by-step toward dest */
    private fun animateDriver(from: LatLng, to: LatLng, steps: Int, delayMs: Long) {
        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                if (step >= steps) return
                step++
                val fraction = step.toFloat() / steps
                val lat = from.latitude  + (to.latitude  - from.latitude)  * fraction
                val lng = from.longitude + (to.longitude - from.longitude) * fraction
                driverMarker?.position = LatLng(lat, lng)
                if (step < steps) handler.postDelayed(this, delayMs)
            }
        }
        handler.postDelayed(runnable, delayMs)
    }

    private fun animateStatus() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                currentStep = (currentStep + 1) % statusSteps.size
                binding.tvStatus.text = statusSteps[currentStep]
                if (currentStep < statusSteps.size - 1) {
                    handler.postDelayed(this, 2000)
                }
            }
        }, 2000)
    }

    private fun goToSuccess(orderId: String) {
        val intent = Intent(this, OrderSuccessActivity::class.java)
        intent.putExtra("order_id", orderId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
