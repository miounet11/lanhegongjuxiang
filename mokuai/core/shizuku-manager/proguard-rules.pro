# Shizuku管理模块混淆规则
# 添加于 2024-01-01

# ===============================
# 基本保留规则
# ===============================

# 保留ShizukuManager类
-keep class com.lanhe.module.shizuku.ShizukuManager { *; }

# 保留所有接口
-keep class com.lanhe.module.shizuku.interfaces.** { *; }

# 保留异常类
-keep class com.lanhe.module.shizuku.exception.** { *; }

# 保留常量类
-keep class com.lanhe.module.shizuku.constants.** { *; }

# ===============================
# 反射相关规则
# ===============================

# 保留通过反射调用的方法
-keepclassmembers class com.lanhe.module.shizuku.** {
    public <methods>;
    public static <methods>;
}

# 保留枚举类
-keep class com.lanhe.module.shizuku.** extends java.lang.Enum { *; }

# ===============================
# 注解相关规则
# ===============================

# 保留注解类
-keepattributes *Annotation*
-keep class * extends java.lang.annotation.Annotation { *; }

# ===============================
# 泛型相关规则
# ===============================

# 保留泛型信息
-keepattributes Signature, InnerClasses, EnclosingMethod

# ===============================
# 日志和调试相关
# ===============================

# 保留日志方法（如果需要调试）
-keep class com.lanhe.module.shizuku.utils.ShizukuUtils {
    public static void logDebug(java.lang.String);
    public static void logWarning(java.lang.String, java.lang.Throwable);
    public static void logError(java.lang.String, java.lang.Throwable);
}

# ===============================
# 第三方库规则
# ===============================

# 如果使用了Shizuku框架，保留相关类
-keep class rikka.shizuku.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }

# ===============================
# 资源相关规则
# ===============================

# 保留资源文件引用
-keepclassmembers class ** {
    public static <fields>;
}

# ===============================
# 优化相关规则
# ===============================

# 允许内联优化
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# 允许方法内联
-inline { @com.lanhe.module.shizuku.annotations.Inline }

# ===============================
# 警告抑制
# ===============================

# 忽略某些警告
-dontwarn com.lanhe.module.shizuku.**
-dontwarn rikka.shizuku.**
-dontwarn org.lsposed.hiddenapibypass.**

# ===============================
# 测试相关规则
# ===============================

# 测试时不混淆
-dontobfuscate

# 保留测试类
-keep class com.lanhe.module.shizuku.**Test { *; }
-keep class com.lanhe.module.shizuku.**Test$* { *; }
