# Privacy Policy for SMS Alert

**Last updated: 2026-05-28**

## Data Collection

This app does **not** collect, transmit, or share any personal data with external servers or third parties. All data processing happens on-device only.

## SMS Access

This app accesses SMS messages **solely on-device** for the purpose of detecting keyword matches to trigger local alerts. SMS content is:
- Read locally via `SmsReceiver` broadcast
- Checked against user-defined keywords stored in local SharedPreferences/DataStore
- Never transmitted off-device
- Never uploaded to any server
- Never shared with third parties

## Permissions

The app requests the following permissions and explains their purpose:

| Permission | Purpose |
|------------|---------|
| RECEIVE_SMS / READ_SMS | Detect incoming SMS containing user-defined alert keywords |
| POST_NOTIFICATIONS | Display alert notifications |
| VIBRATE | Vibrate device during alerts |
| WAKE_LOCK | Wake screen for urgent alerts |
| FOREGROUND_SERVICE | Run monitoring service in background |
| USE_FULL_SCREEN_INTENT | Show full-screen alert on lock screen |
| SYSTEM_ALERT_WINDOW | Display alert overlay in emergency situations |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | Prevent system from killing monitoring service |

## Data Storage

- **Alert history**: Stored locally in Room database on device
- **Keywords**: Stored locally in SharedPreferences on device
- **App preferences**: Stored locally in DataStore on device

All data can be cleared by the user via the app's settings or by uninstalling the app.

## Third-Party Services

This app does not use any third-party analytics, advertising, or tracking services.

## Contact

For questions about this privacy policy, please contact the developer via the app's Google Play Store listing.
