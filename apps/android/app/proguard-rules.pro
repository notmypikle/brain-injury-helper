# Keep kotlinx.serialization generated serializers
-keepclassmembers class com.bih.app.api.** {
    *** Companion;
}
-keepclasseswithmembers class com.bih.app.api.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.slf4j.**
