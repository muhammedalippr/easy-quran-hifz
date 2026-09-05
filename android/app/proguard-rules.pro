# Keep line numbers and source file names for crash reports
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes JavascriptInterface

# Capacitor Core & Plugins
-keep public class com.easyquranhifz.app.MainActivity { *; }
-keep public class * extends com.getcapacitor.Plugin {
    public <methods>;
}
-keep public class com.getcapacitor.** { *; }

# Google Play In-App Update
-keep class com.google.android.play.core.** { *; }

# Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }

# WebView JavaScript Interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
