# Privacy Policy

**Effective date:** 2024-10-12

Calma is designed with privacy as a foundational principle. This policy explains what data the app handles and how it is protected.

## Data We Collect

**Calma collects zero personal data.** The app does not transmit any information to any server, third party, or analytics service. All data remains on your device.

### Local Data Only

| Data Type | Purpose | Retention |
|-----------|---------|-----------|
| Focus sessions (duration, timestamps) | Statistics, streak calculation | Until deleted by user |
| Breathing exercises (pattern, duration, cycles) | Statistics | Until deleted by user |
| Calm exercises (type, duration, steps) | Statistics | Until deleted by user |
| Daily check-ins (mood, stress, energy, focus, optional note) | Trends, history | Until deleted by user |
| Sound mixer presets (volumes, sleep timer) | User preference | Until deleted by user |
| App preferences (language, theme, notifications) | App behavior | Until deleted by user |

### What We Do NOT Collect

- No personal identifiers (name, email, phone, contacts)
- No location data
- No device identifiers (advertising ID, Android ID)
- No usage analytics, crash reports, or telemetry
- No network requests of any kind (the app has no `INTERNET` permission)

## Data Storage & Security

- All data is stored in the app's private internal storage (Room database, DataStore preferences).
- Data is not backed up to Google Drive or transferred between devices (`android:allowBackup="false"`).
- Data is encrypted at rest by Android's filesystem encryption.
- Deleting the app removes all data permanently.

## User Control

You have full control over your data:

- **View** all entries in the app's screens.
- **Edit** any check-in from the Mood screen.
- **Delete** individual sessions or check-ins.
- **Erase everything** via Settings → "Erase all local data" (irreversible).
- **Uninstall** the app to remove everything.

## Children's Privacy

Calma does not knowingly collect data from children under 13. Since no data is collected at all, COPPA/GDPR-K concerns do not apply.

## Third Parties

Calma includes no third-party SDKs, trackers, analytics, or advertising libraries. The only dependencies are AndroidX libraries and Kotlin stdlib, all of which run locally.

## Changes to This Policy

Any updates will be posted in this repository with a new effective date. Continued use of the app constitutes acceptance.

## Contact

For privacy questions, open an issue at the [GitHub repository](https://github.com/calma-app/calma).