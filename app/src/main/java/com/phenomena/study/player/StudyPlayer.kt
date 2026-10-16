package com.phenomena.study.player

import android.content.Context
import android.content.Intent
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


data class SpeechEngineOption(val id: String, val label: String)
data class SpeechVoiceOption(val id: String, val label: String)

/**
 * Device-installed TTS engine and voice selection, modeled on SOPHIA.
 * A reader app can be used here only if it implements Android TextToSpeechService.
 */
class StudyPlayer(private val context: Context) {
    private val prefs = context.getSharedPreferences("phenomena_narrator_v12", Context.MODE_PRIVATE)
    val engines = mutableStateListOf<SpeechEngineOption>()
    val voices = mutableStateListOf<SpeechVoiceOption>()
    var engineId by mutableStateOf(prefs.getString("engine", "").orEmpty()); private set
    var voiceId by mutableStateOf(prefs.getString("voice", "").orEmpty()); private set
    var ready by mutableStateOf(false); private set
    var speaking by mutableStateOf(false); private set
    var mediaPlaying by mutableStateOf(false); private set
    var autoAdvance by mutableStateOf(true)
    var speechRate by mutableFloatStateOf(prefs.getFloat("rate", 0.95f))
    var status by mutableStateOf("Discovering installed Android speech engines…"); private set
    var beatIndex by mutableIntStateOf(0); private set
    var activeText by mutableStateOf(""); private set

    private val main = Handler(Looper.getMainLooper())
    private val media = ExoPlayer.Builder(context).build()
    private var tts: TextToSpeech? = null
    private var generation = 0
    private var activeUtteranceId: String? = null
    private var previewing = false
    private var segments: List<Beat> = emptyList()

    init {
        media.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { mediaPlaying = isPlaying }
        })
        discoverEngines()
        initializeTts()
    }

    private fun discoverEngines() {
        val candidates = context.packageManager.queryIntentServices(
            Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0
        )
        val all = candidates.mapNotNull { info ->
            val service = info.serviceInfo ?: return@mapNotNull null
            SpeechEngineOption(service.packageName, info.loadLabel(context.packageManager)?.toString() ?: service.packageName)
        }.distinctBy { it.id }.sortedBy { it.label.lowercase(Locale.ROOT) }
        engines.clear()
        engines.add(SpeechEngineOption("", "Android system default"))
        engines.addAll(all)
        if (engineId.isNotBlank() && all.none { it.id == engineId }) {
            // Keep saved preference visible even when the package was disabled or uninstalled.
            engines.add(SpeechEngineOption(engineId, "Unavailable: $engineId"))
        }
    }

    fun refreshEngines() {
        discoverEngines()
        initializeTts()
    }

    fun chooseEngine(id: String) {
        if (id == engineId && ready) return
        engineId = id
        voiceId = ""
        prefs.edit().putString("engine", id).remove("voice").apply()
        initializeTts()
    }

    fun chooseVoice(id: String) {
        if (!ready) return
        stop()
        val speaker = tts ?: return
        if (id.isBlank()) {
            voiceId = ""
            prefs.edit().remove("voice").apply()
            status = "Engine default voice selected. Choose an explicit voice to override it."
            // Restore the engine's locale preference through the default installed language.
            speaker.language = Locale.getDefault()
            return
        }
        val selected = speaker.voices?.firstOrNull { it.name == id } ?: run {
            status = "Voice unavailable; choose another installed voice"
            return
        }
        speaker.voice = selected
        voiceId = id
        prefs.edit().putString("voice", id).apply()
        status = "Selected: ${selected.locale.displayName} · ${selected.name}"
    }

    private fun initializeTts() {
        val expected = ++generation
        stop()
        ready = false
        voices.clear()
        tts?.shutdown()
        tts = null
        status = "Connecting to speech engine…"
        val init = TextToSpeech.OnInitListener { result ->
            main.post {
                if (expected != generation) return@post
                val speaker = tts
                if (result != TextToSpeech.SUCCESS || speaker == null) {
                    status = "Speech engine not available. Select Android system default."
                    ready = false
                    return@post
                }
                val available = speaker.voices.orEmpty().sortedWith(
                    compareBy({ it.locale.toLanguageTag() }, { it.name })
                )
                voices.clear()
                voices.addAll(available.map { voice ->
                    SpeechVoiceOption(voice.name, "${voice.locale.displayName} · ${voice.name}")
                })
                val chosen = available.firstOrNull { it.name == voiceId }
                if (chosen != null) speaker.voice = chosen
                else if (voiceId.isNotBlank()) {
                    voiceId = ""
                    prefs.edit().remove("voice").apply()
                }
                speaker.setSpeechRate(speechRate)
                speaker.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        main.post {
                            if (expected == generation && utteranceId == activeUtteranceId) {
                                speaking = true
                                status = if (previewing) "Previewing voice" else "Narrating chapter"
                            }
                        }
                    }
                    override fun onDone(utteranceId: String?) {
                        main.post {
                            if (expected != generation || utteranceId != activeUtteranceId) return@post
                            activeUtteranceId = null
                            speaking = false
                            if (previewing) {
                                previewing = false
                                status = "Voice preview complete"
                            } else if (autoAdvance && beatIndex + 1 < segments.size) {
                                beatIndex++
                                play()
                            } else status = "Narration finished"
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        main.post {
                            if (expected == generation && utteranceId == activeUtteranceId) {
                                activeUtteranceId = null
                                speaking = false
                                status = "Selected speech engine could not narrate this passage"
                            }
                        }
                    }
                })
                ready = true
                status = "Ready · ${voices.size} installed voices"
            }
        }
        tts = if (engineId.isBlank()) TextToSpeech(context, init)
        else TextToSpeech(context, init, engineId)
    }

    fun setRate(value: Float) {
        speechRate = value
        prefs.edit().putFloat("rate", value).apply()
        tts?.setSpeechRate(value)
    }
    fun setSegments(items: List<Beat>) {
        if (segments != items) {
            stop()
            segments = items
            beatIndex = 0
            activeText = items.firstOrNull()?.content.orEmpty()
        }
    }
    private fun speakUtterance(text: String, preview: Boolean = false) {
        val speaker = tts
        if (!ready || speaker == null) {
            status = "Choose an installed speech engine before playing"
            return
        }
        if (text.isBlank()) return
        if (text.length >= TextToSpeech.getMaxSpeechInputLength()) {
            status = "Speech segment is too long for the installed engine"
            return
        }
        media.pause()
        activeUtteranceId = null
        speaker.stop()
        speaking = false
        previewing = preview
        speaker.setSpeechRate(speechRate)
        val id = "phenomena_${System.nanoTime()}"
        activeUtteranceId = id
        val outcome = speaker.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (outcome != TextToSpeech.SUCCESS) {
            activeUtteranceId = null
            previewing = false
            status = "Could not start narration with this voice"
        } else status = if (preview) "Starting voice preview" else "Narrating ${beatIndex + 1}/${segments.size}"
    }
    fun previewVoice() {
        speakUtterance(
            "Welcome to PHENOMENA. Philosophy begins with the patient description of lived experience.",
            preview = true
        )
    }
    fun play() {
        if (segments.isEmpty()) return
        activeText = segments.getOrNull(beatIndex)?.content.orEmpty()
        speakUtterance(activeText)
    }
    fun toggleSpeech() {
        if (speaking || activeUtteranceId != null) {
            stop()
            status = "Stopped · Replay starts at the beginning of this chapter"
        } else play()
    }
    fun jump(delta: Int) {
        if (segments.isNotEmpty()) {
            beatIndex = (beatIndex + delta).coerceIn(0, segments.lastIndex)
            play()
        }
    }
    fun jumpTo(index: Int) {
        if (index in segments.indices) {
            beatIndex = index
            play()
        }
    }
    fun pickAudio(uri: Uri) {
        stop()
        media.setMediaItem(MediaItem.fromUri(uri))
        media.prepare()
        media.play()
        status = "Playing imported audio"
    }
    fun toggleAudio() { if (media.isPlaying) media.pause() else media.play() }
    fun stop() {
        activeUtteranceId = null
        previewing = false
        tts?.stop()
        media.pause()
        speaking = false
    }
    fun release() {
        ++generation
        stop()
        tts?.shutdown()
        tts = null
        media.release()
        ready = false
    }
}

fun toSpeechChunks(text: String, maxChunk: Int = 2500): List<Beat> {
    // Keep chapter boundaries within the Android TTS input limit without
    // chopping ordinary words at arbitrary character offsets.
    val safeLimit = maxChunk.coerceIn(300, 2800)
    return speechSafeChapter(Beat("Reading passage", text, "Study reading"), safeLimit)
        .mapIndexed { index, beat ->
            beat.copy(title = "Passage ${index + 1}", kind = "Read-along")
        }
}

/** Break long seminar chapters into Android-safe, semantically navigable segments. */
fun speechSafeChapter(chapter: Beat, maxChars: Int = 2200): List<Beat> {
    val words = chapter.content.replace("\r", "").split(Regex("\\s+"))
    val chunks = mutableListOf<String>()
    var current = StringBuilder()
    fun flush() {
        if (current.isNotEmpty()) chunks.add(current.toString())
        current = StringBuilder()
    }
    for (word in words) {
        if (word.length > maxChars) {
            flush()
            chunks.addAll(word.chunked(maxChars))
        } else {
            if (current.length + word.length + 1 > maxChars) flush()
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
    }
    flush()
    return chunks.mapIndexed { index, chunk ->
        Beat(if (chunks.size == 1) chapter.title else "${chapter.title} · ${index + 1}/${chunks.size}", chunk, chapter.kind)
    }
}
