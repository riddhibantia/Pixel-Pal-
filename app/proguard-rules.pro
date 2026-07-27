# Hilt rules
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# DataStore rules
-keep class androidx.datastore.preferences.protobuf.** { *; }

# Coil rules
-keep class coil.** { *; }

# Kotlinx Serialization rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class kotlinx.serialization.json.** { *; }
