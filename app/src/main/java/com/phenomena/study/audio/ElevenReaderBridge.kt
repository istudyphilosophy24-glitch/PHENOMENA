package com.phenomena.study.audio

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

/**
 * ElevenReader uses its own licensed voices. This is a lawful Android sharing
 * integration, NOT a reconstruction or embedding of ElevenReader's private API.
 *
 * If the vendor ever publishes a TextToSpeechService, the normal Android engine
 * API can discover it and PHENOMENA will use it (see StudyPlayer).
 *
 * Full transcripts go through a content:// FileProvider rather than Binder
 * EXTRA_TEXT; the latter can fail on long academic lectures.
 */
object ElevenReaderBridge {
    const val PACKAGE = "io.elevenlabs.readerapp"

    fun isAndroidTtsEngine(context: Context): Boolean =
        context.packageManager.queryIntentServices(
            Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),0
        ).any { it.serviceInfo?.packageName==PACKAGE }

    fun sendLecture(context: Context,title: String,transcript: String): Boolean {
        if (transcript.isBlank()) {
            Toast.makeText(context,"There is no transcript to share.",Toast.LENGTH_LONG).show()
            return false
        }
        return try {
            val dir=File(context.cacheDir,"lecture_shares").apply { mkdirs() }
            val name=MessageDigest.getInstance("SHA-256").digest(
                (title+transcript.length).toByteArray()).take(7).joinToString("") {
                "%02x".format(it)
            }
            val path=File(dir,"phenomena_$name.txt")
            // The exported document is educational commentary, not a licensed
            // translation unless imported independently by the user.
            path.writeText("$title\nPHENOMENA — educational listening transcript\n\n$transcript",Charsets.UTF_8)
            val uri=FileProvider.getUriForFile(context,
                "${context.packageName}.shared",path)
            val request=Intent(Intent.ACTION_SEND).apply {
                type="text/plain"
                setPackage(PACKAGE)
                putExtra(Intent.EXTRA_STREAM,uri)
                putExtra(Intent.EXTRA_TITLE,title)
                if(transcript.length<18000) putExtra(Intent.EXTRA_TEXT,transcript)
                clipData=ClipData.newRawUri(title,uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(request)
                true
            } catch (_:ActivityNotFoundException) {
                val generic=Intent(request).apply { setPackage(null) }
                context.startActivity(Intent.createChooser(generic,
                    "Open lecture in ElevenReader or another reader")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                Toast.makeText(context,"Select ElevenReader in the sharing menu if available.",
                    Toast.LENGTH_LONG).show()
                true
            }
        } catch (error:Exception) {
            Toast.makeText(context,"Unable to share the full transcript: ${error.localizedMessage}",
                Toast.LENGTH_LONG).show()
            false
        }
    }
}
