package com.phenomena.study.service

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.phenomena.study.MainActivity
import com.phenomena.study.data.Catalog
import com.phenomena.study.model.Beat
import com.phenomena.study.player.speechSafeChapter
import org.json.JSONArray
import java.util.Locale

/** Foreground TTS radio: distinct from the Activity-owned StudyPlayer.
 * Queue and chunk cursor persist; interruption resumes at the START of the last
 * chunk rather than promising exact-word seeking unsupported by Android TTS.
 * Start commands must originate from an explicit user interaction in the app. */
class SeminarPlaybackService : Service() {
    companion object {
        const val ACTION_PLAY = "com.phenomena.study.radio.PLAY"
        const val ACTION_PAUSE = "com.phenomena.study.radio.PAUSE"
        const val ACTION_RESUME = "com.phenomena.study.radio.RESUME"
        const val ACTION_NEXT = "com.phenomena.study.radio.NEXT"
        const val ACTION_PREVIOUS = "com.phenomena.study.radio.PREVIOUS"
        const val ACTION_STOP = "com.phenomena.study.radio.STOP"
        const val ACTION_STATUS = "com.phenomena.study.radio.STATUS"
        const val EXTRA_MODULES = "module_ids"
        const val EXTRA_CHUNK = "chunk_index"
        private const val CHANNEL_ID = "phenomena_seminars_v2"
        private const val NOTIFY_ID = 2091
        fun send(context: Context, action: String, modules: List<String> = emptyList()) {
            val request = Intent(context, SeminarPlaybackService::class.java).setAction(action)
            if (modules.isNotEmpty()) request.putStringArrayListExtra(EXTRA_MODULES, ArrayList(modules))
            if (action == ACTION_PLAY || action == ACTION_RESUME) {
                ContextCompat.startForegroundService(context, request)
            } else context.startService(request)
        }
    }

    private data class Chunk(val moduleId:String,val moduleTitle:String,val chapterId:String,
                             val chapterTitle:String,val text:String)
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("phenomena_radio_v2",MODE_PRIVATE) }
    private val audio by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private val audioAttrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var audioFocus: AudioFocusRequest? = null
    private var session: MediaSession? = null
    private var tts: TextToSpeech? = null
    private var currentUtterance: String? = null
    private var ttsGeneration = 0
    private var speechReady = false
    private var active = false
    private var paused = false
    private var foreground = false
    private var cursor = 0
    private var modules: List<String> = emptyList()
    private var chapters: List<Chunk> = emptyList()
    private var playbackRate = 0.95f

    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT>=26) {
            val channel=NotificationChannel(CHANNEL_ID,"PHENOMENA · Seminars",NotificationManager.IMPORTANCE_LOW)
            channel.description="Background phenomenological seminar narration"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        session=MediaSession(this,"PHENOMENA-2-SeminarRadio").apply {
            setCallback(object:MediaSession.Callback() {
                override fun onPlay() { send(this@SeminarPlaybackService,ACTION_RESUME) }
                override fun onPause() { send(this@SeminarPlaybackService,ACTION_PAUSE) }
                override fun onSkipToNext() { send(this@SeminarPlaybackService,ACTION_NEXT) }
                override fun onSkipToPrevious() { send(this@SeminarPlaybackService,ACTION_PREVIOUS) }
                override fun onStop() { send(this@SeminarPlaybackService,ACTION_STOP) }
            })
            isActive=true
        }
    }

    override fun onBind(intent:Intent?):IBinder? = null

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        when(intent?.action) {
            ACTION_PLAY -> {
                modules=intent.getStringArrayListExtra(EXTRA_MODULES)?.filter { it.isNotBlank() }
                    ?.distinct()?.take(24).orEmpty().ifEmpty { readSavedModules() }
                if(modules.isEmpty()) { stopRadio(); return START_NOT_STICKY }
                chapters=buildQueue(modules)
                if(chapters.isEmpty()) { stopRadio(); return START_NOT_STICKY }
                cursor=0
                active=true;paused=false;save()
                ensureForeground()
                prepareTts()
            }
            ACTION_RESUME -> {
                if(chapters.isEmpty()) restore()
                if(chapters.isEmpty()) { stopRadio();return START_NOT_STICKY }
                active=true;paused=false
                ensureForeground()
                if(speechReady) speak() else prepareTts()
            }
            ACTION_PAUSE -> pauseRadio()
            ACTION_NEXT -> advance(1)
            ACTION_PREVIOUS -> advance(-1)
            ACTION_STOP -> stopRadio()
            else -> { if(!active) return START_NOT_STICKY }
        }
        updateNotification()
        return if(active) START_STICKY else START_NOT_STICKY
    }

    private fun buildQueue(ids:List<String>):List<Chunk> {
        val catalog=Catalog(applicationContext)
        val result=ArrayList<Chunk>()
        for(id in ids) {
            val module=catalog.seminar(id) ?: continue
            for(chapter in module.chapters) {
                val sections=speechSafeChapter(Beat(chapter.title,chapter.text,chapter.kind))
                sections.forEach { section ->
                    result.add(Chunk(module.id,module.title,chapter.id,section.title,section.content))
                }
            }
        }
        return result
    }
    private fun readSavedModules():List<String> {
        val json=prefs.getString("module_queue","[]") ?: "[]"
        return runCatching {
            val array=JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
    }
    private fun restore() {
        modules=readSavedModules()
        chapters=buildQueue(modules)
        cursor=prefs.getInt("chunk",0).coerceIn(0,(chapters.size-1).coerceAtLeast(0))
    }
    private fun save() {
        prefs.edit().putString("module_queue",JSONArray(modules).toString())
            .putInt("chunk",cursor).putLong("saved_at",System.currentTimeMillis())
            .putString("current_title",chapters.getOrNull(cursor)?.chapterTitle.orEmpty()).apply()
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName)
            .putExtra(EXTRA_CHUNK,cursor).putExtra("total",chapters.size)
            .putExtra("paused",paused))
    }

    private fun requestFocus():Boolean {
        if(audioFocus!=null) return true
        val change=AudioManager.OnAudioFocusChangeListener { state ->
            when(state) {
                AudioManager.AUDIOFOCUS_LOSS -> handler.post { pauseRadio() }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> handler.post { pauseRadio() }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> handler.post { pauseRadio() }
            }
        }
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttrs).setOnAudioFocusChangeListener(change).build()
        val granted=audio.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if(granted) audioFocus=request
        return granted
    }
    private fun prepareTts() {
        val generation=++ttsGeneration
        tts?.stop();tts?.shutdown();tts=null;speechReady=false
        val narrationPrefs=getSharedPreferences("phenomena_narrator_v12",MODE_PRIVATE)
        val requestedEngine=narrationPrefs.getString("engine","").orEmpty()
        playbackRate=narrationPrefs.getFloat("rate",0.95f)
        val listener=TextToSpeech.OnInitListener { code ->
            handler.post {
                if(generation!=ttsGeneration) return@post
                if(code!=TextToSpeech.SUCCESS) {pauseRadio();return@post}
                val speaker=tts ?: return@post
                val voiceId=narrationPrefs.getString("voice","").orEmpty()
                speaker.voices?.firstOrNull { it.name==voiceId }?.let { speaker.voice=it }
                speaker.setSpeechRate(playbackRate)
                speaker.setAudioAttributes(audioAttrs)
                speaker.setOnUtteranceProgressListener(object:UtteranceProgressListener() {
                    override fun onStart(utteranceId:String?) {}
                    override fun onDone(utteranceId:String?) { handler.post {
                        if(generation==ttsGeneration && utteranceId==currentUtterance && active && !paused) {
                            currentUtterance=null;cursor++
                            if(cursor>=chapters.size) stopRadio() else {save();speak()}
                        }
                    } }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId:String?) { handler.post {
                        if(generation==ttsGeneration && utteranceId==currentUtterance) pauseRadio()
                    } }
                })
                speechReady=true
                if(active && !paused) speak()
            }
        }
        tts=if(requestedEngine.isBlank()) TextToSpeech(this,listener)
            else TextToSpeech(this,listener,requestedEngine)
    }
    private fun speak() {
        if(!active || paused || !speechReady) return
        val current=chapters.getOrNull(cursor) ?: run {stopRadio();return}
        if(!requestFocus()) {pauseRadio();return}
        val speaker=tts ?: return
        currentUtterance=null
        speaker.stop()
        val id="phenomena_radio_${System.nanoTime()}"
        currentUtterance=id
        val result=speaker.speak(current.text,TextToSpeech.QUEUE_FLUSH,null,id)
        if(result==TextToSpeech.ERROR) {pauseRadio();return}
        save();updateNotification()
    }
    private fun pauseRadio() {
        if(!active) return
        currentUtterance=null
        tts?.stop();paused=true
        audioFocus?.let {audio.abandonAudioFocusRequest(it)};audioFocus=null
        save();updateNotification()
        // Paused service remains alive with a visible notification, and can be stopped from there.
    }
    private fun advance(delta:Int) {
        if(chapters.isEmpty()) restore()
        if(chapters.isEmpty()) return
        cursor=(cursor+delta).coerceIn(0,chapters.lastIndex)
        save()
        if(active && !paused) speak() else updateNotification()
    }
    private fun stopRadio() {
        currentUtterance=null
        active=false;paused=false;speechReady=false;ttsGeneration++
        tts?.stop();tts?.shutdown();tts=null
        audioFocus?.let {audio.abandonAudioFocusRequest(it)};audioFocus=null
        save();updateSession()
        if(foreground) { stopForeground(STOP_FOREGROUND_REMOVE);foreground=false }
        stopSelf()
    }
    private fun notificationAction(icon:Int, name:String, action:String, code:Int):Notification.Action {
        val pending=PendingIntent.getService(this,code,
            Intent(this,SeminarPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(icon,name,pending).build()
    }
    private fun createNotification():Notification {
        val chapter=chapters.getOrNull(cursor)
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b=Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(chapter?.moduleTitle ?: "PHENOMENA Great Seminar")
            .setContentText(chapter?.chapterTitle ?: "Your listening queue")
            .setContentIntent(open).setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(active && !paused)
            .addAction(notificationAction(android.R.drawable.ic_media_previous,"Previous",ACTION_PREVIOUS,1))
            .addAction(notificationAction(if(paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if(paused) "Resume" else "Pause",if(paused) ACTION_RESUME else ACTION_PAUSE,2))
            .addAction(notificationAction(android.R.drawable.ic_media_next,"Next",ACTION_NEXT,3))
            .addAction(notificationAction(android.R.drawable.ic_menu_close_clear_cancel,"Stop",ACTION_STOP,4))
        session?.sessionToken?.let { token ->
            b.setStyle(Notification.MediaStyle().setMediaSession(token).setShowActionsInCompactView(0,1,2))
        }
        return b.build()
    }
    private fun ensureForeground() {
        if(!foreground) { startForeground(NOTIFY_ID,createNotification());foreground=true }
        updateSession()
    }
    private fun updateNotification() {
        if(active) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFY_ID,createNotification())
            updateSession()
        }
    }
    private fun updateSession() {
        val state=when {
            !active -> PlaybackState.STATE_STOPPED
            paused -> PlaybackState.STATE_PAUSED
            else -> PlaybackState.STATE_PLAYING
        }
        val actions=PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP
        session?.setPlaybackState(PlaybackState.Builder().setActions(actions).setState(state,0L,1f).build())
    }
    override fun onDestroy() {
        ttsGeneration++
        tts?.stop();tts?.shutdown();tts=null
        audioFocus?.let {audio.abandonAudioFocusRequest(it)}
        session?.isActive=false;session?.release();session=null
        super.onDestroy()
    }
}
