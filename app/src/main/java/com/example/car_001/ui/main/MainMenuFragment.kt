package com.example.car_001.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.car_001.databinding.FragmentMainMenuBinding

class MainMenuFragment : Fragment() {
    
    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        // Publisher Mode
        binding.publisherCard.setOnClickListener {
            findNavController().navigate(
                com.example.car_001.R.id.action_mainMenuFragment_to_publisherFragment
            )
        }
        
        // Subscriber Mode
        binding.subscriberCard.setOnClickListener {
            findNavController().navigate(
                com.example.car_001.R.id.action_mainMenuFragment_to_subscriberFragment
            )
        }
        
        // Settings
        binding.settingsCard.setOnClickListener {
            findNavController().navigate(
                com.example.car_001.R.id.action_mainMenuFragment_to_settingsFragment
            )
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 