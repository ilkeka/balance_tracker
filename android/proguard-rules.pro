# Project-specific R8 rules for the release build.

# kotlinx.serialization: keep generated serializers and their Companion objects.
# Navigation routes and sync payloads are serialized through generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Koin resolves types and qualifiers at runtime from class references.
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
