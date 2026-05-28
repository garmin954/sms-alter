# ── General Android ──

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes EnclosingMethod

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Application

# Keep the R class (Compose Navigation needs it)
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ── Kotlin ──

-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin metadata for reflection libraries
-keep class kotlin.Metadata { *; }

# ── Room ──

-keep class com.example.smsalert.data.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ──

-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class javax.annotation.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt generated components
-keep class com.example.smsalert.SmsAlertApp { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Hilt ViewModel factory
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ── DataStore ──

-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── Compose ──

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose runtime needs to see composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── Navigation Compose ──

-keep class androidx.navigation.** { *; }

# ── App-specific ──

# Keep data classes used in state flows / serialization
-keep class com.example.smsalert.ui.components.PermissionItem { *; }
-keep class com.example.smsalert.ui.components.BottomNavItem { *; }
-keep class com.example.smsalert.ui.components.LogItem { *; }
-keep class com.example.smsalert.data.entity.AlertRecord { *; }

# Keep BroadcastReceiver registered in manifest
-keep class com.example.smsalert.SmsReceiver { *; }

# Keep ViewModel constructors for Hilt injection
-keepclassmembers class com.example.smsalert.viewmodel.** {
    <init>(...);
}

# ── Remove logging in release ──

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

-assumenosideeffects class com.example.smsalert.LogStore {
    public static void d(...);
}
