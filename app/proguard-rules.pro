# Keep Room and JXL classes if ProGuard/R8 is enabled in release builds
-keep class * extends androidx.room.RoomDatabase
-keep class jxl.** { *; }
-dontwarn jxl.**
