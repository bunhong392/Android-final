package com.shopapp.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.shopapp.R
import com.shopapp.util.LocaleHelper

/**
 * Read-only map view accessible only by Staff and Admin.
 * Displays the delivery location that the customer picked during checkout.
 */
class ViewOrderLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    companion object {
        const val EXTRA_LAT     = "order_lat"
        const val EXTRA_LNG     = "order_lng"
        const val EXTRA_ADDRESS = "order_address"
        const val EXTRA_ORDER_ID = "order_id"
    }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_order_location)

        val lat     = intent.getDoubleExtra(EXTRA_LAT, 11.5564)
        val lng     = intent.getDoubleExtra(EXTRA_LNG, 104.9282)
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""

        // Toolbar back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Directions button — opens Google Maps navigation to the delivery location
        findViewById<Button>(R.id.btnDirections).setOnClickListener {
            val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback: open in browser if Google Maps app is not installed
                val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving")
                startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            }
        }

        // Title
        findViewById<TextView>(R.id.tvTitle).text = "Delivery Location"

        // Address label
        val tvAddress = findViewById<TextView>(R.id.tvDeliveryAddress)
        tvAddress.text = if (address.isNotBlank()) "📍 $address"
        else "📍 %.5f, %.5f".format(lat, lng)

        // Order ID label
        if (orderId.isNotBlank()) {
            findViewById<TextView>(R.id.tvOrderRef).text = "Order #${orderId.take(8).uppercase()}"
        }

        // Initialise Google Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapViewLocation) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Store lat/lng so onMapReady can use them
        _lat = lat
        _lng = lng
    }

    private var _lat = 0.0
    private var _lng = 0.0

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        // Disable tap-to-place — this is view-only
        map.setOnMapClickListener(null)

        val location = LatLng(_lat, _lng)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
        map.addMarker(
            MarkerOptions()
                .position(location)
                .title("Delivery Here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )?.showInfoWindow()
    }
}
