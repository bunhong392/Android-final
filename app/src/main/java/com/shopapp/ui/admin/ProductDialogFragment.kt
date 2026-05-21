package com.shopapp.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.shopapp.data.model.Product
import com.shopapp.databinding.DialogProductBinding

class ProductDialogFragment(
    private val product: Product?,
    private val onSave: (Product) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogProductBinding.inflate(layoutInflater)

        val categories = listOf("Food", "Drinks", "Electronics", "Clothing", "Other")
        val catAdapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, categories)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = catAdapter

        // Pre-fill for edit
        product?.let { p ->
            binding.etName.setText(p.name)
            binding.etDescription.setText(p.description)
            binding.etPrice.setText(p.price.toString())
            binding.etStock.setText(p.stock.toString())
            binding.etImageUrl.setText(p.imageUrl)
            val idx = categories.indexOf(p.category)
            if (idx >= 0) binding.spinnerCategory.setSelection(idx)
        }

        val title = if (product == null) "Add Product" else "Edit Product"

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = binding.etName.text.toString().trim()
                val description = binding.etDescription.text.toString().trim()
                val priceStr = binding.etPrice.text.toString().trim()
                val stockStr = binding.etStock.text.toString().trim()
                val imageUrl = binding.etImageUrl.text.toString().trim()
                val category = binding.spinnerCategory.selectedItem.toString()

                if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val price = priceStr.toDoubleOrNull() ?: 0.0
                val stock = stockStr.toIntOrNull() ?: 0

                val updated = Product(
                    id = product?.id ?: "",
                    name = name,
                    description = description,
                    price = price,
                    stock = stock,
                    category = category,
                    imageUrl = imageUrl,
                    createdAt = product?.createdAt ?: System.currentTimeMillis()
                )
                onSave(updated)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
