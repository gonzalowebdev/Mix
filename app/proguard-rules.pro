# Mix: reglas para el modo optimizado (R8)
-keep class com.gonzalowebdev.mix.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
