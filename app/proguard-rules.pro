# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.postiz.mobile.**$$serializer { *; }
-keepclassmembers class com.postiz.mobile.** { *** Companion; }
-keepclasseswithmembers class com.postiz.mobile.** { kotlinx.serialization.KSerializer serializer(...); }

# R8 is on for release builds. Keep what is reached only by reflection:
# kotlinx.serialization models and the Retrofit service interface are covered above and by the libraries' own
# consumer rules; these cover the remaining app-side reflection and keep readable stack traces.
-keepattributes Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-keep interface com.postiz.mobile.data.remote.PostizApiService { *; }
-renamesourcefileattribute SourceFile
