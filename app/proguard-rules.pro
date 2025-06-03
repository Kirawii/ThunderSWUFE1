# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 启用优化
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# 保持必要的数据库实体和 DAO
-keep class com.kirawii.thunderswufe.data.database.ElectricityRecord { *; }
-keep class com.kirawii.thunderswufe.data.database.ElectricityDao_Impl { *; }

# 保持网络接口
-keep class com.kirawii.thunderswufe.network.ElectricityService { *; }
-keep class com.kirawii.thunderswufe.network.ElectricityResponse { *; }

# 保持必要的 TensorFlow Lite 类
-keep class org.tensorflow.lite.support.common.** { *; }
-keep class org.tensorflow.lite.support.model.** { *; }

# 保持必要的 Compose 类
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# 保持 Kotlin 协程相关
-keep class kotlinx.coroutines.** { *; }

# 移除日志
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# 保持自定义应用类
-keep class com.kirawii.thunderswufe.ThunderApplication { *; }

# 启用代码压缩
-repackageclasses

# 移除未使用的资源
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保持 Retrofit 服务接口
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# 保持 Gson 类型适配器
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保持 Room 数据库迁移
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static java.lang.Class[] getRequiredTypeConverters();
}

# 保持 Kotlin 序列化
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations