package com.shopapp.ui.home

import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentOrdersBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private val adapter = OrdersAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { loadOrders() }
        loadOrders()
    }

    private fun loadOrders() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Repository.getMyOrders()
            binding.swipeRefresh.isRefreshing = false
            result.onSuccess { orders ->
                adapter.submitList(orders)
                binding.tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                Toast.makeText(requireContext(), "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
