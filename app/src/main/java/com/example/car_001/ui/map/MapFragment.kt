package com.example.car_001.ui.map

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.car_001.databinding.FragmentMapBinding

class MapFragment : Fragment(), OnMapReadyCallback {
    
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    
    private var googleMap: GoogleMap? = null
    private var crashLocation: LatLng? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get crash location from arguments
        arguments?.let { args ->
            val latitude = args.getDouble("latitude", 0.0)
            val longitude = args.getDouble("longitude", 0.0)
            if (latitude != 0.0 && longitude != 0.0) {
                crashLocation = LatLng(latitude, longitude)
            }
        }
        
        // Setup map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        setupClickListeners()
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        crashLocation?.let { location ->
            // Add marker for crash location
            val markerOptions = MarkerOptions()
                .position(location)
                .title("🚨 Crash Location")
                .snippet("Emergency response needed")
            
            map.addMarker(markerOptions)
            
            // Move camera to crash location
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } ?: run {
            // Default location if no crash location provided
            val defaultLocation = LatLng(13.0827, 80.2707) // Chennai coordinates
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
        }
    }
    
    private fun setupClickListeners() {
        binding.directionsButton.setOnClickListener {
            crashLocation?.let { location ->
                openDirections(location)
            } ?: run {
                Toast.makeText(requireContext(), "No crash location available", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.shareLocationButton.setOnClickListener {
            crashLocation?.let { location ->
                shareLocation(location)
            } ?: run {
                Toast.makeText(requireContext(), "No crash location available", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openDirections(location: LatLng) {
        try {
            val uri = Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to web browser
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open directions", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareLocation(location: LatLng) {
        try {
            val locationText = "🚨 CRASH ALERT!\nLocation: ${location.latitude}, ${location.longitude}\n" +
                    "Google Maps: https://www.google.com/maps?q=${location.latitude},${location.longitude}"
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, locationText)
                putExtra(Intent.EXTRA_SUBJECT, "Emergency Crash Alert")
            }
            
            startActivity(Intent.createChooser(intent, "Share Crash Location"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not share location", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 