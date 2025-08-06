package com.example.car_001.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot completed, starting background services")
                // Start background MQTT service if needed
                // This can be implemented later for background crash monitoring
            }
        }
    }
    
    companion object {
        private const val TAG = "BootReceiver"
    }
} 