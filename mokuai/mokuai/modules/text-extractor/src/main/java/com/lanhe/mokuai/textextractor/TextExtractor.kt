package com.lanhe.mokuai.textextractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// import org.apache.poi.xwpf.extractor.XWPFWordExtractor
// import org.apache.poi.xwpf.usermodel.XWPFDocument
// import org.apache.poi.xssf.usermodel.XSSFWorkbook
// import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.*
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

/**
 * 文本提取器 - 从各种文件格式中提取文本内容
 * 注意：Word和Excel格式支持被禁用以保持Android 7.0+兼容性
 */
class TextExtractor(private val context: Context) {

    data class ExtractionResult(
        val success: Boolean,
        val text: String = "",
        val metadata: Map<String, String> = emptyMap(),
        val pageCount: Int = 0,
        val wordCount: Int = 0,
        val error: String? = null
    )

    /**
     * 从文件URI提取文本
     */
    suspend fun extractFromUri(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            val mimeType = getMimeType(uri)
            val fileName = getFileName(uri)

            when {
                mimeType?.startsWith("text/") == true -> extractFromTextFile(uri)
                mimeType == "application/pdf" -> extractFromPdf(uri)
                mimeType in listOf(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword"
                ) -> {
                    // Word extraction disabled for minSdk 24 compatibility
                    ExtractionResult(
                        success = false,
                        error = "Word document extraction requires minSdk 26+ (POI library limitation)"
                    )
                }
                mimeType in listOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                ) -> {
                    // Excel extraction disabled for minSdk 24 compatibility
                    ExtractionResult(
                        success = false,
                        error = "Excel extraction requires minSdk 26+ (POI library limitation)"
                    )
                }
                mimeType == "application/epub+zip" -> extractFromEpub(uri)
                mimeType?.startsWith("image/") == true -> extractFromImage(uri)
                fileName?.endsWith(".md") == true -> extractFromMarkdown(uri)
                fileName?.endsWith(".json") == true -> extractFromJson(uri)
                fileName?.endsWith(".xml") == true -> extractFromXml(uri)
                else -> ExtractionResult(
                    success = false,
                    error = "Unsupported file type: $mimeType"
                )
            }
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }

    /**
     * 从纯文本文件提取
     */
    private suspend fun extractFromTextFile(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: ""

            val wordCount = text.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
            val lineCount = text.lines().size

            ExtractionResult(
                success = true,
                text = text,
                metadata = mapOf(
                    "type" to "text",
                    "lines" to lineCount.toString(),
                    "characters" to text.length.toString()
                ),
                wordCount = wordCount
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to read text file: ${e.message}"
            )
        }
    }

    /**
     * 从PDF提取文本
     */
    private suspend fun extractFromPdf(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            parcelFileDescriptor?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                val textBuilder = StringBuilder()

                for (i in 0 until pageCount) {
                    renderer.openPage(i).use { page ->
                        // 注意：PdfRenderer主要用于渲染，不能直接提取文本
                        // 这里需要使用OCR或其他PDF库
                        textBuilder.append("Page ${i + 1}:\n")
                        textBuilder.append("[PDF页面内容需要OCR或专门的PDF文本提取库]\n\n")
                    }
                }

                renderer.close()

                ExtractionResult(
                    success = true,
                    text = textBuilder.toString(),
                    metadata = mapOf(
                        "type" to "pdf",
                        "pages" to pageCount.toString()
                    ),
                    pageCount = pageCount,
                    wordCount = textBuilder.toString().split(Regex("\\s+")).size
                )
            } ?: ExtractionResult(
                success = false,
                error = "Failed to open PDF file"
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to extract from PDF: ${e.message}"
            )
        }
    }

    /**
     * 从Word文档提取文本 - 禁用以保持minSdk 24兼容性
     */
    /* Disabled: POI requires minSdk 26+
    private suspend fun extractFromWord(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val text = when {
                    uri.toString().endsWith(".docx") -> {
                        val document = XWPFDocument(inputStream)
                        val extractor = XWPFWordExtractor(document)
                        val text = extractor.text
                        extractor.close()
                        document.close()
                        text
                    }
                    else -> {
                        // .doc 格式需要不同的处理
                        "[旧版Word文档格式，需要POI HWPF库支持]"
                    }
                }

                val wordCount = text.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

                ExtractionResult(
                    success = true,
                    text = text,
                    metadata = mapOf(
                        "type" to "word",
                        "format" to if (uri.toString().endsWith(".docx")) "docx" else "doc"
                    ),
                    wordCount = wordCount
                )
            } ?: ExtractionResult(
                success = false,
                error = "Failed to open Word document"
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to extract from Word: ${e.message}"
            )
        }
    }
    */

    /**
     * 从Excel提取文本 - 禁用以保持minSdk 24兼容性
     */
    /* Disabled: POI requires minSdk 26+
    private suspend fun extractFromExcel(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = when {
                    uri.toString().endsWith(".xlsx") -> XSSFWorkbook(inputStream)
                    else -> HSSFWorkbook(inputStream)
                }

                val textBuilder = StringBuilder()
                val sheetCount = workbook.numberOfSheets

                for (i in 0 until sheetCount) {
                    val sheet = workbook.getSheetAt(i)
                    textBuilder.append("Sheet: ${sheet.sheetName}\n")

                    sheet.forEach { row ->
                        row.forEach { cell ->
                            textBuilder.append("${cell.toString()}\t")
                        }
                        textBuilder.append("\n")
                    }
                    textBuilder.append("\n")
                }

                workbook.close()

                ExtractionResult(
                    success = true,
                    text = textBuilder.toString(),
                    metadata = mapOf(
                        "type" to "excel",
                        "sheets" to sheetCount.toString(),
                        "format" to if (uri.toString().endsWith(".xlsx")) "xlsx" else "xls"
                    ),
                    wordCount = textBuilder.toString().split(Regex("\\s+")).size
                )
            } ?: ExtractionResult(
                success = false,
                error = "Failed to open Excel file"
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to extract from Excel: ${e.message}"
            )
        }
    }
    */

    /**
     * 从EPUB电子书提取文本
     */
    private suspend fun extractFromEpub(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_epub.epub")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val textBuilder = StringBuilder()
            var chapterCount = 0

            ZipFile(tempFile).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.endsWith(".html") || it.name.endsWith(".xhtml") }
                    .forEach { entry ->
                        chapterCount++
                        val content = zip.getInputStream(entry).bufferedReader().readText()
                        val cleanText = content
                            .replace(Regex("<[^>]+>"), "") // Remove HTML tags
                            .replace(Regex("\\s+"), " ")   // Normalize whitespace
                            .trim()

                        textBuilder.append("Chapter $chapterCount:\n")
                        textBuilder.append(cleanText)
                        textBuilder.append("\n\n")
                    }
            }

            tempFile.delete()

            ExtractionResult(
                success = true,
                text = textBuilder.toString(),
                metadata = mapOf(
                    "type" to "epub",
                    "chapters" to chapterCount.toString()
                ),
                pageCount = chapterCount,
                wordCount = textBuilder.toString().split(Regex("\\s+")).size
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to extract from EPUB: ${e.message}"
            )
        }
    }

    /**
     * 从图片提取文本（需要OCR）
     */
    private suspend fun extractFromImage(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            // 这里需要集成OCR库，如ML Kit或Tesseract
            ExtractionResult(
                success = true,
                text = "[图片文本提取需要OCR库支持]",
                metadata = mapOf(
                    "type" to "image",
                    "mimeType" to (getMimeType(uri) ?: "unknown")
                )
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to extract from image: ${e.message}"
            )
        }
    }

    /**
     * 从Markdown文件提取纯文本
     */
    private suspend fun extractFromMarkdown(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            val markdown = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: ""

            // 移除Markdown标记
            val plainText = markdown
                .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "") // Headers
                .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // Bold
                .replace(Regex("\\*([^*]+)\\*"), "$1") // Italic
                .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1") // Links
                .replace(Regex("^[*+-]\\s+", RegexOption.MULTILINE), "") // Lists
                .replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "") // Numbered lists
                .replace(Regex("`([^`]+)`"), "$1") // Inline code
                .replace(Regex("```[\\s\\S]*?```"), "[Code Block]") // Code blocks

            val wordCount = plainText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

            ExtractionResult(
                success = true,
                text = plainText,
                metadata = mapOf(
                    "type" to "markdown",
                    "originalLength" to markdown.length.toString()
                ),
                wordCount = wordCount
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to extract from Markdown: ${e.message}"
            )
        }
    }

    /**
     * 从JSON文件提取文本
     */
    private suspend fun extractFromJson(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: ""

            // 提取JSON中的所有字符串值
            val textValues = mutableListOf<String>()
            extractJsonStrings(jsonText, textValues)

            val extractedText = textValues.joinToString("\n")
            val wordCount = extractedText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

            ExtractionResult(
                success = true,
                text = extractedText,
                metadata = mapOf(
                    "type" to "json",
                    "stringCount" to textValues.size.toString()
                ),
                wordCount = wordCount
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to extract from JSON: ${e.message}"
            )
        }
    }

    /**
     * 从XML文件提取文本
     */
    private suspend fun extractFromXml(uri: Uri): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val dbFactory = DocumentBuilderFactory.newInstance()
                val dBuilder = dbFactory.newDocumentBuilder()
                val doc = dBuilder.parse(inputStream)
                doc.documentElement.normalize()

                val textContent = extractTextFromNode(doc.documentElement)
                val wordCount = textContent.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

                ExtractionResult(
                    success = true,
                    text = textContent,
                    metadata = mapOf(
                        "type" to "xml",
                        "rootElement" to doc.documentElement.nodeName
                    ),
                    wordCount = wordCount
                )
            } ?: ExtractionResult(
                success = false,
                error = "Failed to open XML file"
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "Failed to extract from XML: ${e.message}"
            )
        }
    }

    /**
     * 递归提取XML节点中的文本
     */
    private fun extractTextFromNode(node: org.w3c.dom.Node): String {
        val textBuilder = StringBuilder()

        if (node.nodeType == org.w3c.dom.Node.TEXT_NODE) {
            textBuilder.append(node.textContent.trim())
        }

        val children = node.childNodes
        for (i in 0 until children.length) {
            val childText = extractTextFromNode(children.item(i))
            if (childText.isNotEmpty()) {
                if (textBuilder.isNotEmpty()) {
                    textBuilder.append(" ")
                }
                textBuilder.append(childText)
            }
        }

        return textBuilder.toString()
    }

    /**
     * 从JSON字符串中提取所有文本值
     */
    private fun extractJsonStrings(json: String, result: MutableList<String>) {
        // 简单的正则提取，实际应用应使用JSON解析库
        val pattern = "\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\""
        val regex = Regex(pattern)
        regex.findAll(json).forEach { match ->
            val value = match.groupValues[1]
            if (value.isNotEmpty() && !value.matches(Regex("^\\d+$"))) {
                result.add(value)
            }
        }
    }

    /**
     * 获取文件MIME类型
     */
    private fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri) ?: run {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }

    /**
     * 获取文件名
     */
    private fun getFileName(uri: Uri): String? {
        return uri.lastPathSegment
    }

    /**
     * 批量提取文本
     */
    suspend fun extractBatch(uris: List<Uri>): List<ExtractionResult> = withContext(Dispatchers.IO) {
        uris.map { uri ->
            extractFromUri(uri)
        }
    }

    /**
     * 搜索提取的文本
     */
    fun searchInText(text: String, query: String, caseSensitive: Boolean = false): List<SearchMatch> {
        val results = mutableListOf<SearchMatch>()
        val pattern = if (caseSensitive) query else query.lowercase()
        val searchText = if (caseSensitive) text else text.lowercase()

        var index = 0
        while (index < searchText.length) {
            val foundIndex = searchText.indexOf(pattern, index)
            if (foundIndex == -1) break

            // 获取上下文
            val contextStart = maxOf(0, foundIndex - 50)
            val contextEnd = minOf(text.length, foundIndex + pattern.length + 50)
            val context = text.substring(contextStart, contextEnd)

            results.add(SearchMatch(
                position = foundIndex,
                matchedText = text.substring(foundIndex, foundIndex + pattern.length),
                context = context,
                lineNumber = text.substring(0, foundIndex).count { it == '\n' } + 1
            ))

            index = foundIndex + 1
        }

        return results
    }

    data class SearchMatch(
        val position: Int,
        val matchedText: String,
        val context: String,
        val lineNumber: Int
    )
}