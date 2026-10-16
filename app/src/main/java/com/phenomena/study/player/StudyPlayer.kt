package com.phenomena.study.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.phenomena.study.model.Beat
import java.util.Locale

/** Android TTS for lesson narration; Media3 for user-selected prerecorded audio. */
class StudyPlayer(private val context: Context) {
    var ready by mutableStateOf(false); private set
    var speaking by mutableStateOf(false); private set
    var mediaPlaying by mutableStateOf(false); private set
    var autoAdvance by mutableStateOf(true)
    var speechRate by mutableFloatStateOf(0.95f)
    var accent by mutableStateOf("British")
    var status by mutableStateOf("Preparing Android text-to-speech…"); private set
    var beatIndex by mutableIntStateOf(0); private set
    var activeText by mutableStateOf(""); private set
    private val main = Handler(Looper.getMainLooper())
    private val media: ExoPlayer = ExoPlayer.Builder(context).build()
    private val tts: TextToSpeech
    private var segments: List<Beat> = emptyList()

    init {
        media.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { mediaPlaying = isPlaying }
        })
        tts = TextToSpeech(context) { result ->
            main.post {
                ready = result == TextToSpeech.SUCCESS
                status = if (ready) "Narration ready" else "Install or enable an Android TTS engine to listen"
                if (ready) configureTts()
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = main.post { speaking = true; status = "Narrating" }.let { Unit }
            override fun onDone(id: String?) = main.post {
                speaking = false
                if (autoAdvance && beatIndex + 1 < segments.size) {
                    beatIndex += 1
                    play()
                } else status = "Narration finished"
            }.let { Unit }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) = main.post { speaking = false; status = "Speech engine error" }.let { Unit }
        })
    }
    private fun configureTts() {
        if (!ready) return
        tts.language = if (accent == "British") Locale.UK else Locale.US
        tts.setSpeechRate(speechRate)
    }
    fun setVoice(value: String) { accent = value; configureTts() }
    fun setRate(value: Float) { speechRate = value; configureTts() }
    fun setSegments(items: List<Beat>) {
        if (segments != items) {
            stop()
            segments = items
            beatIndex = 0
            activeText = items.firstOrNull()?.content.orEmpty()
        }
    }
    fun play() {
        if (!ready) { status = "Android text-to-speech is unavailable"; return }
        if (segments.isEmpty()) return
        media.pause(); configureTts()
        val content = segments.getOrNull(beatIndex)?.content.orEmpty().take(TextToSpeech.getMaxSpeechInputLength() - 100)
        activeText = content
        val outcome = tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, "phenomena_${System.nanoTime()}")
        status = if (outcome == TextToSpeech.SUCCESS) "Narrating segment ${beatIndex + 1}/${segments.size}" else "Could not start speech"
    }
    fun toggleSpeech() { if (speaking) { tts.stop(); speaking = false; status = "Stopped · Replay to resume from segment start" } else play() }
    fun jump(delta: Int) { if (segments.isNotEmpty()) { beatIndex = (beatIndex + delta).coerceIn(0, segments.lastIndex); play() } }
    fun jumpTo(index: Int) { if (index in segments.indices) { beatIndex = index; play() } }
    fun pickAudio(uri: Uri) {
        tts.stop(); speaking = false
        media.setMediaItem(MediaItem.fromUri(uri))
        media.prepare(); media.play()
        status = "Playing imported audio with Media3"
    }
    fun toggleAudio() { if (media.isPlaying) media.pause() else media.play() }
    fun stop() { tts.stop(); media.pause(); speaking = false }
    fun release() { tts.stop(); tts.shutdown(); media.release() }
}

fun toSpeechChunks(text: String, maxChunk: Int = 2500): List<Beat> {
    val chunks = mutableListOf<String>()
    val paragraphs = text.replace("\r", "").split(Regex("\n{2,}"))
    var cur = ""
    for (paragraph in paragraphs) {
        for (part in paragraph.chunked(maxChunk)) {
            if (cur.length + part.length > maxChunk && cur.isNotBlank()) {
                chunks += cur.trim(); cur = ""
            }
            cur += part + "\n\n"
        }
    }
    if (cur.isNotBlank()) chunks += cur.trim()
    return chunks.mapIndexed { index, chunk -> Beat("Passage ${index + 1}", chunk, "Imported text") }
}
