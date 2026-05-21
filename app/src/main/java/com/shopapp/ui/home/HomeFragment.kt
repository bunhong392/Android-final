package com.shopapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.shopapp.R
import com.shopapp.data.model.Product
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

// ── Simple data class for promo banners ──────────────────────────────────────
data class PromoBanner(
    val label: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val bgColor: Int          // resolved color int
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Main grid adapter (all products / filtered)
    private val adapter = ProductAdapter { product -> openDetail(product) }

    // Horizontal popular strip
    private val popularAdapter = ProductAdapter { product -> openDetail(product) }

    private var allProducts = listOf<Product>()
    private var selectedChip: Chip? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, state: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPromoBanners()
        setupPopularStrip()
        setupProductGrid()
        setupChips()
        setupSearch()

        binding.swipeRefresh.setOnRefreshListener { loadProducts() }
        loadProducts()
    }

    // ── Promo Banners Carousel ────────────────────────────────────────────────

    private fun setupPromoBanners() {
        val banners = listOf(
            PromoBanner(
                label    = "panda party ✦",
                title    = "Up to\n50% off*",
                subtitle = "*T&Cs apply.",
                emoji    = "🍕",
                bgColor  = 0xFFE91E8C.toInt()
            ),
            PromoBanner(
                label    = "13 May 🗓️",
                title    = "Enjoy\n50% off*",
                subtitle = "*T&Cs apply.",
                emoji    = "🧋",
                bgColor  = 0xFFD4A84B.toInt()
            ),
            PromoBanner(
                label    = "Flash Deal ⚡",
                title    = "Up to\n60% off*",
                subtitle = "*T&Cs apply.",
                emoji    = "🎉",
                bgColor  = 0xFF4F46E5.toInt()
            )
        )

        val bannerAdapter = PromoBannerAdapter(banners)
        binding.rvPromoBanners.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = bannerAdapter
        }
    }

    // ── Popular Horizontal Strip ──────────────────────────────────────────────

    private fun setupPopularStrip() {
        binding.rvPopular.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = popularAdapter
        }

        binding.btnSeeAll.setOnClickListener {
            binding.rvProducts.smoothScrollToPosition(0)
        }
    }

    // ── Main Product Grid ─────────────────────────────────────────────────────

    private fun setupProductGrid() {
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter        = this@HomeFragment.adapter
            isNestedScrollingEnabled = false
        }
    }

    // ── Category Chips ────────────────────────────────────────────────────────

    private fun setupChips() {
        val chips = listOf(
            Pair(binding.chipAll,         ""),
            Pair(binding.chipFood,        "Food"),
            Pair(binding.chipDrinks,      "Drinks"),
            Pair(binding.chipElectronics, "Electronics"),
            Pair(binding.chipClothing,    "Clothing")
        )

        chips.forEach { (chip, category) ->
            chip.setOnClickListener {
                selectChip(chip, chips)
                filterByCategory(category)
            }
        }
        selectChip(binding.chipAll, chips)
    }

    private fun selectChip(chip: Chip, all: List<Pair<Chip, String>>) {
        all.forEach { (c, _) -> c.isChecked = false; c.isSelected = false }
        chip.isChecked  = true
        chip.isSelected = true
        selectedChip = chip
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                adapter.submitList(allProducts.filter { it.name.lowercase().contains(query) })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private fun loadProducts(category: String = "") {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {

            // Load main product list
            val result = Repository.getProducts(category)
            binding.swipeRefresh.isRefreshing = false
            result.onSuccess { products ->
                allProducts = products
                adapter.submitList(products)
                binding.tvEmpty.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show()
            }

            // Load popular strip from DB (admin-curated, falls back to first 6 if none set)
            Repository.getPopularProducts().onSuccess { popular ->
                popularAdapter.submitList(popular)
                // Show/hide the entire popular section based on whether there's content
                val hasPopular = popular.isNotEmpty()
                binding.rvPopular.visibility   = if (hasPopular) View.VISIBLE else View.GONE
                binding.btnSeeAll.visibility   = if (hasPopular) View.VISIBLE else View.GONE
            }
        }
    }

    private fun filterByCategory(cat: String) = loadProducts(cat)

    private fun openDetail(product: Product) {
        startActivity(
            Intent(requireContext(), ProductDetailActivity::class.java)
                .putExtra("product_id", product.id)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


// ══════════════════════════════════════════════════════════════════════════════
//  PromoBannerAdapter
// ══════════════════════════════════════════════════════════════════════════════

class PromoBannerAdapter(
    private val items: List<PromoBanner>
) : RecyclerView.Adapter<PromoBannerAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bannerBg = itemView.findViewById<View>(R.id.bannerBg)
        val tvLabel  = itemView.findViewById<android.widget.TextView>(R.id.tvBannerLabel)
        val tvTitle  = itemView.findViewById<android.widget.TextView>(R.id.tvBannerTitle)
        val tvSub    = itemView.findViewById<android.widget.TextView>(R.id.tvBannerSub)
        val tvEmoji  = itemView.findViewById<android.widget.TextView>(R.id.tvBannerEmoji)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promo_banner, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.bannerBg.setBackgroundColor(item.bgColor)
        holder.tvLabel.text = item.label
        holder.tvTitle.text = item.title
        holder.tvSub.text   = item.subtitle
        holder.tvEmoji.text = item.emoji
    }

    override fun getItemCount() = items.size
}