package com.phenomena.study.data

import android.content.Context
import android.net.Uri
import android.text.Html
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/** Import ONLY user-selected materials. Scanned PDFs without a text layer require external OCR. */
object DocumentImporter {
    data class Result(val title: String, val content: String, val format: String)
    private const val MAX_IMPORT = 18_000_000
    private const val MAX_DOC = 1_500_000
    fun import(context: Context, uri: Uri): Result {
        val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "Imported reading"
        val type = name.substringAfterLast('.', "txt").lowercase()
        val data = context.contentResolver.openInputStream(uri)?.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (out.size() + count > MAX_IMPORT) throw IllegalArgumentException("File exceeds the 18 MB import limit")
                out.write(buffer, 0, count)
            }
            out.toByteArray()
        } ?: error("Could not open selected file")
        val extracted = when (type) {
            "epub" -> parseEpub(data)
            "pdf" -> parsePdf(context, data)
            "html", "htm" -> htmlToText(data.toString(Charsets.UTF_8))
            else -> data.toString(Charsets.UTF_8)
        }.trim()
        if (extracted.isBlank()) throw IllegalArgumentException("No readable text found (scanned PDFs require OCR)")
        return Result(name.substringBeforeLast('.'), extracted.take(MAX_DOC), type)
    }
    private fun htmlToText(html: String): String {
        val clean = html.replace(Regex("(?is)<(script|style|nav)[^>]*>.*?</\\1>"), "")
            .replace(Regex("(?i)</(p|h[1-6]|div|li|blockquote|section)>"), "\n\n")
        return Html.fromHtml(clean, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("[ \\t]+"), " ").replace(Regex("\n{3,}"), "\n\n").trim()
    }
    private fun parsePdf(context: Context, bytes: ByteArray): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        return PDDocument.load(bytes).use { pdf ->
            if (pdf.numberOfPages > 600) throw IllegalArgumentException("PDF too long; limit 600 pages")
            PDFTextStripper().getText(pdf)
        }
    }
    private fun parseEpub(bytes: ByteArray): String {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val item = zip.nextEntry ?: break
                if (!item.isDirectory) {
                    val out = ByteArrayOutputStream()
                    val tmp = ByteArray(8192)
                    while (true) {
                        val n = zip.read(tmp)
                        if (n < 0) break
                        if (out.size() + n > 2_000_000) throw IllegalArgumentException("EPUB entry exceeds limit")
                        out.write(tmp, 0, n)
                    }
                    entries[item.name.replace('\\', '/')] = out.toByteArray()
                }
                if (entries.size > 3000) throw IllegalArgumentException("EPUB contains too many files")
            }
        }
        // Honor OPF manifest and spine order if present (not ZIP entry order).
        val opfPath = entries.keys.firstOrNull { it.endsWith(".opf", ignoreCase = true) }
        val order = if (opfPath != null) {
            val opf = entries.getValue(opfPath).toString(Charsets.UTF_8)
            val base = opfPath.substringBeforeLast('/', "")
            val manifest = Regex("<item\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(opf).mapNotNull { m ->
                val tag = m.value
                val id = Regex("\\bid=[\"']([^\"']+)").find(tag)?.groupValues?.get(1)
                val href = Regex("\\bhref=[\"']([^\"']+)").find(tag)?.groupValues?.get(1)
                if (id != null && href != null) id to (if (base.isEmpty()) href else "$base/$href") else null
            }.toMap()
            Regex("<itemref\\b[^>]*idref=[\"']([^\"']+)", RegexOption.IGNORE_CASE).findAll(opf)
                .mapNotNull { manifest[it.groupValues[1]] }.toList()
        } else emptyList()
        val pages = (if (order.isNotEmpty()) order else entries.keys.filter { it.endsWith(".xhtml") || it.endsWith(".html") })
            .mapNotNull { entries[it]?.toString(Charsets.UTF_8) }
        return pages.joinToString("\n\n") { htmlToText(it) }
    }
}
