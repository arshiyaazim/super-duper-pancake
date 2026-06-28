# SMS Gateway (SMS Manager)

An Android application that acts as an **SMS gateway** — intercepting incoming SMS messages matching a specific pattern, forwarding them to a backend API, and sending reply SMS messages based on the API response.

## Overview

SMS Gateway listens for incoming SMS messages that start with `ID` (case-insensitive and flexible spacing), forwards the message content to a configured backend server via HTTP POST, parses the API response, and sends an automated SMS reply back to the specified phone number.

**Package:** `com.locationwhere.smsgateway`
**Min SDK:** 26 (Android 8.0 Oreo)
**Target SDK:** 34 (Android 14)

## Features

- **Flexible SMS Interception** — Detects `ID:`, `ID :`, `I D :`, etc., via regex.
- **API Forwarding** — Forwards SMS content to a configurable backend endpoint.
- **Auto-Reply SMS** — Sends reply SMS based on API response data.
- **Foreground Service** — Persistent service with notification for reliability.
- **Boot Auto-Start** — Automatically restarts the gateway service after device reboot.
- **Encrypted Settings** — Uses `EncryptedSharedPreferences` (AES-256) for storing secrets.
- **Activity Logging** — Room database stores last 100 log entries with status tracking.
- **Retry Mechanism** — Automatically retries failed API calls with exponential backoff.
- **Connection Test** — Test backend connectivity from the settings screen.

## Setup & Configuration

To ensure error-free operation after installation, follow these steps:

### 1. App Permissions
Upon first launch, the app will request several permissions. For the gateway to work correctly, you **must grant**:
- **SMS Permissions**: To receive, read, and send SMS messages.
- **Phone State**: To identify the device/SIM if needed.
- **Notifications**: Required for the foreground service (Android 13+).

### 2. Battery Optimization (Crucial)
Android's battery-saving features can kill background services. 
- When prompted, allow the app to **Ignore Battery Optimizations**.
- If not prompted, go to *Settings > Apps > SMS Manager > Battery* and select **Unrestricted**.

### 3. App Settings
Navigate to the Settings screen (Floating Action Button on main screen):
- **Backend URL**: The full base URL of your server (e.g., `https://your-api.com`).
- **Gateway Secret**: A secure string used to authenticate requests to your backend.
- **Admin Number**: (Optional) A default number for system-level notifications.
- **Enable Forwarding**: Ensure this toggle is **ON**.

Use the **"Test Connection"** button to verify that the app can reach your server before exiting settings.

### 4. Production Local Secret File (Recommended)
Create `local.properties` in the project root to prefill production values at build time:

SMSGATEWAY_BACKEND_URL=http://5.189.131.48:3000
SMSGATEWAY_GATEWAY_SECRET=your_gateway_secret
SMSGATEWAY_ADMIN_NUMBER=01958122300

If these values are present, the settings screen auto-loads them on first launch.

---

## Backend Server Configuration

Your server must implement two specific endpoints to communicate with this app.

### 1. SMS Forwarding Endpoint
**POST** `{Backend URL}/api/v1/gateway/sms`

**Request Body (JSON):**
```json
{
  "secret": "your-gateway-secret",
  "from": "+8801XXXXXXXXX",
  "message": "ID: 01712345678 Fazle Azim"
}
```

**Expected Response (JSON):**
- **Success:**
  ```json
  {
    "status": "success",
    "employeeCode": "EMP001",
    "replyTo": "+8801XXXXXXXXX",
    "replyMessage": "Attendance recorded for Fazle Azim"
  }
  ```
- **Duplicate:**
  ```json
  { "status": "duplicate" }
  ```

### 2. Connection Test Endpoint
**POST** `{Backend URL}/api/v1/gateway/test`

**Request Body (JSON):**
```json
{ "secret": "your-gateway-secret" }
```
**Expected Response:** `200 OK`

---

## SMS Format
The app is configured to be highly flexible with incoming message formats. Any message starting with `ID` (case-insensitive) followed by any combination of spaces or a colon will be intercepted.

**Examples of Valid Formats:**
- `ID: 01958122300 Azim`
- `ID : Azim 01958122300`
- `I D : 01958122300 Azim`
- `id:SomeValue`

---

## Tech Stack

 Component       | Technology                      |
:----------------|:--------------------------------|
 Language         | Kotlin 2.0.0                    |
 UI               | XML Layouts + ViewBinding       |
 Networking       | OkHttp 4.12.0                   |
 Database         | Room 2.6.1 (KSP)               |
 Security         | EncryptedSharedPreferences      |
 Concurrency      | Kotlin Coroutines               |

## Building

1. Open the project in Android Studio.
2. Sync Gradle.
3. Build → Make Project (or `./gradlew assembleDebug`).

## License
*(No license specified)*
