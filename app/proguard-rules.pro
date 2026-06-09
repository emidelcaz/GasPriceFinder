# ============================================================
# REGLAS PROGUARD ESPECIFICAS PARA HILT + WORKMANAGER
# ============================================================
# Estas reglas son CRITICAS para que R8/ProGuard no elimine
# ni ofusque las clases necesarias para la inyeccion de
# dependencias en Workers.
#
# Si ves NoSuchMethodException en SyncWorker despues de
# compilar en release (minifyEnabled=true), lo mas probable
# es que falten estas reglas.
# ============================================================

# --- Hilt ---
-keepattributes *Annotation*
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManagerHolder { *; }

# --- AssistedInject (usado por @HiltWorker) ---
-keep class dagger.assisted.** { *; }
-keep @dagger.assisted.AssistedInject class *
-keepclassmembers @dagger.assisted.AssistedInject class * {
    @dagger.assisted.Assisted <init>(...);
}

# --- Hilt Workers ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep @androidx.hilt.work.HiltWorker class *
-keepclassmembers @androidx.hilt.work.HiltWorker class * {
    @dagger.assisted.AssistedInject <init>(...);
}

# --- WorkManager ---
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
-dontwarn androidx.work.**

# --- androidx.startup ---
-keep class androidx.startup.** { *; }

# --- Clases especificas de la app ---
-keep class com.gas_price_finder.data.local.worker.SyncWorker { *; }
-keepclassmembers class com.gas_price_finder.data.local.worker.SyncWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters, ...);
}