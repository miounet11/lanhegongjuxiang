package lanhe.media

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import kotlinx.coroutines.*
import lanhe.filesystem.LanheFileManager
import lanhe.filesystem.UniFile

/**
 * 蓝河助手多媒体渲染器
 * 集成图片、视频、音频、文本等多媒体文件预览功能
 */
class LanheMediaRenderer(
    private val context: Context
) {
    private val fileManager = LanheFileManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 渲染器组件
    private val imageRenderer = ImageRenderer(context)
    private val videoRenderer = VideoRenderer(context)
    private val audioRenderer = AudioRenderer(context)
    private val textRenderer = TextRenderer(context)
    private val documentRenderer = DocumentRenderer(context)

    /**
     * 预览文件（自动识别文件类型）
     */
    suspend fun previewFile(filePath: String, webView: WebView): PreviewResult = withContext(Dispatchers.Main) {
        try {
            val file = fileManager.getFile(filePath)
            val mimeType = file.mimeType

            when {
                mimeType?.startsWith("image/") == true -> {
                    previewImage(file, webView)
                }
                mimeType?.startsWith("video/") == true -> {
                    previewVideo(file, webView)
                }
                mimeType?.startsWith("audio/") == true -> {
                    previewAudio(file, webView)
                }
                mimeType?.startsWith("text/") == true || isTextFile(file) -> {
                    previewText(file, webView)
                }
                isDocumentFile(file) -> {
                    previewDocument(file, webView)
                }
                else -> {
                    PreviewResult.Unsupported(filePath, "Unsupported file type: $mimeType")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PreviewResult.Error(filePath, e.message ?: "Unknown error")
        }
    }

    /**
     * 图片预览
     */
    suspend fun previewImage(file: UniFile, webView: WebView): PreviewResult = withContext(Dispatchers.Main) {
        try {
            val imageInfo = imageRenderer.getImageInfo(file)
            val previewUrl = createPreviewUrl(file, "image")

            val jsCode = """
                lanheFileManager.showImagePreview({
                    url: '$previewUrl',
                    fileName: '${file.name}',
                    fileSize: ${file.size},
                    mimeType: '${file.mimeType}',
                    dimensions: { width: ${imageInfo.width}, height: ${imageInfo.height} },
                    metadata: ${imageInfo.toJson()},
                    actions: ['share', 'edit', 'set_wallpaper', 'details', 'delete']
                });
            """

            webView.evaluateJavascript(jsCode, null)
            PreviewResult.Success(file.path, "Image preview loaded")
        } catch (e: Exception) {
            PreviewResult.Error(file.path, "Failed to preview image: ${e.message}")
        }
    }

    /**
     * 视频预览
     */
    suspend fun previewVideo(file: UniFile, webView: WebView): PreviewResult = withContext(Dispatchers.Main) {
        try {
            val videoInfo = videoRenderer.getVideoInfo(file)
            val previewUrl = createPreviewUrl(file, "video")
            val thumbnailUrl = createThumbnailUrl(file)

            val jsCode = """
                lanheFileManager.showVideoPlayer({
                    url: '$previewUrl',
                    thumbnail: '$thumbnailUrl',
                    fileName: '${file.name}',
                    fileSize: ${file.size},
                    mimeType: '${file.mimeType}',
                    duration: ${videoInfo.duration},
                    resolution: { width: ${videoInfo.width}, height: ${videoInfo.height} },
                    metadata: ${videoInfo.toJson()},
                    controls: ['play', 'pause', 'fullscreen', 'seek', 'share', 'delete']
                });
            """

            webView.evaluateJavascript(jsCode, null)
            PreviewResult.Success(file.path, "Video player loaded")
        } catch (e: Exception) {
            PreviewResult.Error(file.path, "Failed to preview video: ${e.message}")
        }
    }

    /**
     * 音频预览
     */
    suspend fun previewAudio(file: UniFile, webView: WebView): PreviewResult = withContext(Dispatchers.Main) {
        try {
            val audioInfo = audioRenderer.getAudioInfo(file)
            val audioUrl = createPreviewUrl(file, "audio")
            val coverArtUrl = getCoverArtUrl(file)

            val jsCode = """
                lanheFileManager.showAudioPlayer({
                    url: '$audioUrl',
                    coverArt: '$coverArtUrl',
                    fileName: '${file.name}',
                    fileSize: ${file.size},
                    mimeType: '${file.mimeType}',
                    duration: ${audioInfo.duration},
                    metadata: ${audioInfo.toJson()},
                    controls: ['play', 'pause', 'seek', 'volume', 'share', 'delete']
                });
            """

            webView.evaluateJavascript(jsCode, null)
            PreviewResult.Success(file.path, "Audio player loaded")
        } catch (e: Exception) {
            PreviewResult.Error(file.path, "Failed to preview audio: ${e.message}")
        }
    }

    /**
     * 文本预览
     */
    suspend fun previewText(file: UniFile, webView: WebView): PreviewResult = withContext(Dispatchers.IO) {
        try {
            val textContent = textRenderer.getTextContent(file)
            val lineCount = textContent.lines().size
            val encoding = textRenderer.detectEncoding(file)

            withContext(Dispatchers.Main) {
                val jsCode = """
                    lanheFileManager.showTextViewer({
                        content: ${escapeJsString(textContent)},
                        fileName: '${file.name}',
                        fileSize: ${file.size},
                        mimeType: '${file.mimeType}',
                        encoding: '$encoding',
                        lineCount: $lineCount,
                        readOnly: ${!file.canWrite},
                        actions: ['edit', 'share', 'search', 'delete']
                    });
                """

                webView.evaluateJavascript(jsCode, null)
            }

            PreviewResult.Success(file.path, "Text viewer loaded")
        } catch (e: Exception) {
            PreviewResult.Error(file.path, "Failed to preview text: ${e.message}")
        }
    }

    /**
     * 文档预览
     */
    suspend fun previewDocument(file: UniFile, webView: WebView): PreviewResult = withContext(Dispatchers.Main) {
        try {
            val documentInfo = documentRenderer.getDocumentInfo(file)
            val previewUrl = createPreviewUrl(file, "document")

            val jsCode = """
                lanheFileManager.showDocumentViewer({
                    url: '$previewUrl',
                    fileName: '${file.name}',
                    fileSize: ${file.size},
                    mimeType: '${file.mimeType}',
                    pageCount: ${documentInfo.pageCount},
                    metadata: ${documentInfo.toJson()},
                    actions: ['share', 'export', 'print', 'delete']
                });
            """

            webView.evaluateJavascript(jsCode, null)
            PreviewResult.Success(file.path, "Document viewer loaded")
        } catch (e: Exception) {
            PreviewResult.Error(file.path, "Failed to preview document: ${e.message}")
        }
    }

    /**
     * 获取支持的多媒体格式
     */
    fun getSupportedFormats(): SupportedFormats {
        return SupportedFormats(
            images = imageRenderer.supportedFormats,
            videos = videoRenderer.supportedFormats,
            audio = audioRenderer.supportedFormats,
            text = textRenderer.supportedFormats,
            documents = documentRenderer.supportedFormats
        )
    }

    /**
     * 检查是否为文本文件
     */
    private fun isTextFile(file: UniFile): Boolean {
        val textExtensions = setOf("txt", "md", "log", "conf", "config", "ini", "xml", "json", "csv", "py", "js", "html", "css", "kotlin", "java", "gradle", "properties")
        return file.extension?.lowercase() in textExtensions
    }

    /**
     * 检查是否为文档文件
     */
    private fun isDocumentFile(file: UniFile): Boolean {
        val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp")
        return file.extension?.lowercase() in documentExtensions
    }

    /**
     * 创建预览URL
     */
    private fun createPreviewUrl(file: UniFile, type: String): String {
        return "lanhe://preview/$type?path=${Uri.encode(file.path)}"
    }

    /**
     * 创建缩略图URL
     */
    private fun createThumbnailUrl(file: UniFile): String {
        return "lanhe://thumbnail?path=${Uri.encode(file.path)}"
    }

    /**
     * 获取封面艺术URL
     */
    private fun getCoverArtUrl(file: UniFile): String? {
        return if (file.mimeType?.startsWith("audio/") == true) {
            "lanhe://coverart?path=${Uri.encode(file.path)}"
        } else null
    }

    /**
     * 转义JavaScript字符串
     */
    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
    }
}

/**
 * 预览结果
 */
sealed class PreviewResult {
    data class Success(val path: String, val message: String) : PreviewResult()
    data class Error(val path: String, val error: String) : PreviewResult()
    data class Unsupported(val path: String, val reason: String) : PreviewResult()
}

/**
 * 支持的格式
 */
data class SupportedFormats(
    val images: List<String>,
    val videos: List<String>,
    val audio: List<String>,
    val text: List<String>,
    val documents: List<String>
) {
    val all: List<String> by lazy { images + videos + audio + text + documents }
}