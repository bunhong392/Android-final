package com.shopapp.ui.admin

import androidx.lifecycle.lifecycleScope
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.shopapp.R
import com.shopapp.data.model.Product
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentAdminAddProductBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import com.shopapp.ui.staff.StaffActivity

class AdminAddProductFragment(
    private val editProduct: Product? = null
) : Fragment() {

    private var _binding: FragmentAdminAddProductBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    // ── Use ACTION_GET_CONTENT — works on emulator AND real device ────────────
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult

            // Take persistent permission so URI survives process restarts
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            selectedImageUri = uri

            // Copy to internal storage so it works even if original is moved/deleted
            val savedPath = saveImageToInternalStorage(uri)
            binding.etImageUrl.setText(savedPath)

            // Show preview
            Glide.with(this)
                .load(if (savedPath.startsWith("/")) File(savedPath) else uri)
                .centerCrop()
                .placeholder(R.drawable.ic_product_placeholder)
                .into(binding.ivPhotoPreview)
            binding.ivPhotoPreview.visibility = View.VISIBLE
            binding.tvPhotoHint.text = "Photo selected ✓"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySpinner()
        setupAvailabilitySpinner()
        prefillIfEditing()

        binding.cardPhotoUpload.setOnClickListener { openImagePicker() }
        binding.btnSaveProduct.setOnClickListener { saveProduct() }

        val title = if (editProduct == null) "Add product" else "Edit product"
        val tv = (activity as? AdminActivity)?.tvPageTitle
            ?: (activity as? StaffActivity)?.tvPageTitle
        tv?.text = title
    }

    private fun openImagePicker() {
        // ACTION_GET_CONTENT opens the full file browser — works on emulator too
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            // Allow user to pick from files app, photos, drive, etc.
            putExtra(Intent.EXTRA_LOCAL_ONLY, false)
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select product photo"))
    }

    // Copy image bytes to app-internal storage → path never breaks
    private fun saveImageToInternalStorage(uri: Uri): String {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
                ?: return uri.toString()
            val fileName = "product_img_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            FileOutputStream(file).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            uri.toString()   // fallback: store the URI string
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Food", "Drinks", "Electronics", "Clothing", "Other")
        binding.spinnerCategory.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, categories
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun setupAvailabilitySpinner() {
        val stores = listOf("All stores", "Store A", "Store B", "Online only")
        binding.spinnerAvailability.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, stores
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun prefillIfEditing() {
        editProduct?.let { p ->
            binding.etName.setText(p.name)
            binding.etDescription.setText(p.description)
            binding.etPrice.setText(p.price.toString())
            binding.etStock.setText(p.stock.toString())
            binding.etImageUrl.setText(p.imageUrl)

            val idx = listOf("Food","Drinks","Electronics","Clothing","Other").indexOf(p.category)
            if (idx >= 0) binding.spinnerCategory.setSelection(idx)
            binding.btnSaveProduct.text = "Update product"

            if (p.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(if (p.imageUrl.startsWith("/")) File(p.imageUrl) else p.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_product_placeholder)
                    .into(binding.ivPhotoPreview)
                binding.ivPhotoPreview.visibility = View.VISIBLE
                binding.tvPhotoHint.text = "Photo selected ✓"
            }
        }
    }

    private fun saveProduct() {
        val name        = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceStr    = binding.etPrice.text.toString().trim()
        val stockStr    = binding.etStock.text.toString().trim()
        val imageUrl    = binding.etImageUrl.text.toString().trim()
        val category    = binding.spinnerCategory.selectedItem.toString()

        if (name.isEmpty())     { binding.etName.error = "Required"; return }
        if (priceStr.isEmpty()) { binding.etPrice.error = "Required"; return }
        if (stockStr.isEmpty()) { binding.etStock.error = "Required"; return }

        val price = priceStr.toDoubleOrNull() ?: run {
            Toast.makeText(requireContext(), "Invalid price", Toast.LENGTH_SHORT).show(); return
        }
        val stock = stockStr.toIntOrNull() ?: run {
            Toast.makeText(requireContext(), "Invalid stock", Toast.LENGTH_SHORT).show(); return
        }

        setLoading(true)

        val product = Product(
            id        = editProduct?.id ?: "",
            name      = name, description = description,
            price     = price, stock = stock,
            category  = category, imageUrl = imageUrl,
            createdAt = editProduct?.createdAt ?: System.currentTimeMillis()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = if (editProduct == null)
                Repository.addProduct(product).map { "Product added successfully!" }
            else
                Repository.updateProduct(product).map { "Product updated successfully!" }

            setLoading(false)
            result.onSuccess { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                if (editProduct == null) clearForm()
                else {
                    (activity as? AdminActivity)?.loadFragment(AdminProductsFragment())
                        ?: (activity as? StaffActivity)?.loadFragment(AdminProductsFragment())
                }
            }.onFailure {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearForm() {
        binding.etName.text?.clear()
        binding.etDescription.text?.clear()
        binding.etPrice.text?.clear()
        binding.etStock.text?.clear()
        binding.etImageUrl.text?.clear()
        binding.spinnerCategory.setSelection(0)
        binding.ivPhotoPreview.visibility = View.GONE
        binding.tvPhotoHint.text = "Tap to upload photo"
        selectedImageUri = null
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSaveProduct.isEnabled = !loading
        binding.btnSaveProduct.text = if (loading) "Saving..."
            else if (editProduct == null) "Save product" else "Update product"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
