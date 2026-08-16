# Hilt
-keep class dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep class dagger.hilt.internal.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}
-dontwarn dagger.hilt.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class kotlinx.serialization.json.** { *; }
-dontwarn kotlinx.serialization.**

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * implements androidx.work.ListenableWorker { *; }
-keepclassmembers class * {
    @androidx.work.WorkerFactory *;
}
-dontwarn androidx.work.impl.**

# DataStore
-keep class androidx.datastore.preferences.protobuf.** { *; }
-dontwarn androidx.datastore.**

# AndroidX / general
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
-dontwarn javax.annotation.**
