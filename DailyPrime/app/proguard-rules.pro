# Keep Room database entities and DAOs
-keep class com.dailyprime.app.data.local.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keepclassmembers class * {
    @androidx.room.Dao *;
}

# Keep BroadcastReceivers and WorkManager workers
-keep class com.dailyprime.app.notifications.** { *; }
-keep class com.dailyprime.app.worker.** { *; }
