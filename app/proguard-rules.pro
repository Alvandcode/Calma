# Calma keeps no reflection-based serialization, so default rules are sufficient.
# Compose, Room and DataStore ship their own consumer rules.

# Keep line numbers for readable crash reports while still obfuscating.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room generated implementations
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Kotlin metadata used by reflection-free coroutines/debugging
-dontwarn kotlinx.coroutines.**