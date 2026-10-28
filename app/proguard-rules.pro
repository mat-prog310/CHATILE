# ProGuard rules for Chatile - Optimized for obfuscation

# ============================================
# GLOBAL OPTIONS
# ============================================

# Enable obfuscation and optimization
# (removed -dontobfuscate and -dontoptimize to enable code shrinking and obfuscation)

# Print usage to analyze what's being kept
-printusage unused.txt

# Write mapping file for deobfuscation
-printmapping mapping.txt

# Ignore warnings for unused classes/methods
-dontwarn **

# Keep line numbers for stack traces (optional, remove for better obfuscation)
# -renamesourcefileattribute SourceFile
# -keepattributes SourceFile,LineNumberTable

# ============================================
# ANDROID SPECIFIC
# ============================================

# Keep Android components (minimal)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Application
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep R classes (required for resources)
-keep class **.R
-keep class **.R$*

# Keep BuildConfig
-keep class **.BuildConfig { *; }

# Keep Manifest meta-data
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================
# JETPACK COMPOSE (minimal rules)
# ============================================

# Keep Compose stable annotations
-keepclassmembers class * {
    @androidx.compose.runtime.Stable
    <methods>;
}

# Keep Compose state holders
-keepclassmembers class * {
    @androidx.compose.runtime.MutableState
    <methods>;
}

# Keep Compose Preview annotations
-keep class androidx.compose.ui.tooling.preview.Preview {
    <methods>;
}

# Keep Compose material icons (if using vector assets)
-keep class androidx.compose.material.icons.* { *; }
-keep class androidx.compose.material3.icons.* { *; }

# Keep Compose navigation
-keep class androidx.navigation.** { *; }
-keepclassmembers class * {
    @androidx.navigation.* <methods>;
}

# Keep Compose viewModel
-keepclassmembers class * {
    @androidx.lifecycle.ViewModelStoreOwner <methods>;
}

# ============================================
# FIREBASE (minimal rules)
# ============================================

# Firebase Auth - keep only necessary classes
-keep class com.google.firebase.auth.FirebaseAuth { *; }
-keep class com.google.firebase.auth.FirebaseUser { *; }
-keep class com.google.firebase.auth.GoogleAuthProvider { *; }
-keep class com.google.firebase.auth.PhoneAuthProvider { *; }
-keep class com.google.firebase.auth.EmailAuthProvider { *; }
-keep class com.google.firebase.auth.AuthCredential { *; }
-keep class com.google.firebase.auth.AuthResult { *; }

# Firebase Analytics - minimal
-keep class com.google.firebase.analytics.FirebaseAnalytics { *; }

# Firebase common classes needed for initialization
-keep class com.google.firebase.FirebaseApp { *; }
-keep class com.google.firebase.FirebaseOptions { *; }

# Google Play Services - minimal for Firebase
-keep class com.google.android.gms.common.GoogleApiAvailability { *; }
-keep class com.google.android.gms.common.api.GoogleApiClient { *; }
-keep class com.google.android.gms.tasks.Task { *; }

# Keep annotations for Firebase
-keepattributes *Annotation*
-keepattributes Signature

# ============================================
# BILLING CLIENT
# ============================================

-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-keepclassmembers class * {
    @com.android.billingclient.api.* <methods>;
}

# ============================================
# CREDENTIALS & GOOGLE SIGN-IN
# ============================================

-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.api.signin.GoogleSignIn {
    *;
}
-keep class com.google.android.gms.auth.api.signin.GoogleSignInClient {
    *;
}
-keep class com.google.android.gms.auth.api.signin.GoogleSignInAccount {
    *;
}
-keep class com.google.android.gms.common.api.ApiException {
    *;
}

# ============================================
# OKHTTP (minimal)
# ============================================

# Keep OkHttp annotations
-keepclassmembers class * {
    @com.squareup.okhttp3.* <methods>;
}

# Keep OkHttp classes needed for requests
-keep class com.squareup.okhttp3.OkHttpClient { *; }
-keep class com.squareup.okhttp3.Request { *; }
-keep class com.squareup.okhttp3.Response { *; }

# ============================================
# COROUTINES
# ============================================

# Keep coroutine annotations only
-keepclassmembers class * {
    @kotlinx.coroutines.* <methods>;
}

# ============================================
# COIL (Image loading)
# ============================================

-keep class coil.** { *; }
-keep interface coil.** { *; }
-keepclassmembers class * {
    @coil.annotation.* <methods>;
}

# ============================================
# GLANCE (Widgets)
# ============================================

-keep class androidx.glance.** { *; }
-keep interface androidx.glance.** { *; }

# ============================================
# KOTLIN SERIALIZATION (if used)
# ============================================

-keepclassmembers class ** {
    @kotlinx.serialization.Serializable
    <methods>;
}

# ============================================
# RETROFIT (if used - not in current deps but keeping for safety)
# ============================================

-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepclassmembers class * {
    @retrofit2.* <methods>;
}

# ============================================
# APPLICATION SPECIFIC
# ============================================

# Keep MainActivity for deep links
-keep public class com.chatile.MainActivity { *; }

# Keep data classes that need serialization (adjust as needed)
# -keepclassmembers class com.chatile.data.** { *; }

# ============================================
# OPTIMIZATION HINTS
# ============================================

# Allow shrinking of library classes
-dontskipnonpubliclibraryclassmembers

# Allow optimization of access modifiers
-allowaccessmodification

# Allow removing unused code
-dontskipnonpubliclibraryclasses
