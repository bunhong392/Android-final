package com.shopapp.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentStaffProfileBinding
import kotlinx.coroutines.launch

class StaffProfileFragment : Fragment() {

    private var _binding: FragmentStaffProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? StaffActivity)?.tvPageTitle?.text = "Profile"

        // Load staff info from Firebase
        viewLifecycleOwner.lifecycleScope.launch {
            val user = Repository.getCurrentUser()
            if (user != null) {
                // Avatar initial — first letter of name
                val initial = user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                binding.tvAvatar.text       = initial
                binding.tvName.text         = user.name.ifEmpty { "Staff" }
                binding.tvEmail.text        = user.email.ifEmpty { "—" }
                binding.tvPhone.text        = user.phone.ifEmpty { "—" }
                binding.tvStore.text        = user.store.ifEmpty { "—" }
                binding.tvEmployeeCode.text = user.employeeCode.ifEmpty { "—" }
            }
        }

        // Logout with confirmation dialog
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    (activity as? StaffActivity)?.logout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
