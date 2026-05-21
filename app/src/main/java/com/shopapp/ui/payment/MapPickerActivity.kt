package com.shopapp.ui.payment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.shopapp.R
import com.shopapp.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    // ── Views ──────────────────────────────────────────────────────────────
    private lateinit var map: GoogleMap
    private lateinit var tvInstruction: TextView
    private lateinit var tvPickedAddress: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnMyLocation: ImageButton
    private lateinit var etSearchLocation: EditText
    private lateinit var lvSearchResults: ListView
    private lateinit var progressSearch: ProgressBar

    // ── State ──────────────────────────────────────────────────────────────
    private var pickedLatLng: LatLng? = null
    private var pickedAddress: String = ""
    private var searchResults: List<android.location.Address> = emptyList()

    // ── Location ───────────────────────────────────────────────────────────
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val defaultLocation = LatLng(11.5564, 104.9282) // Phnom Penh

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
    }

    // ── Locale ─────────────────────────────────────────────────────────────
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    // ══════════════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Bind views
        tvInstruction      = findViewById(R.id.tvInstruction)
        tvPickedAddress    = findViewById(R.id.tvPickedAddress)
        btnConfirm         = findViewById(R.id.btnConfirmLocation)
        btnMyLocation      = findViewById(R.id.btnMyLocation)
        etSearchLocation   = findViewById(R.id.etSearchLocation)
        lvSearchResults    = findViewById(R.id.lvSearchResults)
        progressSearch     = findViewById(R.id.progressSearch)

        btnConfirm.isEnabled = false

        // Init map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapPicker) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Confirm button
        btnConfirm.setOnClickListener {
            val latlng = pickedLatLng ?: return@setOnClickListener
            val result = Intent().apply {
                putExtra(PaymentActivity.EXTRA_LAT,  latlng.latitude)
                putExtra(PaymentActivity.EXTRA_LNG,  latlng.longitude)
                putExtra(PaymentActivity.EXTRA_ADDR, pickedAddress)
            }
            setResult(RESULT_OK, result)
            finish()
        }

        // My-location FAB
        btnMyLocation.setOnClickListener { requestCurrentLocation() }

        // Search bar
        setupSearchBar()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MAP READY
    // ══════════════════════════════════════════════════════════════════════
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false   // we use our own FAB

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14f))

        // Tap to pick
        map.setOnMapClickListener { latLng ->
            hideSearchResults()
            placeMarker(latLng)
        }

        // Enable the blue-dot layer if permission is already granted
        enableMyLocationLayer()

        // Auto-jump to current location when map first loads
        requestCurrentLocation()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GPS / MY LOCATION
    // ══════════════════════════════════════════════════════════════════════
    private fun requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            fetchAndGoToCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchAndGoToCurrentLocation() {
        // Show a brief loading message
        tvPickedAddress.text = "📡 Getting your location…"
        btnConfirm.isEnabled = false

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                    placeMarker(latLng)
                } else {
                    // lastLocation can be null on first boot / location was off
                    Toast.makeText(
                        this,
                        "⚠️ Could not get location. Make sure GPS is ON, then tap 📍 again.",
                        Toast.LENGTH_LONG
                    ).show()
                    tvPickedAddress.text = "📍 Tap on the map to select location"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Location error: ${it.message}", Toast.LENGTH_SHORT).show()
                tvPickedAddress.text = "📍 Tap on the map to select location"
            }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationLayer() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (::map.isInitialized) map.isMyLocationEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                enableMyLocationLayer()
                fetchAndGoToCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied. Tap the map to set your address.",
                    Toast.LENGTH_LONG
                ).show()
                tvPickedAddress.text = "📍 Tap on the map to select location"
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SEARCH BAR
    // ══════════════════════════════════════════════════════════════════════
    private fun setupSearchBar() {
        // Live typing → clear old results while user is still typing
        etSearchLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) hideSearchResults()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Press "Search" / "Done" on keyboard
        etSearchLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                val query = etSearchLocation.text.toString().trim()
                if (query.isNotEmpty()) {
                    hideKeyboard()
                    searchForLocation(query)
                }
                true
            } else false
        }

        // Tap a result from the list
        lvSearchResults.setOnItemClickListener { _, _, position, _ ->
            val addr = searchResults.getOrNull(position) ?: return@setOnItemClickListener
            val latLng = LatLng(addr.latitude, addr.longitude)
            hideSearchResults()
            etSearchLocation.setText("")
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            placeMarker(latLng)
        }
    }

    private fun searchForLocation(query: String) {
        progressSearch.visibility = View.VISIBLE
        lvSearchResults.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(this@MapPickerActivity, Locale.getDefault())
                @Suppress("DEPRECATION")
                val found = geocoder.getFromLocationName(query, 5) ?: emptyList()

                withContext(Dispatchers.Main) {
                    progressSearch.visibility = View.GONE
                    if (found.isEmpty()) {
                        Toast.makeText(
                            this@MapPickerActivity,
                            "No results found for \"$query\"",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        searchResults = found
                        showSearchResults(found)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressSearch.visibility = View.GONE
                    Toast.makeText(
                        this@MapPickerActivity,
                        "Search failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showSearchResults(addresses: List<android.location.Address>) {
        val labels = addresses.map { addr ->
            buildString {
                if (!addr.featureName.isNullOrBlank() && addr.featureName != addr.thoroughfare)
                    append(addr.featureName).append(", ")
                if (!addr.thoroughfare.isNullOrBlank())
                    append(addr.thoroughfare).append(", ")
                if (!addr.locality.isNullOrBlank())
                    append(addr.locality).append(", ")
                if (!addr.countryName.isNullOrBlank())
                    append(addr.countryName)
            }.trimEnd(',', ' ').ifBlank {
                "%.5f, %.5f".format(addr.latitude, addr.longitude)
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        lvSearchResults.adapter = adapter
        lvSearchResults.visibility = View.VISIBLE
    }

    private fun hideSearchResults() {
        lvSearchResults.visibility = View.GONE
        searchResults = emptyList()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SHARED HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** Drop a marker + reverse-geocode the tapped/resolved location. */
    private fun placeMarker(latLng: LatLng) {
        pickedLatLng = latLng
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Delivery Here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        resolveAddress(latLng)
    }

    private fun resolveAddress(latLng: LatLng) {
        tvPickedAddress.text = "📍 Locating address…"
        btnConfirm.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(this@MapPickerActivity, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val addr = if (!addresses.isNullOrEmpty()) {
                    val a = addresses[0]
                    buildString {
                        if (!a.featureName.isNullOrBlank() && a.featureName != a.thoroughfare)
                            append(a.featureName).append(", ")
                        if (!a.thoroughfare.isNullOrBlank())
                            append(a.thoroughfare).append(", ")
                        if (!a.subLocality.isNullOrBlank())
                            append(a.subLocality).append(", ")
                        if (!a.locality.isNullOrBlank())
                            append(a.locality)
                    }.trimEnd(',', ' ')
                } else {
                    "%.5f, %.5f".format(latLng.latitude, latLng.longitude)
                }

                withContext(Dispatchers.Main) {
                    pickedAddress = addr
                    tvPickedAddress.text = "📍 $addr"
                    btnConfirm.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pickedAddress = "%.5f, %.5f".format(latLng.latitude, latLng.longitude)
                    tvPickedAddress.text = "📍 $pickedAddress"
                    btnConfirm.isEnabled = true
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}
