package com.shopapp.ui.admin

import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentAdminSettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminSettingsFragment : Fragment() {

    private var _binding: FragmentAdminSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AdminActivity)?.tvPageTitle?.text = "Settings"

        loadAdminProfile()
        setupListeners()
    }
    private fun loadAdminProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Try loading as Admin first, fallback to User
            val user = Repository.getCurrentUser()
            user?.let {
                binding.tvAdminName.text    = it.name
                binding.tvAdminEmail.text   = it.email
                binding.tvCurrentRole.text  = it.role.replaceFirstChar { c -> c.uppercaseChar() }
                binding.tvAdminStore.text   = "Phnom Penh, KH"
                binding.tvEmployeeCode.text = it.uid.take(12).uppercase()
            }
        }
    }
    private fun setupListeners() {
        // Edit name dialog
        binding.btnEditName.setOnClickListener {
            showEditDialog("Edit Name", binding.tvAdminName.text.toString()) { newName ->
                binding.tvAdminName.text = newName
                Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show()
            }
        }

        // Role checkboxes (just UI demo — real role change would update Firestore)
        binding.checkManager.setOnCheckedChangeListener { _, checked ->
            if (checked) Toast.makeText(requireContext(), "Manager role enabled", Toast.LENGTH_SHORT).show()
        }
        binding.checkAdmin.setOnCheckedChangeListener { _, checked ->
            if (!checked) {
                // Prevent unchecking own admin role
                binding.checkAdmin.isChecked = true
                Toast.makeText(requireContext(), "Cannot remove own Admin role", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Clear Data buttons ────────────────────────────────────────────────
        binding.btnClearProducts.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("🗑️ Clear Products")
                .setMessage("Delete all products from the database? This cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = Repository.clearProductsOnly()
                        if (result.isSuccess) {
                            Toast.makeText(requireContext(), "✅ All products cleared", Toast.LENGTH_SHORT).show()
                        } else {
                            val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                            Toast.makeText(requireContext(), "❌ Failed: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnClearOrders.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("🗑️ Clear Orders")
                .setMessage("Delete all orders from the database? This cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = Repository.clearOrdersOnly()
                        if (result.isSuccess) {
                            Toast.makeText(requireContext(), "✅ All orders cleared", Toast.LENGTH_SHORT).show()
                        } else {
                            val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                            Toast.makeText(requireContext(), "❌ Failed: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

//        binding.btnClearUsers.setOnClickListener {
//            AlertDialog.Builder(requireContext())
//                .setTitle("🗑️ Clear All Users")
//                .setMessage("Delete all user accounts? You will be logged out and all users will need to register again.")
//                .setPositiveButton("Clear") { _, _ ->
//                    viewLifecycleOwner.lifecycleScope.launch {
//                        Repository.clearUsersOnly()
//                        Toast.makeText(requireContext(), "✅ All users cleared", Toast.LENGTH_SHORT).show()
//                        (activity as? AdminActivity)?.logout()
//                    }
//                }
//                .setNegativeButton("Cancel", null)
//                .show()
//        }
//
//        binding.btnClearAll.setOnClickListener {
//            AlertDialog.Builder(requireContext())
//                .setTitle("⚠️ Clear ALL Data")
//                .setMessage("This will delete ALL users, products, orders and cart items.\n\nThe app will reset completely. Are you absolutely sure?")
//                .setPositiveButton("YES, CLEAR ALL") { _, _ ->
//                    viewLifecycleOwner.lifecycleScope.launch {
//                        Repository.clearAllData()
//                        Toast.makeText(requireContext(), "✅ Database cleared", Toast.LENGTH_SHORT).show()
//                        (activity as? AdminActivity)?.logout()
//                    }
//                }
//                .setNegativeButton("Cancel", null)
//                .show()
//        }

        // Logout
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    (activity as? AdminActivity)?.logout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showEditDialog(title: String, currentValue: String, onSave: (String) -> Unit) {
        val input = TextInputEditText(requireContext()).apply {
            setText(currentValue)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newVal = input.text.toString().trim()
                if (newVal.isNotEmpty()) onSave(newVal)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}