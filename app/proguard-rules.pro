# Lissafi ProGuard Rules
-keepattributes *Annotation*
-keep class com.lissafi.app.data.entity.** { *; }

# Purge des appels Log.* dans le build RELEASE : aucune donnée (identifiants,
# montants, tokens) ne doit rester journalisable sur un appareil en production.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static java.lang.String getStackTraceString(java.lang.Throwable);
}
