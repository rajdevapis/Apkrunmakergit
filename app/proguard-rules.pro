-keep class com.unity3d.** { *; }
-keep interface com.unity3d.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
