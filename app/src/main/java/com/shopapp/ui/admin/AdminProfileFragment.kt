package com.shopapp.ui.admin

import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentAdminProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminProfileFragment : Fragment() {
    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            val user = Repository.getCurrentUser()
            binding.tvName.text = user?.name ?: "Admin"
            binding.tvEmail.text = user?.email ?: ""
            binding.tvRole.text = "Role: ${user?.role?.uppercase()}"
        }

        binding.btnLogout.setOnClickListener {
            (activity as? AdminActivity)?.logout()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
