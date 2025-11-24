package com.lanhe.mokuai.imagehelper

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.renderscript.*
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.math.*

/**
 * 图片助手 - 图片处理和优化工具
 */
class ImageHelper(private val context: Context) {

    companion object {
        private const val JPEG_QUALITY_HIGH = 95
        private const val JPEG_QUALITY_MEDIUM = 85
        private const val JPEG_QUALITY_LOW = 75
        private const val MAX_TEXTURE_SIZE = 4096 // OpenGL ES最大纹理尺寸
    }

    data class ImageInfo(
        val width: Int,
        val height: Int,
        val mimeType: String,
        val sizeBytes: Long,
        val orientation: Int = ExifInterface.ORIENTATION_NORMAL,
        val hasAlpha: Boolean = false,
        val colorSpace: String? = null,
        val metadata: Map<String, String> = emptyMap()
    )

    data class ProcessResult(
        val success: Boolean,
        val bitmap: Bitmap? = null,
        val outputPath: String? = null,
        val sizeBytes: Long = 0,
        val processingTime: Long = 0,
        val error: String? = null
    )

    /**
     * 获取图片信息
     */
    suspend fun getImageInfo(uri: Uri): ImageInfo? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val mimeType = context.contentResolver.getType(uri) ?: "image/*"
            val sizeBytes = context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0

            // 获取EXIF信息
            val orientation = getExifOrientation(uri)
            val metadata = getExifMetadata(uri)

            ImageInfo(
                width = options.outWidth,
                height = options.outHeight,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                orientation = orientation,
                hasAlpha = options.outMimeType == "image/png" || options.outMimeType == "image/webp",
                colorSpace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    options.outColorSpace?.toString()
                } else null,
                metadata = metadata
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 压缩图片
     */
    suspend fun compressImage(
        uri: Uri,
        quality: Int = JPEG_QUALITY_MEDIUM,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val bitmap = loadBitmap(uri, maxWidth, maxHeight)
            val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.${getFormatExtension(format)}")

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(format, quality, out)
            }

            ProcessResult(
                success = true,
                bitmap = bitmap,
                outputPath = outputFile.absolutePath,
                sizeBytes = outputFile.length(),
                processingTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ProcessResult(
                success = false,
                error = e.message,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 调整图片大小
     */
    suspend fun resizeImage(
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        maintainAspectRatio: Boolean = true
    ): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val originalBitmap = loadBitmap(uri)
            val (finalWidth, finalHeight) = if (maintainAspectRatio) {
                calculateAspectRatioSize(
                    originalBitmap.width,
                    originalBitmap.height,
                    targetWidth,
                    targetHeight
                )
            } else {
                Pair(targetWidth, targetHeight)
            }

            val resizedBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                finalWidth,
                finalHeight,
                true
            )

            if (resizedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            ProcessResult(
                success = true,
                bitmap = resizedBitmap,
                processingTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ProcessResult(
                success = false,
                error = e.message,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 裁剪图片
     */
    suspend fun cropImage(
        uri: Uri,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val originalBitmap = loadBitmap(uri)

            // 验证裁剪参数
            val cropX = x.coerceIn(0, originalBitmap.width - 1)
            val cropY = y.coerceIn(0, originalBitmap.height - 1)
            val cropWidth = width.coerceIn(1, originalBitmap.width - cropX)
            val cropHeight = height.coerceIn(1, originalBitmap.height - cropY)

            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                cropX,
                cropY,
                cropWidth,
                cropHeight
            )

            if (croppedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            ProcessResult(
                success = true,
                bitmap = croppedBitmap,
                processingTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ProcessResult(
                success = false,
                error = e.message,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 旋转图片
     */
    suspend fun rotateImage(uri: Uri, degrees: Float): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val originalBitmap = loadBitmap(uri)
            val matrix = Matrix().apply {
                postRotate(degrees)
            }

            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )

            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            ProcessResult(
                success = true,
                bitmap = rotatedBitmap,
                processingTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ProcessResult(
                success = false,
                error = e.message,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 添加水印
     */
    suspend fun addWatermark(
        imageUri: Uri,
        watermarkText: String? = null,
        watermarkBitmap: Bitmap? = null,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        alpha: Int = 128,
        textSize: Float = 40f,
        textColor: Int = Color.WHITE
    ): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val originalBitmap = loadBitmap(imageUri)
            val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)

            when {
                watermarkText != null -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textColor
                        this.textSize = textSize
                        this.alpha = alpha
                        typeface = Typeface.DEFAULT_BOLD
                        setShadowLayer(1f, 0f, 1f, Color.BLACK)
                    }

                    val bounds = Rect()
                    paint.getTextBounds(watermarkText, 0, watermarkText.length, bounds)

                    val (x, y) = calculateWatermarkPosition(
                        originalBitmap.width,
                        originalBitmap.height,
                        bounds.width(),
                        bounds.height(),
                        position
                    )

                    canvas.drawText(watermarkText, x.toFloat(), y.toFloat(), paint)
                }
                watermarkBitmap != null -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.alpha = alpha
                    }

                    val (x, y) = calculateWatermarkPosition(
                        originalBitmap.width,
                        originalBitmap.height,
                        watermarkBitmap.width,
                        watermarkBitmap.height,
                        position
                    )

                    canvas.drawBitmap(watermarkBitmap, x.toFloat(), y.toFloat(), paint)
                }
            }

            originalBitmap.recycle()

            ProcessResult(
                success = true,
                bitmap = resultBitmap,
                processingTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ProcessResult(
                success = false,
                error = e.message,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 应用滤镜
     */
    suspend fun applyFilter(uri: Uri, filter: ImageFilter): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val originalBitmap = loadBitmap(uri)
            val resultBitmap = when (filter) {
                ImageFilter.GRAYSCALE -> applyGrayscale(originalBitmap)
                ImageFilter.SEPIA -> applySepia(originalBitmap)
                ImageFilter.BLUR -> applyBlur(originalBitmap, 15f)
                ImageFilter.SHARPEN -> applySharpen(originalBitmap)
                ImageFilter.BRIGHTNESS -> adjustBrightness(originalBitmap, 30f)
                ImageFilter.CONTRAST -> adjustContrast(originalBitmap, 1.5f)
                ImageFilter.VINTAGE -> applyVintage(originalBitmap)
                ImageFilter.NEGATIVE -> applyNegative(originalBitmap)
            }

            if (resultBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            ProcessResult(
                success = true,
                bitmap = resultBitmap,
                processingTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ProcessResult(
                success = false,
                error = e.message,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 转换图片格式
     */
    suspend fun convertFormat(
        uri: Uri,
        targetFormat: Bitmap.CompressFormat,
        quality: Int = JPEG_QUALITY_MEDIUM
    ): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val bitmap = loadBitmap(uri)
            val outputFile = File(
                context.cacheDir,
                "converted_${System.currentTimeMillis()}.${getFormatExtension(targetFormat)}"
            )

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(targetFormat, quality, out)
            }

            bitmap.recycle()

            ProcessResult(
                success = true,
                outputPath = outputFile.absolutePath,
                sizeBytes = outputFile.length(),
                processingTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ProcessResult(
                success = false,
                error = e.message,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * 图片转Base64
     */
    suspend fun imageToBase64(uri: Uri, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): String? =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmap(uri)
                val baos = ByteArrayOutputStream()
                bitmap.compress(format, 100, baos)
                val bytes = baos.toByteArray()
                bitmap.recycle()
                Base64.encodeToString(bytes, Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Base64转图片
     */
    suspend fun base64ToImage(base64String: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    // ========== 私有辅助方法 ==========

    private fun loadBitmap(uri: Uri, maxWidth: Int = MAX_TEXTURE_SIZE, maxHeight: Int = MAX_TEXTURE_SIZE): Bitmap {
        val options = BitmapFactory.Options()

        // 首先获取图片尺寸
        options.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        // 计算缩放比例
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        options.inJustDecodeBounds = false

        // 加载图片
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IOException("Failed to load bitmap")
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun calculateAspectRatioSize(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Pair<Int, Int> {
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        val targetAspectRatio = targetWidth.toFloat() / targetHeight.toFloat()

        return if (aspectRatio > targetAspectRatio) {
            // 原图更宽，以宽度为准
            Pair(targetWidth, (targetWidth / aspectRatio).toInt())
        } else {
            // 原图更高，以高度为准
            Pair((targetHeight * aspectRatio).toInt(), targetHeight)
        }
    }

    private fun calculateWatermarkPosition(
        imageWidth: Int,
        imageHeight: Int,
        watermarkWidth: Int,
        watermarkHeight: Int,
        position: WatermarkPosition
    ): Pair<Int, Int> {
        val padding = 20
        return when (position) {
            WatermarkPosition.TOP_LEFT -> Pair(padding, padding + watermarkHeight)
            WatermarkPosition.TOP_RIGHT -> Pair(imageWidth - watermarkWidth - padding, padding + watermarkHeight)
            WatermarkPosition.BOTTOM_LEFT -> Pair(padding, imageHeight - padding)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(imageWidth - watermarkWidth - padding, imageHeight - padding)
            WatermarkPosition.CENTER -> Pair(
                (imageWidth - watermarkWidth) / 2,
                (imageHeight + watermarkHeight) / 2
            )
        }
    }

    private fun getFormatExtension(format: Bitmap.CompressFormat): String {
        return when (format) {
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "jpg"
        }
    }

    private fun getExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun getExifMetadata(uri: Uri): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)

                // 获取常用EXIF数据
                exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { metadata["DateTime"] = it }
                exif.getAttribute(ExifInterface.TAG_MAKE)?.let { metadata["Make"] = it }
                exif.getAttribute(ExifInterface.TAG_MODEL)?.let { metadata["Model"] = it }
                exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.let { metadata["Latitude"] = it }
                exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.let { metadata["Longitude"] = it }
            }
        } catch (e: Exception) {
            // 忽略错误
        }
        return metadata
    }

    // ========== 滤镜实现 ==========

    private fun applyGrayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applySepia(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val sepiaMatrix = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(sepiaMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyBlur(source: Bitmap, radius: Float): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val rs = RenderScript.create(context)
            val input = Allocation.createFromBitmap(rs, source)
            val output = Allocation.createTyped(rs, input.type)
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            script.setRadius(radius.coerceIn(1f, 25f))
            script.setInput(input)
            script.forEach(output)
            output.copyTo(source)
            rs.destroy()
        }
        return source
    }

    private fun applySharpen(source: Bitmap): Bitmap {
        val result = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val rs = RenderScript.create(context)
            val input = Allocation.createFromBitmap(rs, source)
            val output = Allocation.createTyped(rs, input.type)
            val script = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
            val kernel = floatArrayOf(
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
            )
            script.setCoefficients(kernel)
            script.setInput(input)
            script.forEach(output)
            output.copyTo(result)
            rs.destroy()
        }
        return result
    }

    private fun adjustBrightness(source: Bitmap, brightness: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun adjustContrast(source: Bitmap, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val translate = (1f - contrast) / 2f * 255f
        val colorMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyVintage(source: Bitmap): Bitmap {
        // 组合多个效果：降低饱和度 + 暖色调 + 暗角
        var result = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)

        // 降低饱和度
        result = applySaturation(result, 0.6f)

        // 添加暖色调
        result = applyWarmTone(result)

        // 添加暗角效果
        result = applyVignette(result)

        return result
    }

    private fun applyNegative(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applySaturation(source: Bitmap, saturation: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyWarmTone(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyVignette(source: Bitmap): Bitmap {
        val result = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val centerX = source.width / 2f
        val centerY = source.height / 2f
        val radius = min(centerX, centerY) * 1.2f

        val paint = Paint().apply {
            shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(100, 0, 0, 0)),
                floatArrayOf(0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), paint)
        return result
    }

    enum class WatermarkPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
    }

    enum class ImageFilter {
        GRAYSCALE, SEPIA, BLUR, SHARPEN, BRIGHTNESS, CONTRAST, VINTAGE, NEGATIVE
    }
}