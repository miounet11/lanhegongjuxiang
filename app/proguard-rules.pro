# ===================================================
# 蓝河助手 ProGuard 生产级混淆配置
# 版本: 1.0.0
# 最后更新: 2025-11-24
# ===================================================

# ===================================================
# 1. 基础优化配置
# ===================================================

# 优化通过次数（推荐5次以获得更好的优化效果）
-optimizationpasses 5

# 保留行号信息用于崩溃分析（生产环境必需）
-keepattributes SourceFile,LineNumberTable

# 保留注解信息（Room、Retrofit等框架必需）
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 保留异常信息
-keepattributes Exceptions

# 混淆类名时使用短名称
-renamesourcefileattribute SourceFile

# 不混淆枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 生产环境移除Log调用（保留Error和Warn级别）
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 移除调试相关代码
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
}

# 优化选项
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-allowaccessmodification
-repackageclasses ''

# ===================================================
# 2. Android核心类保护
# ===================================================

# 保留四大组件
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# 保留Fragment及其生命周期方法
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment {
    public void setUserVisibleHint(boolean);
    public void onHiddenChanged(boolean);
    public void onResume();
    public void onPause();
}

# 保留Dialog
-keep public class * extends android.app.Dialog
-keep public class * extends androidx.appcompat.app.AppCompatDialog

# 保留View构造函数（XML布局需要）
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
    void set*(***);
    *** get*();
}

# 保留自定义View
-keep class com.lanhe.gongjuxiang.views.** { *; }
-keep class com.lanhe.gongjuxiang.widgets.** { *; }

# ViewBinding相关
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * bind(android.view.View);
    public static * inflate(android.view.LayoutInflater);
}
-keep class com.lanhe.gongjuxiang.databinding.** { *; }

# ===================================================
# 3. Shizuku框架保护（核心功能）
# ===================================================

# Shizuku API完整保护
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# Shizuku回调和接口
-keepclassmembers class * implements rikka.shizuku.Shizuku$OnRequestPermissionResultListener { *; }
-keepclassmembers class * implements rikka.shizuku.Shizuku$OnBinderReceivedListener { *; }
-keepclassmembers class * implements rikka.shizuku.Shizuku$OnBinderDeadListener { *; }

# 保留ShizukuManager及所有方法
-keep class com.lanhe.gongjuxiang.utils.ShizukuManager { *; }
-keep class com.lanhe.gongjuxiang.utils.ShizukuManagerImpl { *; }

# 隐藏API绕过库
-keep class org.lsposed.hiddenapibypass.** { *; }

# ===================================================
# 4. Room数据库保护
# ===================================================

# Room实体类（必须保留所有字段）
-keep @androidx.room.Entity class * {
    <fields>;
    <init>(...);
    *;
}

# Room DAO接口
-keep @androidx.room.Dao interface * {
    *;
}

# Room Database类
-keep @androidx.room.Database class * {
    *;
}

# Room TypeConverters
-keep @androidx.room.TypeConverters class * {
    *;
}

# 保留应用的数据库和实体类
-keep class com.lanhe.gongjuxiang.utils.AppDatabase { *; }
-keep class com.lanhe.gongjuxiang.models.** { *; }
-keep class com.lanhe.gongjuxiang.data.entities.** { *; }
-keep class com.lanhe.gongjuxiang.data.dao.** { *; }

# Room运行时
-keep class androidx.room.** { *; }
-keep class android.arch.persistence.room.** { *; }

# ===================================================
# 5. Kotlin协程保护
# ===================================================

# Kotlin协程
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.** {
    *;
}
-dontwarn kotlinx.coroutines.**

# 保留挂起函数
-keepclassmembers class * {
    suspend <methods>;
}

# 协程异常处理
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }

# ===================================================
# 6. JSON序列化保护（Gson）
# ===================================================

# 保留所有数据模型类
-keep class com.lanhe.gongjuxiang.models.** {
    <fields>;
    <init>(...);
    *;
}

# Gson相关
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# 保留使用@SerializedName注解的字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留使用@Expose注解的字段
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}

# ===================================================
# 7. Native方法和反射保护
# ===================================================

# 保留native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留反射调用的类和方法
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# WebView JavaScript接口
-keep public class com.lanhe.gongjuxiang.browser.** {
    @android.webkit.JavascriptInterface <methods>;
}

# ===================================================
# 8. 第三方库配置
# ===================================================

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface * extends retrofit2.Call { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# Lottie
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# Dexter权限库
-keep class com.karumi.dexter.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# LeakCanary（仅调试版本）
-dontwarn com.squareup.leakcanary.**

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }

# ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Shimmer
-keep class com.facebook.shimmer.** { *; }

# PhotoView
-keep class com.github.chrisbanes.photoview.** { *; }

# RecyclerView Animators
-keep class jp.wasabeef.recyclerview.** { *; }

# Fluent System Icons
-keep class com.microsoft.design.** { *; }

# Apache Commons
-dontwarn org.apache.commons.**
-keep class org.apache.commons.** { *; }

# Mozilla Rhino
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

# Fresco
-keep class com.facebook.fresco.** { *; }
-keep class com.facebook.drawee.** { *; }
-keep class com.facebook.imagepipeline.** { *; }
-dontwarn com.facebook.**

# ===================================================
# 9. 项目模块保护
# ===================================================

# 保留所有Activity
-keep class com.lanhe.gongjuxiang.activities.** { *; }

# 保留所有Fragment
-keep class com.lanhe.gongjuxiang.fragments.** { *; }

# 保留所有ViewModel
-keep class com.lanhe.gongjuxiang.viewmodels.** { *; }

# 保留所有Service
-keep class com.lanhe.gongjuxiang.services.** { *; }

# 保留所有Adapter
-keep class com.lanhe.gongjuxiang.adapters.** { *; }

# 保留所有工具类（核心业务逻辑）
-keep class com.lanhe.gongjuxiang.utils.** { *; }

# 保留浏览器相关类
-keep class com.lanhe.gongjuxiang.browser.** { *; }

# 保留所有接口
-keep interface com.lanhe.gongjuxiang.interfaces.** { *; }

# ===================================================
# 10. 自定义模块保护
# ===================================================

# 核心模块
-keep class com.lanhe.mokuai.core.** { *; }
-keep interface com.lanhe.mokuai.core.** { *; }

# 功能模块
-keep class com.lanhe.mokuai.modules.** { *; }
-keep interface com.lanhe.mokuai.modules.** { *; }

# 模块API接口（必须保留以支持模块间通信）
-keep interface * extends com.lanhe.mokuai.core.common.ModuleApi { *; }

# ===================================================
# 11. 安全相关保护
# ===================================================

# 生物识别
-keep class androidx.biometric.** { *; }

# 加密库
-keep class androidx.security.crypto.** { *; }

# ===================================================
# 12. 其他重要配置
# ===================================================

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# R文件
-keep class **.R$* {
    <fields>;
}

# BuildConfig
-keep class com.lanhe.gongjuxiang.BuildConfig { *; }

# 保留Kotlin元数据
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# AndroidX相关
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Android Support库向后兼容
-dontwarn android.support.**
-keep class android.support.** { *; }

# ===================================================
# 13. 性能优化特定规则
# ===================================================

# 优化字符串处理
-optimizations !code/allocation/variable

# 保留性能监控相关类
-keep class com.lanhe.gongjuxiang.utils.PerformanceMonitor { *; }
-keep class com.lanhe.gongjuxiang.utils.RealPerformanceMonitorManager { *; }
-keep class com.lanhe.gongjuxiang.utils.SystemOptimizer { *; }

# ===================================================
# 14. 调试和崩溃报告
# ===================================================

# 保留堆栈跟踪相关信息
-keepattributes SourceFile,LineNumberTable

# 崩溃报告框架需要的信息
-keep public class * extends java.lang.Exception

# ===================================================
# 15. 警告处理
# ===================================================

# 忽略某些警告（根据实际编译情况调整）
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# ===================================================
# 特别注意事项：
# 1. 发布前必须测试所有功能，确保混淆没有破坏功能
# 2. 保留映射文件(mapping.txt)用于崩溃日志解析
# 3. 定期检查并更新第三方库的混淆规则
# 4. 生产环境建议开启 isMinifyEnabled = true
# 5. 测试Shizuku功能确保权限系统正常工作
# ===================================================