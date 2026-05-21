package com.shopapp.ui.admin

import androidx.lifecycle.lifecycleScope
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shopapp.data.model.Product
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentAdminProductsBinding
import com.shopapp.ui.staff.StaffActivity
import kotlinx.coroutines.launch

class AdminProductsFragment : Fragment() {
    private var _binding: FragmentAdminProductsBinding? = null
    private val binding get() = _binding!!
    private var allProducts = listOf<Product>()

    /** Works whether this fragment is hosted in AdminActivity or StaffActivity */
    private fun hostLoadFragment(f: androidx.fragment.app.Fragment) {
        (activity as? AdminActivity)?.loadFragment(f)
            ?: (activity as? StaffActivity)?.loadFragment(f)
    }
    private fun setPageTitle(title: String) {
        val tv = (activity as? AdminActivity)?.tvPageTitle
            ?: (activity as? StaffActivity)?.tvPageTitle
        tv?.text = title
    }

    private val adapter = AdminProductAdapter(
        onEdit    = { p -> hostLoadFragment(AdminAddProductFragment(editProduct = p)) },
        onDelete  = { p -> confirmDelete(p) },
        onPopular = { p, isPopular -> togglePopular(p, isPopular) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAdminProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPageTitle("Products")
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#534AB7"))
        binding.swipeRefresh.setOnRefreshListener { loadProducts() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().lowercase()
                adapter.submitList(allProducts.filter { it.name.lowercase().contains(q) })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.chipAll.setOnClickListener         { adapter.submitList(allProducts) }
        binding.chipFood.setOnClickListener        { adapter.submitList(allProducts.filter { it.category == "Food" }) }
        binding.chipDrinks.setOnClickListener      { adapter.submitList(allProducts.filter { it.category == "Drinks" }) }
        binding.chipElectronics.setOnClickListener { adapter.submitList(allProducts.filter { it.category == "Electronics" }) }
        binding.chipClothing.setOnClickListener    { adapter.submitList(allProducts.filter { it.category == "Clothing" }) }

        binding.fabAdd.setOnClickListener { hostLoadFragment(AdminAddProductFragment()) }
        loadProducts()
    }

    override fun onResume() { super.onResume(); loadProducts() }

    private fun loadProducts() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Repository.getProducts()
            binding.swipeRefresh.isRefreshing = false
            result.onSuccess { products ->
                allProducts = products
                adapter.submitList(products)
                binding.layoutEmpty.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Popular toggle ────────────────────────────────────────────────────────

    private fun togglePopular(product: Product, markPopular: Boolean) {
        val label = if (markPopular) "Mark \"${product.name}\" as popular?" else "Remove \"${product.name}\" from popular?"
        AlertDialog.Builder(requireContext())
            .setTitle(if (markPopular) "Mark as Popular ⭐" else "Remove from Popular")
            .setMessage(label)
            .setPositiveButton(if (markPopular) "Mark Popular" else "Remove") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Repository.setProductPopular(product.id, markPopular)
                        .onSuccess {
                            val msg = if (markPopular) "\"${product.name}\" marked as popular ⭐"
                            else "\"${product.name}\" removed from popular"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            loadProducts()          // refresh list so star updates
                        }
                        .onFailure {
                            Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Delete \"${product.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Repository.deleteProduct(product.id)
                        .onSuccess { Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show(); loadProducts() }
                        .onFailure { Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}