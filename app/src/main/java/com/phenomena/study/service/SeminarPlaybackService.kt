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
import com.phenomena.study.data.InstituteMaterials
import com.phenomena.study.learning.GuidedTranscript
import com.phenomena.study.learning.UniversityPedagogy
import com.phenomena.study.player.toSpeechChunks
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
        const val ACTION_RADIO = "com.phenomena.study.radio.RADIO"
        const val ACTION_SLEEP = "com.phenomena.study.radio.SLEEP"
        const val ACTION_BOOKMARK = "com.phenomena.study.radio.BOOKMARK"
        const val ACTION_RESTORE_BOOKMARK = "com.phenomena.study.radio.RESTORE_BOOKMARK"
        const val EXTRA_MINUTES = "sleep_minutes"
        const val EXTRA_BOOKMARK_INDEX = "bookmark_index"
        const val EXTRA_STYLE = "radio_style"
        const val EXTRA_TOPIC_WORK = "topic_work_id"
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
        fun send(context: Context, action: String, modules: List<String> = emptyList(),
                 minutes: Int = -1, bookmarkIndex: Int = -1, style: String = "", topicWork: String = "") {
            val request = Intent(context, SeminarPlaybackService::class.java).setAction(action)
            if (modules.isNotEmpty()) request.putStringArrayListExtra(EXTRA_MODULES, ArrayList(modules))
            if(minutes>=0) request.putExtra(EXTRA_MINUTES,minutes)
            if(bookmarkIndex>=0) request.putExtra(EXTRA_BOOKMARK_INDEX,bookmarkIndex)
            if(style.isNotBlank()) request.putExtra(EXTRA_STYLE,style)
            if(topicWork.isNotBlank()) request.putExtra(EXTRA_TOPIC_WORK,topicWork)
            if (action == ACTION_PLAY || action == ACTION_RADIO || action == ACTION_RESUME || action == ACTION_RESTORE_BOOKMARK) {
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
    private var radioMode = false
    private var radioStyle = "DISCOVERY"
    private var thematicWorkId = ""
    private val sleeper=Runnable { if(active)stopRadio() }

    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT>=26) {
            val channel=NotificationChannel(CHANNEL_ID,"PHENOMENA · Seminars",NotificationManager.IMPORTANCE_LOW)
            channel.description="Background phenomenological seminar narration"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        session=MediaSession(this,"PHENOMENA-3-InstituteRadio").apply {
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
                radioMode=false
                modules=intent.getStringArrayListExtra(EXTRA_MODULES)?.filter { it.isNotBlank() }
                    ?.distinct()?.take(24).orEmpty()
                if(modules.isEmpty()) { stopRadio(); return START_NOT_STICKY }
                chapters=buildQueue(modules)
                if(chapters.isEmpty()) { stopRadio(); return START_NOT_STICKY }
                cursor=0
                active=true;paused=false;save()
                ensureForeground()
                prepareTts()
            }
            ACTION_RADIO -> {
                radioMode=true
                radioStyle=intent.getStringExtra(EXTRA_STYLE)?.takeIf {
                    it=="DISCOVERY" || it=="THEMATIC" || it=="NOCTURNAL"
                } ?: "DISCOVERY"
                thematicWorkId=intent.getStringExtra(EXTRA_TOPIC_WORK).orEmpty()
                modules=nextRandomMix()
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
            ACTION_SLEEP -> {
                val minutes=intent.getIntExtra(EXTRA_MINUTES,-1)
                if(minutes>=0) {
                    handler.removeCallbacks(sleeper)
                    val deadline=if(minutes==0)0L else System.currentTimeMillis()+minutes*60000L
                    prefs.edit().putLong("sleep_until",deadline).apply()
                    if(minutes>0)handler.postDelayed(sleeper,minutes*60000L)
                    save()
                }
            }
            ACTION_BOOKMARK -> bookmarkNow()
            ACTION_RESTORE_BOOKMARK -> restoreBookmark(
                intent.getIntExtra(EXTRA_BOOKMARK_INDEX,-1))
            else -> { if(!active) return START_NOT_STICKY }
        }
        updateNotification()
        return if(active) START_STICKY else START_NOT_STICKY
    }

    private fun buildQueue(ids:List<String>):List<Chunk> {
        val catalog=Catalog(applicationContext)
        val result=ArrayList<Chunk>()
        for(id in ids) {
            if(id.startsWith("university:")) {
                val lesson=catalog.lesson(id.removePrefix("university:")) ?: continue
                val source=catalog.work(lesson.workId)?.shortTitle.orEmpty()
                val segments=UniversityPedagogy.beats(lesson) { text->toSpeechChunks(text,2000) }
                segments.forEachIndexed { index, section ->
                    result.add(Chunk(id, "University · $source · ${lesson.title}",
                        "${lesson.id}:university:${index+1}", section.title, section.content))
                }
                continue
            }
            if(id.startsWith("guided:")) {
                val lesson=catalog.lesson(id.removePrefix("guided:")) ?: continue
                val work=catalog.work(lesson.workId)?.shortTitle
                    ?.ifBlank { catalog.work(lesson.workId)?.title ?: "PHENOMENA" }
                    ?: "PHENOMENA"
                val segments=GuidedTranscript.beats(lesson) { text -> toSpeechChunks(text,2000) }
                segments.forEachIndexed { index, section ->
                    result.add(Chunk(id, "$work · ${lesson.title}",
                        "${lesson.id}:${index+1}", "Guided · ${section.title}", section.content))
                }
                continue
            }
            val authored = InstituteMaterials.lectures.firstOrNull { it.id == id }
            if (authored != null) {
                authored.paragraphs.forEachIndexed { index, paragraph ->
                    val sections = speechSafeChapter(Beat(
                        "Part ${index+1}: ${authored.title}", paragraph, authored.type))
                    sections.forEachIndexed { j, section ->
                        result.add(Chunk(authored.id, authored.title,
                            "${authored.id}:${index+1}:${j+1}", section.title, section.content))
                    }
                }
                continue
            }
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
    /** Radio mixes guided study and long-form seminars. A rotating exclusion
     * history avoids immediate repeats without loading the entire corpus as
     * simultaneously active speech chunks. */
    private fun nextRandomMix():List<String> {
        val catalog=Catalog(applicationContext)
        val candidates=(catalog.seminars.map { it.id } +
            InstituteMaterials.lectures.map { it.id } +
            catalog.lessons.map { "guided:${it.id}" } +
            catalog.lessons.map { "university:${it.id}" }).distinct()
        if(candidates.isEmpty()) return emptyList()
        val recent=runCatching {
            val array=JSONArray(prefs.getString("radio_history","[]"))
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
        val pool=when(radioStyle) {
            "THEMATIC" -> candidates.filter { id ->
                when {
                    id.startsWith("university:") -> catalog.lesson(id.removePrefix("university:"))?.workId==thematicWorkId
                    id.startsWith("guided:") -> catalog.lesson(id.removePrefix("guided:"))?.workId==thematicWorkId
                    else -> catalog.seminar(id)?.workId==thematicWorkId ||
                        InstituteMaterials.lectures.any { it.id==id && it.workId==thematicWorkId }
                }
            }
            "NOCTURNAL" -> candidates.filterNot { it.startsWith("guided:") }
            else -> candidates
        }.ifEmpty { candidates }
        val available=pool.filterNot { it in recent }.ifEmpty { pool }
        val chosen=available.shuffled().take(24)
        prefs.edit().putString("radio_history",
            JSONArray((recent+chosen).takeLast(72)).toString()).apply()
        return chosen
    }

    private fun readSavedModules():List<String> {
        val json=prefs.getString("module_queue","[]") ?: "[]"
        return runCatching {
            val array=JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
    }
    private fun restore() {
        radioMode=prefs.getBoolean("radio_enabled",false)
        radioStyle=prefs.getString("radio_style","DISCOVERY").orEmpty()
        thematicWorkId=prefs.getString("radio_topic_work","").orEmpty()
        val deadline=prefs.getLong("sleep_until",0L)
        handler.removeCallbacks(sleeper)
        if(deadline>0L) {
            val remaining=deadline-System.currentTimeMillis()
            if(remaining<=0L) { stopRadio();return }
            handler.postDelayed(sleeper,remaining)
        }
        modules=readSavedModules()
        chapters=buildQueue(modules)
        cursor=prefs.getInt("chunk",0).coerceIn(0,(chapters.size-1).coerceAtLeast(0))
    }
    private fun save() {
        prefs.edit().putString("module_queue",JSONArray(modules).toString())
            .putInt("chunk",cursor).putLong("saved_at",System.currentTimeMillis())
            .putBoolean("radio_enabled",radioMode)
            .putString("radio_style",radioStyle)
            .putString("radio_topic_work",thematicWorkId)
            .putBoolean("running",active)
            .putString("play_mode",if(radioMode) "RADIO" else "LONG")
            .putString("current_title",chapters.getOrNull(cursor)?.chapterTitle.orEmpty())
            .putString("current_module",chapters.getOrNull(cursor)?.moduleTitle.orEmpty()).apply()
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName)
            .putExtra(EXTRA_CHUNK,cursor).putExtra("total",chapters.size)
            .putExtra("paused",paused))
    }

    private fun bookmarkNow() {
        if(chapters.isEmpty())return
        val history=runCatching {
            JSONArray(prefs.getString("audio_bookmarks_v4","[]"))
        }.getOrDefault(JSONArray())
        val current=chapters[cursor.coerceIn(0,chapters.lastIndex)]
        val entry=org.json.JSONObject().put("title",current.chapterTitle)
            .put("program",current.moduleTitle)
            .put("modules",JSONArray(modules)).put("cursor",cursor)
            .put("radio",radioMode).put("style",radioStyle)
            .put("topic",thematicWorkId)
            .put("time",System.currentTimeMillis())
        val updated=JSONArray().put(entry)
        for(i in 0 until minOf(history.length(),14))updated.put(history.optJSONObject(i))
        prefs.edit().putString("audio_bookmarks_v4",updated.toString()).apply()
        save()
    }
    private fun restoreBookmark(index:Int) {
        val entry=runCatching {
            JSONArray(prefs.getString("audio_bookmarks_v4","[]")).optJSONObject(index)
        }.getOrNull() ?:return
        val a=entry.optJSONArray("modules") ?:return
        val ids=(0 until a.length()).map{a.optString(it)}.filter{it.isNotBlank()}.take(24)
        if(ids.isEmpty())return
        radioMode=entry.optBoolean("radio",false)
        radioStyle=entry.optString("style","DISCOVERY")
        thematicWorkId=entry.optString("topic","")
        modules=ids;chapters=buildQueue(modules)
        if(chapters.isEmpty())return
        cursor=entry.optInt("cursor",0).coerceIn(0,chapters.lastIndex)
        active=true;paused=false;save();ensureForeground()
        if(speechReady)speak() else prepareTts()
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
                            if(cursor>=chapters.size && radioMode) {
                                modules=nextRandomMix()
                                chapters=buildQueue(modules)
                                cursor=0
                                if(chapters.isEmpty()) stopRadio() else {save();speak()}
                            } else if(cursor>=chapters.size) stopRadio() else {save();speak()}
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
        if(delta>0 && radioMode && cursor==chapters.lastIndex) {
            modules=nextRandomMix();chapters=buildQueue(modules);cursor=0
        } else cursor=(cursor+delta).coerceIn(0,chapters.lastIndex)
        save()
        if(active && !paused) speak() else updateNotification()
    }
    private fun stopRadio() {
        handler.removeCallbacks(sleeper)
        prefs.edit().putLong("sleep_until",0L).apply()
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
        handler.removeCallbacks(sleeper)
        ttsGeneration++
        tts?.stop();tts?.shutdown();tts=null
        audioFocus?.let {audio.abandonAudioFocusRequest(it)}
        session?.isActive=false;session?.release();session=null
        super.onDestroy()
    }
}
