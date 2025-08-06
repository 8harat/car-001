# Car Crash Detection System

A comprehensive Android application that serves as a bridge between ESP32 hardware and MQTT communication for real-time car crash detection and emergency response.

## 🚗 Features

### Publisher Mode
- **Bluetooth ESP32 Connection**: Automatically scan and connect to ESP32 devices
- **Real-time Data Display**: Monitor sensor readings (acceleration, vibration, GPS)
- **Crash Detection**: Automatic detection and MQTT publishing of crash alerts
- **Test Alerts**: Manual crash alert testing functionality
- **MQTT Integration**: Publish crash alerts to MQTT broker

### Subscriber Mode
- **Emergency Alerts**: Receive and display crash alerts in real-time
- **Alert History**: View all previous crash alerts with timestamps
- **Map Integration**: View crash locations on Google Maps
- **Emergency Actions**: Call emergency contacts and get directions
- **Alert Management**: Mark alerts as handled

### Settings
- **MQTT Configuration**: Configure broker IP, port, username, password
- **Personal Information**: Set blood type and emergency contact
- **Notification Settings**: Configure sound, vibration, and lock screen alerts
- **Connection Testing**: Test MQTT connectivity

## 🏗️ Architecture

### Core Components
- **Bluetooth Manager**: Handles ESP32 communication
- **MQTT Manager**: Manages MQTT publishing/subscribing
- **Room Database**: Local storage for settings and alerts
- **Navigation**: Fragment-based UI navigation
- **Google Maps**: Location display and navigation

### Data Flow
```
ESP32 → Bluetooth → Publisher App → MQTT Broker → Subscriber App → Emergency Response
```

## 📱 Screenshots

### Main Menu
- Publisher Mode selection
- Subscriber Mode selection  
- Settings access

### Publisher Mode
- Connection status indicators
- Real-time sensor data display
- Bluetooth device selection
- Test crash alert button

### Subscriber Mode
- MQTT subscription status
- Crash alert display with emergency info
- Alert history list
- Map and call action buttons

### Settings
- MQTT broker configuration
- Personal information setup
- Notification preferences
- Connection testing

## 🛠️ Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (API level 24)
- Google Maps API Key
- MQTT Broker (e.g., Mosquitto, HiveMQ)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd car-001
   ```

2. **Add Google Maps API Key**
   - Get API key from [Google Cloud Console](https://console.cloud.google.com/)
   - Replace `YOUR_MAPS_API_KEY` in `AndroidManifest.xml`

3. **Configure MQTT Broker**
   - Set up MQTT broker (Mosquitto recommended)
   - Note the broker IP and port

4. **Build and Run**
   ```bash
   ./gradlew build
   ```
   - Open in Android Studio
   - Run on device or emulator

### ESP32 Setup

The ESP32 should send JSON data in this format:
```json
{
  "crash": false,
  "vibration": 0,
  "accel_x": 0.12,
  "accel_y": 0.05,
  "accel_z": 9.81,
  "gyro_x": 0.01,
  "gyro_y": -0.02,
  "gyro_z": 0.00,
  "gps_lat": 13.0827,
  "gps_lon": 80.2707,
  "timestamp": "14:23:45"
}
```

## 🔧 Configuration

### MQTT Settings
- **Broker IP**: Your MQTT broker IP address
- **Port**: Usually 1883 (default) or 8883 (SSL)
- **Topic**: `car/crash_alert` (fixed)
- **QoS**: 1 (At least once delivery)

### Bluetooth Settings
- **Device Name**: ESP32 devices are auto-detected
- **Service UUID**: `00001101-0000-1000-8000-00805F9B34FB`
- **Connection**: SPP (Serial Port Profile)

### Personal Information
- **Blood Type**: For emergency responders
- **Emergency Contact**: Phone number for emergency calls

## 📊 Usage Guide

### Publisher Mode (ESP32 Connection)
1. Open the app and select "Publisher Mode"
2. Select your ESP32 device from the dropdown
3. Click "Connect" to establish Bluetooth connection
4. Monitor real-time sensor data
5. Use "Test Crash Alert" to simulate crash detection
6. Configure MQTT settings if needed

### Subscriber Mode (Emergency Response)
1. Open the app and select "Subscriber Mode"
2. Configure MQTT settings in Settings
3. Wait for crash alerts to arrive
4. View alert details and location
5. Use "View on Map" to see crash location
6. Call emergency contacts or get directions
7. Mark alerts as handled when resolved

### Settings Configuration
1. Navigate to Settings
2. Enter MQTT broker details
3. Set personal information
4. Configure notification preferences
5. Test MQTT connection
6. Save settings

## 🔒 Permissions

The app requires the following permissions:
- **Bluetooth**: Connect to ESP32 devices
- **Location**: GPS coordinates for crash location
- **Internet**: MQTT communication
- **Phone**: Emergency calls
- **Vibrate**: Alert notifications

## 🧪 Testing

### Publisher Testing
1. Connect ESP32 with crash detection sensors
2. Simulate crash conditions
3. Verify MQTT message publishing
4. Test Bluetooth reconnection

### Subscriber Testing
1. Set up multiple subscriber devices
2. Send test crash alerts
3. Verify alert reception and display
4. Test emergency actions (call, map)

### Integration Testing
1. End-to-end crash detection flow
2. Multiple subscriber scenarios
3. Network interruption handling
4. Background operation testing

## 🐛 Troubleshooting

### Common Issues

**Bluetooth Connection Failed**
- Ensure ESP32 is discoverable
- Check Bluetooth permissions
- Verify device pairing

**MQTT Connection Failed**
- Verify broker IP and port
- Check network connectivity
- Test with MQTT client tools

**Maps Not Loading**
- Verify Google Maps API key
- Check internet connection
- Ensure location permissions

**Crash Alerts Not Received**
- Verify MQTT subscription
- Check topic name (`car/crash_alert`)
- Test broker connectivity

## 📈 Future Enhancements

- **Background Service**: Continuous monitoring when app is closed
- **Push Notifications**: Firebase Cloud Messaging integration
- **Offline Mode**: Local storage for offline operation
- **Multiple ESP32 Support**: Connect to multiple vehicles
- **Advanced Analytics**: Crash pattern analysis
- **Emergency Services Integration**: Direct API integration

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Make changes and test thoroughly
4. Submit pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions:
- Create an issue on GitHub
- Contact the development team
- Check the troubleshooting section

---

**Note**: This application is designed for emergency response scenarios. Always test thoroughly before deployment in production environments. 