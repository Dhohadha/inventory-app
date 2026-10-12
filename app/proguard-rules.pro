# Add project specific ProGuard rules here.
-keep class com.packready.shopkeeper.data.local.entity.** { *; }
-keep class com.packready.shopkeeper.domain.model.** { *; }
-keep class com.packready.shopkeeper.data.remote.dto.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
