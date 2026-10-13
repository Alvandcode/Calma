# Permissions Reference

Calma requests the minimum permissions required for its features. Every permission is optional and can be denied without breaking core functionality.

| Permission | Protection Level | Purpose | User-Facing Explanation |
|------------|------------------|---------|--------------------------|
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Show focus timer countdown and session-completion notifications; show ambient-audio playback notification. | "Shows a timer notification while a focus session runs, and tells you when a session ends. Denying this does not stop the timer itself." |
| `VIBRATE` | Normal | Gentle haptic pulse at each breathing phase change. | "A short, soft vibration marks each breathing phase change." |
| `FOREGROUND_SERVICE` | Normal (API 9+) | Required to keep ambient audio playing while Calma is not in the foreground (Android 9+ enforcement). | "Allows ambient sounds to keep playing while you use another app. Android requires this to be shown as an ongoing notification." |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Runtime (API 34+) | Explicit foreground-service type for media playback (required on Android 14+). | Same as above; Android 14+ requires an explicit type. |
| `WAKE_LOCK` | Normal | Prevents screen from turning off during an active focus or breathing session started in the app. | "Prevents the screen from turning off during a focus or breathing session that you started in the app." |

## Permissions NOT Requested

| Permission | Why Not |
|------------|---------|
| `INTERNET` | Calma is fully offline; no network calls, no analytics, no ads. |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | No location-based features. |
| `CAMERA` | No camera functionality. |
| `RECORD_AUDIO` | No recording; only playback of bundled assets. |
| `READ_CONTACTS` / `WRITE_CONTACTS` | No contact access. |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | All data stored in app-private storage; no user-facing file ops. |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | No Bluetooth features. |
| `RECEIVE_BOOT_COMPLETED` | No background jobs; timer state restored on app launch. |

## Runtime Request Flow

1. User enables a feature requiring a permission (e.g., toggles "Notifications" in Settings).
2. App checks `ContextCompat.checkSelfPermission`.
3. If denied, shows a rationale dialog with "Allow" / "Don't allow".
4. On "Allow", calls `ActivityCompat.requestPermissions`.
5. Result handled in `MainActivity.onRequestPermissionsResult` → updates DataStore.

Users can also grant/deny via system Settings at any time. Calma gracefully degrades: denied notification permission = no notification, but timer still runs; denied vibration = no haptics, but breathing ring still animates.

## Manifest Declaration

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

No permission groups, no dynamic permission registration.