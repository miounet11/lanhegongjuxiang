package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import jp.wasabeef.glide.transformations.GrayscaleTransformation

/**
 * 蓝河助手 - 图片加载工具类
 *
 * 功能特性：
 * - Glide图片加载封装
 * - 圆形图片变换
 * - 模糊效果处理
 * - 交叉淡入动画
 * - 缓存策略管理
 * - 加载状态监听
 */
class ImageLoader private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: ImageLoader? = null

        fun getInstance(): ImageLoader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageLoader().also { INSTANCE = it }
            }
        }

        // 默认配置
        private const val DEFAULT_CROSSFADE_DURATION = 300
        private const val DEFAULT_BLUR_RADIUS = 25
        private const val DEFAULT_CORNER_RADIUS = 8
    }

    /**
     * 图片加载配置类
     */
    data class LoadConfig(
        val placeholder: Int? = null,
        val error: Int? = null,
        val crossfadeDuration: Int = DEFAULT_CROSSFADE_DURATION,
        val diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.ALL,
        val skipMemoryCache: Boolean = false,
        val transformation: ImageTransformation = ImageTransformation.NONE,
        val blurRadius: Int = DEFAULT_BLUR_RADIUS,
        val cornerRadius: Int = DEFAULT_CORNER_RADIUS,
        val listener: ImageLoadListener? = null
    )

    /**
     * 图片变换类型
     */
    enum class ImageTransformation {
        NONE,           // 无变换
        CIRCLE,         // 圆形
        ROUNDED,        // 圆角
        BLUR,           // 模糊
        GRAYSCALE,      // 灰度
        CENTER_CROP     // 中心裁剪
    }

    /**
     * 图片加载监听器
     */
    interface ImageLoadListener {
        fun onLoadStarted()
        fun onLoadSuccess(drawable: Drawable?)
        fun onLoadFailed(errorDrawable: Drawable?)
        fun onLoadCleared()
    }

    /**
     * 基础图片加载方法
     */
    fun load(context: Context, url: String, imageView: ImageView, config: LoadConfig = LoadConfig()) {
        val requestOptions = buildRequestOptions(config)

        var glideRequest = Glide.with(context)
            .load(url)
            .apply(requestOptions)
            .transition(DrawableTransitionOptions.withCrossFade(config.crossfadeDuration))

        // 添加监听器
        config.listener?.let { listener ->
            glideRequest = glideRequest.listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onLoadFailed(null)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onLoadSuccess(resource)
                    return false
                }
            })
        }

        glideRequest.into(imageView)
    }

    /**
     * 加载圆形图片
     */
    fun loadCircular(context: Context, url: String, imageView: ImageView, config: LoadConfig = LoadConfig()) {
        val circularConfig = config.copy(transformation = ImageTransformation.CIRCLE)
        load(context, url, imageView, circularConfig)
    }

    /**
     * 加载圆角图片
     */
    fun loadRounded(context: Context, url: String, imageView: ImageView, cornerRadius: Int = DEFAULT_CORNER_RADIUS, config: LoadConfig = LoadConfig()) {
        val roundedConfig = config.copy(
            transformation = ImageTransformation.ROUNDED,
            cornerRadius = cornerRadius
        )
        load(context, url, imageView, roundedConfig)
    }

    /**
     * 加载模糊图片
     */
    fun loadBlurred(context: Context, url: String, imageView: ImageView, blurRadius: Int = DEFAULT_BLUR_RADIUS, config: LoadConfig = LoadConfig()) {
        val blurredConfig = config.copy(
            transformation = ImageTransformation.BLUR,
            blurRadius = blurRadius
        )
        load(context, url, imageView, blurredConfig)
    }

    /**
     * 加载灰度图片
     */
    fun loadGrayscale(context: Context, url: String, imageView: ImageView, config: LoadConfig = LoadConfig()) {
        val grayscaleConfig = config.copy(transformation = ImageTransformation.GRAYSCALE)
        load(context, url, imageView, grayscaleConfig)
    }

    /**
     * 预加载图片到缓存
     */
    fun preload(context: Context, url: String) {
        Glide.with(context)
            .load(url)
            .preload()
    }

    /**
     * 清除指定ImageView的图片
     */
    fun clear(context: Context, imageView: ImageView) {
        Glide.with(context).clear(imageView)
    }

    /**
     * 清除内存缓存
     */
    fun clearMemoryCache(context: Context) {
        Glide.get(context).clearMemory()
    }

    /**
     * 清除磁盘缓存（需要在后台线程调用）
     */
    fun clearDiskCache(context: Context) {
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }

    /**
     * 获取缓存大小
     */
    fun getCacheSize(context: Context): Long {
        val cacheDir = Glide.getPhotoCacheDir(context)
        return cacheDir?.let { dir ->
            dir.listFiles()?.sumOf { it.length() } ?: 0L
        } ?: 0L
    }

    /**
     * 构建RequestOptions
     */
    private fun buildRequestOptions(config: LoadConfig): RequestOptions {
        var requestOptions = RequestOptions()
            .diskCacheStrategy(config.diskCacheStrategy)
            .skipMemoryCache(config.skipMemoryCache)

        // 设置占位符和错误图片
        config.placeholder?.let { requestOptions = requestOptions.placeholder(it) }
        config.error?.let { requestOptions = requestOptions.error(it) }

        // 应用变换
        when (config.transformation) {
            ImageTransformation.CIRCLE -> {
                requestOptions = requestOptions.transform(CenterCrop(), CircleCrop())
            }
            ImageTransformation.ROUNDED -> {
                requestOptions = requestOptions.transform(
                    CenterCrop(),
                    RoundedCorners(config.cornerRadius)
                )
            }
            ImageTransformation.BLUR -> {
                requestOptions = requestOptions.transform(
                    CenterCrop(),
                    BlurTransformation(config.blurRadius, 1)
                )
            }
            ImageTransformation.GRAYSCALE -> {
                requestOptions = requestOptions.transform(
                    CenterCrop(),
                    GrayscaleTransformation()
                )
            }
            ImageTransformation.CENTER_CROP -> {
                requestOptions = requestOptions.transform(CenterCrop())
            }
            ImageTransformation.NONE -> {
                // 不应用变换
            }
        }

        return requestOptions
    }

    /**
     * 构建器模式加载
     */
    class Builder(private val context: Context) {
        private var url: String = ""
        private var config: LoadConfig = LoadConfig()

        fun url(url: String): Builder {
            this.url = url
            return this
        }

        fun placeholder(placeholder: Int): Builder {
            this.config = config.copy(placeholder = placeholder)
            return this
        }

        fun error(error: Int): Builder {
            this.config = config.copy(error = error)
            return this
        }

        fun crossfade(duration: Int): Builder {
            this.config = config.copy(crossfadeDuration = duration)
            return this
        }

        fun diskCacheStrategy(strategy: DiskCacheStrategy): Builder {
            this.config = config.copy(diskCacheStrategy = strategy)
            return this
        }

        fun skipMemoryCache(skip: Boolean): Builder {
            this.config = config.copy(skipMemoryCache = skip)
            return this
        }

        fun transformation(transformation: ImageTransformation): Builder {
            this.config = config.copy(transformation = transformation)
            return this
        }

        fun blurRadius(radius: Int): Builder {
            this.config = config.copy(blurRadius = radius)
            return this
        }

        fun cornerRadius(radius: Int): Builder {
            this.config = config.copy(cornerRadius = radius)
            return this
        }

        fun listener(listener: ImageLoadListener): Builder {
            this.config = config.copy(listener = listener)
            return this
        }

        fun into(imageView: ImageView) {
            getInstance().load(context, url, imageView, config)
        }
    }
}

/**
 * 扩展函数，简化使用
 */
fun ImageView.loadImage(url: String, config: ImageLoader.LoadConfig = ImageLoader.LoadConfig()) {
    ImageLoader.getInstance().load(context, url, this, config)
}

fun ImageView.loadCircularImage(url: String, config: ImageLoader.LoadConfig = ImageLoader.LoadConfig()) {
    ImageLoader.getInstance().loadCircular(context, url, this, config)
}

fun ImageView.loadRoundedImage(url: String, cornerRadius: Int = 8, config: ImageLoader.LoadConfig = ImageLoader.LoadConfig()) {
    ImageLoader.getInstance().loadRounded(context, url, this, cornerRadius, config)
}

fun ImageView.loadBlurredImage(url: String, blurRadius: Int = 25, config: ImageLoader.LoadConfig = ImageLoader.LoadConfig()) {
    ImageLoader.getInstance().loadBlurred(context, url, this, blurRadius, config)
}