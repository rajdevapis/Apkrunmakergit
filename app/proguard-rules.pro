-keep class com.startapp.** { *; }
-dontwarn com.startapp.**
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
