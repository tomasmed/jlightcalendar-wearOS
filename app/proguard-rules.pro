# ProGuard / R8 rules for kotlinx.serialization and Ktor on Wear OS
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.tomasmed.jlight.wear.**$$serializer { *; }
-keepclassmembers class dev.tomasmed.jlight.wear.** {
    *** Companion;
}
-keepclasseswithmembers class dev.tomasmed.jlight.wear.** {
    kotlinx.serialization.KSerializer serializer(...);
}
