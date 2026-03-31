# ChatLedger ProGuard Rules

# Keep Room entities
-keep class com.chatledger.data.entity.** { *; }
-keep class com.chatledger.data.dao.** { *; }

# Keep Gson models
-keep class com.chatledger.ai.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
