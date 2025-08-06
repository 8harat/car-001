package com.example.car_001.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// ESP32 Sensor Data Model
data class ESP32Data(
    @SerializedName("crash") val crash: Boolean = false,
    @SerializedName("vibration") val vibration: Int = 0,
    @SerializedName("accel_x") val accelX: Double = 0.0,
    @SerializedName("accel_y") val accelY: Double = 0.0,
    @SerializedName("accel_z") val accelZ: Double = 0.0,
    @SerializedName("gyro_x") val gyroX: Double = 0.0,
    @SerializedName("gyro_y") val gyroY: Double = 0.0,
    @SerializedName("gyro_z") val gyroZ: Double = 0.0,
    @SerializedName("gps_lat") val gpsLat: Double = 0.0,
    @SerializedName("gps_lon") val gpsLon: Double = 0.0,
    @SerializedName("timestamp") val timestamp: String = ""
) {
    val acceleration: Double
        get() = kotlin.math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
}

// Crash Alert Model for MQTT
data class CrashAlert(
    @SerializedName("crash") val crash: Boolean = true,
    @SerializedName("lat") val latitude: Double = 0.0,
    @SerializedName("lon") val longitude: Double = 0.0,
    @SerializedName("time") val time: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("blood") val bloodType: String = "",
    @SerializedName("contact") val contact: String = "",
    @SerializedName("device_id") val deviceId: String = "ESP32_CAR_01"
)

// Database Entity for Settings
@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val mqttIp: String = "192.168.1.100",
    val mqttPort: Int = 1883,
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val bloodType: String = "B+",
    val emergencyContact: String = "+91-9876543210"
)

// Database Entity for Crash Alerts
@Entity(tableName = "crash_alerts")
data class CrashAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val crashTime: String = "",
    val crashDate: String = "",
    val bloodType: String = "",
    val contact: String = "",
    val status: String = "new", // new, acknowledged, resolved
    val receivedAt: Long = System.currentTimeMillis()
)

// Bluetooth Device Model
data class BluetoothDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false
)

// Connection Status Enum
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

// App Mode Enum
enum class AppMode {
    PUBLISHER,
    SUBSCRIBER
} 