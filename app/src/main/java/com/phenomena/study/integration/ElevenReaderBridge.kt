package com.phenomena.study.integration

import android.app.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** ElevenReader's premium voices belong to ElevenReader, not PHENOMENA.
 * This class uses the Android document/share contract, NEVER the proprietary
 * audio engine, unauthorized APK internals, inaccessible private files or APIs.
 *
 * The user explicitly taps "Open in ElevenReader"; one UTF-8 text file is
 * temporarily exported with READ-only grant. No app data or account access.
 */
object ElevenReaderBridge {
    private const val READER_PACKAGE = "io.elevenlabs.readerapp"
    data class Export(val title:String,val uri:Uri,val characters:Int)
    enum class Destination { ELEVENREADER, SYSTEM_SHARE_CHOOSER }

    fun prepare(context:Context,title:String,transcript:String):Export {
        require(title.isNotBlank() && transcript.isNotBlank()) {
            "A named lecture with a nonempty transcript must be selected."
        }
        val folder=File(context.cacheDir,"elevenreader-exports").apply{mkdirs()}
        require(folder.isDirectory) {"The temporary export folder could not be created."}
        // Only the last 6 exports are retained to avoid unbounded accumulation.
        folder.listFiles()?.sortedByDescending{it.lastModified()}?.drop(5)?.forEach{it.delete()}
        val safeTitle=title.replace(Regex("[^a-zA-Z0-9 -]"),"").take(52).trim()
            .ifBlank{"phenomena_lecture"}.replace(' ','_')
        val file=File(folder,"${safeTitle}_${System.currentTimeMillis()}.txt")
        val body="# $title\n\n$transcript\n"
        file.writeText(body,Charsets.UTF_8)
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.exportprovider",file)
        return Export(title,uri,body.length)
    }

    fun open(context:Context,export:Export):Destination {
        fun intent():Intent=Intent(Intent.ACTION_SEND).apply {
            type="text/plain"
            putExtra(Intent.EXTRA_STREAM,export.uri)
            putExtra(Intent.EXTRA_TITLE,export.title)
            clipData=ClipData.newRawUri(export.title,export.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Prefer the reader only when the OS actually accepts the SEND intent.
        return try {
            context.startActivity(intent().setPackage(READER_PACKAGE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            Destination.ELEVENREADER
        } catch (_:ActivityNotFoundException) {
            context.startActivity(Intent.createChooser(intent(),
                "Open PHENOMENA lecture in ElevenReader or another reader").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            Destination.SYSTEM_SHARE_CHOOSER
        }
    }
}
