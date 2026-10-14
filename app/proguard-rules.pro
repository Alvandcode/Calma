# Calma keeps no reflection-based serialization, so default rules are sufficient.
# Compose, Room and DataStore ship their own consumer rules.

# Keep line numbers for readable crash reports while still obfuscating.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room - keep entities, DAOs, and generated implementations
-keep class app.calma.data.local.entity.** { *; }
-keep class app.calma.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Kotlin metadata used by reflection-free coroutines/debugging
-dontwarn kotlinx.coroutines.**