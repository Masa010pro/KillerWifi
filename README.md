# 📶 Killer WiFi

**Killer WiFi** is a modern Android application built with Jetpack Compose that provides real-time Wi-Fi network intelligence, signal quality history, speed benchmarking, and an on-device conversational AI assistant.

---

## 🌟 Key Features

### 📡 1. Live Telemetry & Signal Quality
- **Real-Time Signal Gauge**: Radial arc gauge displaying active signal strength percentage and dynamic quality ratings (*Excellent*, *Good*, *Fair*, *Weak*).
- **Frequency Band Auto-Detection**: Automatically identifies if you are connected on **2.4 GHz**, **5 GHz**, or **6 GHz** (Wi-Fi 6E/7).
- **Signal History Line Chart**: Real-time graph with smooth Bézier curves, gradient area fill, and active signal pulse indicators.

### 🌐 2. Access Point & Network Diagnostics
- **Detailed Network Breakdown**: Displays Router BSSID (MAC Address), Device IP, and Subnet mask.
- **Tap-to-Copy**: Quickly copy IP addresses or MAC addresses to the clipboard with visual toast feedback.

### 📊 3. Subnet IP Capacity
- **Subnet Visualizer**: Estimates allocated vs available IP address capacity based on interface prefix lengths (`/24`, etc.).

### ⚡ 4. Internet Speed Benchmark
- **Latency & Speed Measurement**: Measures Ping (ms), Download speed (Mbps), and Upload speed (Mbps) via Cloudflare & Google endpoints.

### ✨ 5. Conversational AI Assistant
- **Floating Action Button (`✨ Ask AI`)**: Opens a modal bottom sheet with a full chat thread.
- **Multi-Turn Chat Thread**: Maintains persistent chat history with user bubbles and AI responses.
- **Context-Aware Telemetry Reasoning**: Understands active network telemetry and prior conversation turns to answer questions about online gaming latency, 4K streaming capability, Wi-Fi band advantages, or network health.
- **Quick Action Chips**: One-tap queries for common diagnostic summaries.

### 🎨 6. Material 3 & Custom Themes
- **Adaptive Theme Engine**: Supports System, Light, and Dark modes with slate blue color schemes.

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Concurrency**: Kotlin Coroutines & Flow
- **Network I/O**: `HttpURLConnection` asynchronous measurement
- **Min SDK**: 24 (Android 7.0+)
- **Compile / Target SDK**: 37

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Ladybug / Koala or newer recommended)
- JDK 17 or JDK 11
- Android SDK 37

### Building the Project

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Masa010pro/KillerWifi.git
   cd KillerWifi
   ```

2. **Open in Android Studio** and sync Gradle dependencies.

3. **Build and Run**:
   - Run via Android Studio onto an Android device or emulator running Android 7.0 (API 24) or higher.
   - Or build from CLI:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
