package com.example.car_001.mqtt

import android.content.Context
import android.util.Log
import com.example.car_001.data.CrashAlert
import com.example.car_001.data.CrashAlertEntity
import com.example.car_001.data.AppDatabase
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*

class MqttManager(private val context: Context) {
    
    private var mqttClient: MqttAndroidClient? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database = AppDatabase.getDatabase(context)
    
    var onMessageReceived: ((CrashAlertEntity) -> Unit)? = null
    var onConnectionStatusChanged: ((ConnectionStatus) -> Unit)? = null
    
    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }
    
    private var currentStatus = ConnectionStatus.DISCONNECTED
    
    fun connect(serverUri: String, username: String = "", password: String = "") {
        scope.launch {
            try {
                updateConnectionStatus(ConnectionStatus.CONNECTING)
                
                mqttClient = MqttAndroidClient(context, serverUri, MqttAndroidClient.AUTO_GENERATE_CLIENT_ID)
                
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    if (username.isNotEmpty()) {
                        userName = username
                        password = password.toCharArray()
                    }
                    connectionTimeout = 30
                    keepAliveInterval = 60
                }
                
                mqttClient?.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        updateConnectionStatus(ConnectionStatus.CONNECTED)
                        Log.d(TAG, "MQTT Connected successfully")
                    }
                    
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(TAG, "MQTT Connection failed", exception)
                        updateConnectionStatus(ConnectionStatus.ERROR)
                    }
                })
                
            } catch (e: Exception) {
                Log.e(TAG, "MQTT Connection error", e)
                updateConnectionStatus(ConnectionStatus.ERROR)
            }
        }
    }
    
    fun disconnect() {
        scope.launch {
            try {
                mqttClient?.disconnect()
                mqttClient = null
                updateConnectionStatus(ConnectionStatus.DISCONNECTED)
            } catch (e: Exception) {
                Log.e(TAG, "MQTT Disconnect error", e)
            }
        }
    }
    
    fun publishCrashAlert(crashAlert: CrashAlert) {
        scope.launch {
            try {
                if (currentStatus == ConnectionStatus.CONNECTED) {
                    val payload = gson.toJson(crashAlert)
                    val message = MqttMessage(payload.toByteArray())
                    message.qos = 1
                    
                    mqttClient?.publish(TOPIC_CRASH_ALERT, message)
                    Log.d(TAG, "Crash alert published: $payload")
                } else {
                    Log.w(TAG, "Cannot publish - MQTT not connected")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error publishing crash alert", e)
            }
        }
    }
    
    fun subscribeToCrashAlerts() {
        scope.launch {
            try {
                if (currentStatus == ConnectionStatus.CONNECTED) {
                    mqttClient?.subscribe(TOPIC_CRASH_ALERT, 1, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Subscribed to crash alerts")
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to subscribe to crash alerts", exception)
                        }
                    })
                    
                    // Set message callback
                    mqttClient?.setCallback(object : MqttCallbackExtended {
                        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                            Log.d(TAG, "MQTT Connect complete, reconnect: $reconnect")
                        }
                        
                        override fun connectionLost(cause: Throwable?) {
                            Log.w(TAG, "MQTT Connection lost", cause)
                            updateConnectionStatus(ConnectionStatus.ERROR)
                        }
                        
                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            Log.d(TAG, "Message arrived on topic: $topic")
                            message?.let { handleIncomingMessage(it) }
                        }
                        
                        override fun deliveryComplete(token: IMqttDeliveryToken?) {
                            Log.d(TAG, "Message delivery complete")
                        }
                    })
                    
                } else {
                    Log.w(TAG, "Cannot subscribe - MQTT not connected")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to crash alerts", e)
            }
        }
    }
    
    private fun handleIncomingMessage(message: MqttMessage) {
        scope.launch {
            try {
                val payload = String(message.payload)
                Log.d(TAG, "Received message: $payload")
                
                val crashAlert = gson.fromJson(payload, CrashAlert::class.java)
                
                // Convert to database entity
                val alertEntity = CrashAlertEntity(
                    latitude = crashAlert.latitude,
                    longitude = crashAlert.longitude,
                    crashTime = crashAlert.time,
                    crashDate = crashAlert.date,
                    bloodType = crashAlert.bloodType,
                    contact = crashAlert.contact,
                    status = "new"
                )
                
                // Save to database
                database.crashAlertDao().insertAlert(alertEntity)
                
                // Notify UI
                onMessageReceived?.invoke(alertEntity)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming message", e)
            }
        }
    }
    
    private fun updateConnectionStatus(status: ConnectionStatus) {
        currentStatus = status
        onConnectionStatusChanged?.invoke(status)
    }
    
    fun isConnected(): Boolean = currentStatus == ConnectionStatus.CONNECTED
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
    
    companion object {
        private const val TAG = "MqttManager"
        const val TOPIC_CRASH_ALERT = "car/crash_alert"
    }
} 