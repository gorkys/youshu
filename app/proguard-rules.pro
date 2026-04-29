# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Coil
-dontwarn coil.**

# Hilt
-keep class dagger.hilt.** { *; }
