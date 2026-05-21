package com.shopapp.ui.admin

import androidx.lifecycle.lifecycleScope
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentAdminOrdersBinding
import com.shopapp.ui.staff.StaffActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminOrdersFragment : Fragment() {

    private var _binding: FragmentAdminOrdersBinding? = null
    private val binding get() = _binding!!

    private val adapter = AdminOrderAdapter { order, status ->
        viewLifecycleOwner.lifecycleScope.launch {
            Repository.updateOrderStatus(order.id, status)
                .onSuccess { Toast.makeText(requireContext(), "Status updated to $status", Toast.LENGTH_SHORT).show(); loadOrders() }
                .onFailure { Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAdminOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv = (activity as? AdminActivity)?.tvPageTitle
            ?: (activity as? StaffActivity)?.tvPageTitle
        tv?.text = "Orders"
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#534AB7"))
        binding.swipeRefresh.setOnRefreshListener { loadOrders() }
        loadOrders()
    }

    private fun loadOrders() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Repository.getAllOrders()
            binding.swipeRefresh.isRefreshing = false
            result.onSuccess { orders ->
                adapter.submitList(orders)
                binding.tvPendingCount.text    = orders.count { it.status == "pending" }.toString()
                binding.tvDeliveringCount.text = orders.count { it.status == "delivering" }.toString()
                binding.tvDeliveredCount.text  = orders.count { it.status == "delivered" }.toString()
                binding.tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                Toast.makeText(requireContext(), "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
