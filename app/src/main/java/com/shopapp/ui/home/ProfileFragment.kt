package com.shopapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentProfileBinding
import com.shopapp.util.LocaleHelper
import com.shopapp.util.ThemeHelper
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user info
        viewLifecycleOwner.lifecycleScope.launch {
            val user = Repository.getCurrentUser()
            if (_binding == null) return@launch
            binding.tvName.text  = user?.name  ?: ""
            binding.tvEmail.text = user?.email ?: ""
            binding.tvPhone.text = user?.phone ?: ""
        }

        // Language buttons — highlight the active one
        val currentLang = LocaleHelper.getLanguage(requireContext())
        updateLangButtons(currentLang)
        binding.btnLangEn.setOnClickListener { switchLanguage("en") }
        binding.btnLangKm.setOnClickListener { switchLanguage("km") }

        // Dark / light mode toggle
        val isDark = ThemeHelper.isDarkMode(requireContext())
        binding.switchDarkMode.isChecked = isDark
        binding.tvDarkModeLabel.text = getString(
            if (isDark) com.shopapp.R.string.dark_mode else com.shopapp.R.string.light_mode
        )
        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            ThemeHelper.setDarkMode(requireContext(), checked)
            binding.tvDarkModeLabel.text = getString(
                if (checked) com.shopapp.R.string.dark_mode else com.shopapp.R.string.light_mode
            )
            // Recreate only MainActivity — no splash screen detour
            (activity as? MainActivity)?.recreate()
        }

        binding.btnLogout.setOnClickListener {
            (activity as? MainActivity)?.logout()
        }
    }

    // ── Language ──────────────────────────────────────────────────────────

    private fun switchLanguage(lang: String) {
        if (LocaleHelper.getLanguage(requireContext()) == lang) return
        LocaleHelper.setLocale(requireContext(), lang)
        // Recreate only MainActivity — no splash screen detour
        (activity as? MainActivity)?.recreate()
    }

    private fun updateLangButtons(lang: String) {
        val isEn = lang == "en"
        binding.btnLangEn.alpha = if (isEn) 1f else 0.50f
        binding.btnLangKm.alpha = if (!isEn) 1f else 0.50f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}