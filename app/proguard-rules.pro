# Strip verbose/debug/info logs from release builds (avoids leaking data to logcat).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
# Hide original source file names in published stack traces.
-renamesourcefileattribute SourceFile
