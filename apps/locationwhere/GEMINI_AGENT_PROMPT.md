# Gemini Agent Prompt: Complete the "Location_Where" Employee Monitoring App

## Project Overview
This is an **Employee Monitoring Android App** (Kotlin, Hilt, Room, Retrofit) with a **Node.js/Express backend** (Prisma, PostgreSQL, Redis, Firebase FCM, AWS S3). The app tracks employee location, detects SIM changes, logs calls, enforces geofences, and supports remote device commands (lock/wipe/siren).

## What's Already Built

### Android App (package: `com.example.location_where`)
- **Auth flow:** SplashActivity → LoginActivity → MainActivity (employee code + password login via `/api/v1/auth/mobile/login`)
- **TokenManager** (DataStore-based) with AuthInterceptor for JWT refresh
- **LocationService** (foreground service): GPS tracking every 30s (60s on low battery), geofence registration from server, battery-adaptive intervals
- **GeofenceBroadcastReceiver**: Reports EXIT breaches to backend, shows local notification
- **SimCheckWorker**: Periodic (1hr) SIM ICCID comparison, alerts on change (Hilt-injected)
- **LocationSyncWorker**: Periodic sync of locally-cached locations (Room DB) when network available
- **CallLogWorker**: Periodic sync of device call logs to `/api/v1/calls/log`
- **CallRecordingService**: Monitors call states and records audio, encrypts with AES-256 before upload
- **CommandWorker**: Polls for remote commands (`LOCK`, `WIPE`, `SIREN`, `MESSAGE`)
- **MonitoringFcmService**: Receives real-time remote commands via FCM
- **ConsentActivity**: Legal consent flow with backend confirmation
- **Device Registration**: Automatically sends device info (FCM token, model, etc.) to backend
- **Anti-Tamper**: Root detection (RootBeer) and Device Admin integration
- **Room DB**: LocationEntity/Dao/Database for offline location caching
- **Hilt DI**: Full dependency injection setup
- **Java 17 / Kotlin 2.0**: Modern build configuration

## What Needs to Be Built (Remaining Work)

### Android App — HIGH PRIORITY

1. **Proper Dashboard UI (MainActivity)**
   - Show real-time status: GPS (Active/Inactive), Battery Level, SIM Status, Last Sync Time
   - Add status indicators (Green/Red) for monitoring services
   - The current UI is functional but needs better visual feedback

2. **Network State Awareness / Manual Queueing**
   - Implement a manual queue for direct API calls in `LocationService` (e.g., geofence breaches) when offline
   - Ensure these "immediate" events are flushed as soon as network returns, even before the next periodic sync

3. **Anti-Tamper Enhancements**
   - Implement `AlarmManager` heartbeat to detect if the app has been force-stopped for a long time
   - Add more robust root detection (detecting Magisk/Zygisk)
   - Send "App Offline" alert to backend if the heartbeat stops

4. **Self-Monitoring / Health Check**
   - Periodically check if `LocationService` and `CallRecordingService` are actually running and restart them if needed (within OS limits)

### Backend — HIGH PRIORITY (To be deployed/verified)

1. **Admin Web Dashboard API refinements**
   - Ensure all endpoints for live tracking and report generation are fully functional
   - WebSocket/SSE for real-time dashboard updates

2. **S3 Storage Verification**
   - Verify that call recordings are correctly uploaded and accessible only via admin pannel

3. **FCM Sending Logic**
   - Ensure the backend correctly triggers push notifications when an admin issues a command

### Build & Configuration Issues to Fix

1. **Kotlin plugin is commented out** in `app/build.gradle.kts` — uncomment `id("org.jetbrains.kotlin.android")`
2. **kotlinOptions block is missing** — add `kotlinOptions { jvmTarget = "11" }`
3. **Hardcoded admin password** in MainActivity (`"admin123"`) — move to server-side verification
4. **No ProGuard/R8 rules** for Retrofit, Gson, Hilt — app will crash in release build
5. **BASE_URL not configured** in RetrofitClient — needs to point to actual backend

## Architecture Notes
- Package: `com.example.location_where`
- DI: Dagger Hilt (`@AndroidEntryPoint`, `@HiltWorker`)
- Local DB: Room (LocationEntity with lat, lng, accuracy, battery, timestamp, synced flag)
- Network: Retrofit2 + OkHttp + Gson + AuthInterceptor (auto token refresh)
- Background: WorkManager for periodic tasks, foreground Service for continuous GPS
- Min SDK: 26, Target SDK: 35

## Priority Order for Implementation
1. Fix build issues (Kotlin plugin, kotlinOptions)
2. Fix SimCheckWorker (Hilt injection, real token)
3. Call log monitoring worker
4. Remote command execution worker + FCM
5. Device info registration
6. Anti-tamper improvements
7. Consent screen
8. Dashboard UI
9. Backend: FCM push, geofence CRUD, call recording upload, heartbeat detection
10. Backend: WebSocket for real-time, report generation
