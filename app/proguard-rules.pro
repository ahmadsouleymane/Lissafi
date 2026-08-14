# Lissafi ProGuard Rules
-keepattributes *Annotation*
-keep class com.lissafi.app.data.entity.** { *; }

# WorkManager instancie sa base Room interne (WorkDatabase_Impl) par réflexion
# avec un nom de classe calculé dynamiquement : R8 ne peut pas tracer cet appel
# et supprime le constructeur sans cette règle, ce qui fait planter l'app au
# tout premier lancement (androidx.startup.InitializationProvider).
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
}

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
