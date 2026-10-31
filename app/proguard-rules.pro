# --- kotlinx-serialization (manifest, FeaturedApp, Account...) ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class fr.birdywood.lapeka.**$$serializer { *; }
-keepclassmembers class fr.birdywood.lapeka.** { *** Companion; }
-keepclasseswithmembers class fr.birdywood.lapeka.** { kotlinx.serialization.KSerializer serializer(...); }

# --- Retrofit ---
-keepattributes Signature, Exceptions
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# --- OkHttp ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Retire les logs en release (bonus, recommandé) ---
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}