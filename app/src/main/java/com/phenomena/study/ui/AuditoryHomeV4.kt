package com.phenomena.study.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.phenomena.study.audio.ElevenReaderBridge
import com.phenomena.study.data.*
import com.phenomena.study.learning.*
import com.phenomena.study.model.*
import com.phenomena.study.player.*
import com.phenomena.study.service.SeminarPlaybackService
import org.json.JSONArray

/**
 * PHENOMENA 4 · audio-first interface.
 * One existing Android foreground speech service provides all four packaged
 * listening modes; ElevenReader receives a user-consented TXT handoff instead
 * of any unsupported access to its proprietary voices.
 */
@Composable
fun AuditoryHomeV4(
    lesson:Lesson, imported:ImportedDocument?, player:StudyPlayer, catalog:Catalog,
    onChooseLesson:(String)->Unit,onAudioPick:()->Unit,onShare:(String)->Unit
) {
    val context=LocalContext.current
    val playbackPrefs=remember{context.getSharedPreferences("phenomena_radio_v2",Context.MODE_PRIVATE)}
    val uiPrefs=remember{context.getSharedPreferences("phenomena_audio_v4",Context.MODE_PRIVATE)}
    var mode by rememberSaveable(imported?.id,lesson.id) {
        mutableStateOf(if(imported!=null) "Guided" else uiPrefs.getString("mode","Guided") ?: "Guided")
    }
    var minimal by rememberSaveable { mutableStateOf(uiPrefs.getBoolean("eyes_free",true)) }
    var preferReader by rememberSaveable { mutableStateOf(uiPrefs.getBoolean("reader_first",true)) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val nativeVoicePrefs=remember {context.getSharedPreferences("phenomena_narrator_v12",Context.MODE_PRIVATE)}
    var voiceRole by rememberSaveable {mutableStateOf(nativeVoicePrefs.getString("voice_role","PROFESSOR").orEmpty())}
    var notificationMessage by remember { mutableStateOf("") }
    val notificationPermission=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        notificationMessage=if(granted) "" else
            "Notification permission was declined; lock-screen media controls may be hidden."
    }
    var transcriptOpen by rememberSaveable { mutableStateOf(false) }
    var chooserOpen by remember { mutableStateOf(false) }
    var chooseTerm by remember {mutableStateOf("")}
    var selectedModuleId by remember(lesson.workId) { mutableStateOf<String?>(null) }
    var radioStyle by rememberSaveable { mutableStateOf(uiPrefs.getString("radio_style","DISCOVERY") ?: "DISCOVERY") }
    var socraticPause by rememberSaveable {mutableFloatStateOf(
        playbackPrefs.getLong("socratic_pause_ms",4000L).toFloat()/1000f) }
    var chapter by remember {mutableStateOf(playbackPrefs.getString("current_title","").orEmpty())}
    var program by remember {mutableStateOf(playbackPrefs.getString("current_module","").orEmpty())}
    var currentChunk by remember {mutableIntStateOf(playbackPrefs.getInt("chunk",0))}
    var playing by remember {mutableStateOf(playbackPrefs.getBoolean("running",false))}
    var paused by remember {mutableStateOf(playbackPrefs.getBoolean("paused",false))}
    var sleepUntil by remember {mutableLongStateOf(playbackPrefs.getLong("sleep_until",0L))}
    var bookmarks by remember {mutableStateOf(playbackPrefs.getString("audio_bookmarks_v4","[]").orEmpty())}
    val modules=remember(lesson.workId,catalog.seminars){catalog.seminars.filter{it.workId==lesson.workId}}
    val authored=remember(lesson.workId){InstituteMaterials.lecturesFor(lesson.workId)}
    val masterclasses=remember(lesson.workId){MasterclassesV4.forWork(lesson.workId)}
    val chosenId=selectedModuleId ?:modules.firstOrNull()?.id ?:masterclasses.firstOrNull()?.id ?:authored.firstOrNull()?.id
    val selectedModule=modules.firstOrNull { it.id==chosenId }
    val selectedMaster=masterclasses.firstOrNull { it.id==chosenId }
    val selectedAuthor=authored.firstOrNull { it.id==chosenId }
    val guidedText=remember(lesson.id){GuidedTranscript.transcript(lesson)}
    val universityText=remember(lesson.id){UniversityPedagogy.transcript(lesson)}
    val importedText=imported?.content
    val readerEngine=remember(player.engines.size){ElevenReaderBridge.isAndroidTtsEngine(context)}
    fun refreshPlayback(){
        chapter=playbackPrefs.getString("current_title","").orEmpty()
        program=playbackPrefs.getString("current_module","").orEmpty()
        currentChunk=playbackPrefs.getInt("chunk",0)
        playing=playbackPrefs.getBoolean("running",false)
        paused=playbackPrefs.getBoolean("paused",false)
        bookmarks=playbackPrefs.getString("audio_bookmarks_v4","[]").orEmpty()
        sleepUntil=playbackPrefs.getLong("sleep_until",0L)
    }
    DisposableEffect(context){
        val receiver=object:BroadcastReceiver(){
            override fun onReceive(c:Context?,intent:Intent?){refreshPlayback()}
        }
        ContextCompat.registerReceiver(context,receiver,
            IntentFilter(SeminarPlaybackService.ACTION_STATUS),ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose{context.unregisterReceiver(receiver)}
    }
    fun send(action:String, ids:List<String> = emptyList(),minutes:Int = -1,
             bookmarkIndex:Int = -1,style:String = "",work:String = ""){
        player.stop()
        if((action==SeminarPlaybackService.ACTION_PLAY ||
            action==SeminarPlaybackService.ACTION_RADIO) &&
            Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=
                PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        SeminarPlaybackService.send(context,action,ids,minutes=minutes,
            bookmarkIndex=bookmarkIndex,style=style,topicWork=work)
        refreshPlayback()
    }
    fun nativeStart(){
        when(mode){
            "Guided" -> if(imported==null)send(SeminarPlaybackService.ACTION_PLAY,listOf("guided:${lesson.id}"))
            else { send(SeminarPlaybackService.ACTION_STOP)
                player.setSegments(toSpeechChunks(imported.content,2000))
                player.autoAdvance=true;player.play() }
            "University" -> send(SeminarPlaybackService.ACTION_PLAY,listOf("university:${lesson.id}"))
            "Long Format" -> if(chosenId!=null)
                send(SeminarPlaybackService.ACTION_PLAY,listOf(chosenId))
            "Radio" -> send(SeminarPlaybackService.ACTION_RADIO,style=radioStyle,work=lesson.workId)
        }
    }
    fun shareInElevenReader(){
        val title=when(mode){
            "University" -> "PHENOMENA University · ${lesson.title}"
            "Long Format" -> "PHENOMENA Seminar · ${selectedModule?.title ?: selectedMaster?.title ?: selectedAuthor?.title ?: lesson.title}"
            else -> imported?.title ?: "PHENOMENA Guided · ${lesson.title}"
        }
        val text=when(mode){
            "University" -> universityText
            "Long Format" -> selectedModule?.chapters?.joinToString("\n\n") {
                "${it.title}\n${it.text}" }.orEmpty()
            else -> importedText ?: guidedText
        }
        send(SeminarPlaybackService.ACTION_STOP)
        ElevenReaderBridge.sendLecture(context,title,text)
    }
    val showMode=listOf("Guided","Long Format","Radio","University").let{
        if(imported!=null)listOf("Guided") else it
    }
    val bookmarkEntries=remember(bookmarks) {
        runCatching {JSONArray(bookmarks)}.getOrNull()?.let{a->
            (0 until minOf(a.length(),6)).mapNotNull { index->
                a.optJSONObject(index)?.let { index to it.optString("program","Audio bookmark") }
            }
        } ?:emptyList()
    }

    LazyColumn(Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=17.dp,vertical=18.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Eyebrow("PHENOMENA IV · AUDIO FIRST")
                    Display("The auditory institute.",size=32)
                }
                OutlinedIconButton(onClick={
                    minimal=!minimal
                    uiPrefs.edit().putBoolean("eyes_free",minimal).apply()
                },modifier=Modifier.size(55.dp)){
                    Icon(if(minimal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "Toggle eyes-free listening layout",tint=Mint,modifier=Modifier.size(25.dp))
                }
            }
            if(!minimal)Text("Choose a study, listen, and return to its argument without prolonged reading.",
                color=Muted,fontSize=12.sp,lineHeight=18.sp)
        }
        item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow(if(mode=="Radio")"YOUR STUDY · RADIO IS CROSS-CATALOGUE"
                    else "CURRENT STUDY · SELECTED TOPIC")
                Text(imported?.title ?: lesson.title,
                    color=Ink,fontFamily=FontFamily.Serif,fontSize=if(minimal)25.sp else 21.sp,
                    lineHeight=30.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
                Text(if(imported!=null)"Your imported document" else
                    "${catalog.work(lesson.workId)?.title} · ${catalog.work(lesson.workId)?.author}",
                    color=Muted,fontSize=12.sp,lineHeight=19.sp)
                if(imported==null){
                    OutlinedButton(onClick={chooserOpen=!chooserOpen},
                        modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                        Icon(Icons.Default.LibraryBooks,null,Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose another philosophical lesson",
                            modifier=Modifier.weight(1f),fontSize=13.sp)
                        Icon(Icons.Default.ExpandMore,null)
                    }
                    if(chooserOpen){
                        OutlinedTextField(value=chooseTerm,onValueChange={chooseTerm=it},
                            placeholder={Text("Search works, authors or topics")},
                            modifier=Modifier.fillMaxWidth(),singleLine=true)
                        val matched=remember(chooseTerm){
                            catalog.lessons.filter{
                                it.title.contains(chooseTerm,true) ||
                                it.section.contains(chooseTerm,true) ||
                                (catalog.work(it.workId)?.author?.contains(chooseTerm,true)==true)
                            }.take(35)
                        }
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max=240.dp)){
                            items(matched,key={it.id}){item->
                                TextButton(onClick={chooserOpen=false;onChooseLesson(item.id)},
                                    modifier=Modifier.fillMaxWidth()){
                                    Column(Modifier.fillMaxWidth()){
                                        Text(item.title,color=Ink,fontSize=13.sp,maxLines=2)
                                        Text(catalog.work(item.workId)?.title.orEmpty(),
                                            color=Muted,fontSize=11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                showMode.forEach{option->
                    Surface(onClick={
                        if(mode!=option){
                            send(SeminarPlaybackService.ACTION_PAUSE)
                            mode=option
                            uiPrefs.edit().putString("mode",option).apply()
                        }
                    },modifier=Modifier.weight(1f),shape=RoundedCornerShape(17.dp),
                        color=if(mode==option)Moss else CardSurface,
                        border=BorderStroke(1.dp,if(mode==option)Mint else Line)){
                        Column(Modifier.padding(vertical=16.dp,horizontal=2.dp),
                            horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(6.dp)){
                            Icon(when(option){
                                "Guided"->Icons.Default.Headphones
                                "Long Format"->Icons.Default.LibraryBooks
                                "University"->Icons.Default.School
                                else->Icons.Default.Radio
                            },null,tint=if(mode==option)Mint else Muted,
                                modifier=Modifier.size(24.dp))
                            Text(when(option){"Long Format"->"Long";else->option},
                                color=Ink,fontSize=11.sp,fontWeight=FontWeight.SemiBold,
                                maxLines=1)
                        }
                    }
                }
            }
        }
        if(mode=="Long Format"&&imported==null) item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("WORK-MATCHED SEMINAR SELECTION")
                if(modules.isEmpty()&&authored.isEmpty()&&masterclasses.isEmpty())
                    EmptyNotice("No indexed full seminar for this work. Guided and University remain available.")
                else {
                    val choices=modules.map { it.id to it.title } +
                        masterclasses.map { it.id to ("V4 Masterclass · "+it.title) } +
                        authored.map { it.id to ("Authored · "+it.title) }
                    choices.forEach { (id,title)->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            RadioButton(selected=chosenId==id,
                                onClick={selectedModuleId=id})
                            Text(title,color=Ink,fontSize=12.sp,lineHeight=18.sp,
                                modifier=Modifier.weight(1f))
                        }
                    }
                    OutlinedButton(onClick={
                        send(SeminarPlaybackService.ACTION_PLAY,
                            (modules.map{it.id}+masterclasses.map{it.id}+authored.map{it.id}).take(24))
                    },modifier=Modifier.fillMaxWidth().heightIn(min=55.dp)){
                        Icon(Icons.Default.PlayArrow,null)
                        Spacer(Modifier.width(7.dp));Text("Play all seminars for this work")
                    }
                }
            }
        }
        if(mode=="Radio")item{
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("PHILOSOPHICAL RADIO · RANDOM PROGRAMMES")
                Body("Discovery explores every work. Thematic stays with your selected work. Nocturnal emphasizes long lectures and calm listening.",
                    color=Muted)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    listOf("DISCOVERY","THEMATIC","NOCTURNAL").forEach{style->
                        LabelTag(style,radioStyle==style){
                            radioStyle=style
                            uiPrefs.edit().putString("radio_style",style).apply()
                        }
                    }
                }
                Text("New randomized queues avoid recently played programs. The actual lecture title is announced and shown below.",
                    color=Muted,fontSize=11.sp,lineHeight=17.sp)
            }
        }
        if(mode=="University") item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("A COMPLETE AUDIO UNIVERSITY")
                Text("12 cumulative teaching movements",color=Ink,
                    fontSize=21.sp,fontFamily=FontFamily.Serif)
                Body("A philosophical problem, lived example, conceptual distinctions, scholarly exposition, logical reconstruction, objections, comparative readings, and research exercises.",
                    color=Muted)
                Text("All 355 study units can be heard here in their pedagogically rewritten sequence. The University tab provides the detailed curriculum.",
                    color=Mint,fontSize=11.sp,lineHeight=18.sp)
                Eyebrow("SOCRATIC THINKING PAUSE")
                Text("The University narrator pauses between major teaching movements to let you answer the question mentally. You can choose no pause.",
                    color=Muted,fontSize=11.sp,lineHeight=18.sp)
                Slider(value=socraticPause,onValueChange={
                    socraticPause=it
                    playbackPrefs.edit().putLong("socratic_pause_ms",(it*1000).toLong()).apply()
                },valueRange=0f..15f,steps=2)
                Text("Time to think: ${socraticPause.toInt()} seconds",
                    color=Brass,fontSize=12.sp)
            }
        }
        if(mode=="Guided") item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("CONTINUOUS GUIDED SCHOLARLY NARRATION")
                Text("One lesson. One unfolding argument.",color=Ink,fontSize=22.sp,
                    fontFamily=FontFamily.Serif)
                Body("Synopsis, exposition, advanced interpretation, argument, objections, comparison and seminar are played in a single ordered session.",
                    color=Muted)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("THE GRAND PLAYER · ONE ACTIVE AUDIO SESSION")
                Text(if(program.isNotBlank())program else (imported?.title?:lesson.title),
                    color=Ink,fontSize=if(minimal)22.sp else 16.sp,
                    maxLines=3,lineHeight=26.sp,fontFamily=FontFamily.Serif)
                Text(if(chapter.isNotBlank())chapter else "Choose a program and press Play",
                    color=Brass,fontSize=13.sp,maxLines=3,lineHeight=19.sp)
                Text("Segment ${currentChunk+1} · ${if(playing)if(paused)"Paused" else "Narrating" else "Ready to listen"}",
                    color=Muted,fontSize=11.sp)
                Button(onClick={
                    if(preferReader&&mode!="Radio")shareInElevenReader()
                    else nativeStart()
                },modifier=Modifier.fillMaxWidth().heightIn(min=70.dp),
                    enabled=mode!="Long Format"||modules.isNotEmpty()||authored.isNotEmpty()||masterclasses.isNotEmpty(),
                    colors=ButtonDefaults.buttonColors(containerColor=Moss)){
                    Icon(if(mode=="Radio")Icons.Default.Shuffle else Icons.Default.PlayArrow,
                        null,Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(if(preferReader&&mode!="Radio")
                        "Listen in ElevenReader" else "Play ${mode.lowercase()}",
                        fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                }
                if(preferReader&&mode!="Radio") {
                    OutlinedButton(onClick={nativeStart()},modifier=Modifier.fillMaxWidth().heightIn(min=57.dp)){
                        Icon(Icons.Default.Headphones,null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play inside PHENOMENA (Android TTS)")
                    }
                } else if(mode!="Radio") {
                    OutlinedButton(onClick={shareInElevenReader()},
                        modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){
                        Icon(Icons.Default.OpenInNew,null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send complete transcript to ElevenReader")
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_PREVIOUS)},
                        modifier=Modifier.weight(1f).heightIn(min=53.dp),
                        contentPadding=PaddingValues(6.dp)){
                        Icon(Icons.Default.SkipPrevious,"Previous chapter",Modifier.size(25.dp))
                    }
                    OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_RESUME)},
                        modifier=Modifier.weight(1f).heightIn(min=53.dp),
                        contentPadding=PaddingValues(6.dp)){
                        Icon(Icons.Default.PlayArrow,"Resume",Modifier.size(25.dp))
                    }
                    OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_PAUSE)},
                        modifier=Modifier.weight(1f).heightIn(min=53.dp),
                        contentPadding=PaddingValues(6.dp)){
                        Icon(Icons.Default.Pause,"Pause",Modifier.size(25.dp))
                    }
                    OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_NEXT)},
                        modifier=Modifier.weight(1f).heightIn(min=53.dp),
                        contentPadding=PaddingValues(6.dp)){
                        Icon(Icons.Default.SkipNext,"Next chapter",Modifier.size(25.dp))
                    }
                }
                OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_STOP)},
                    modifier=Modifier.fillMaxWidth().heightIn(min=49.dp)){
                    Icon(Icons.Default.Stop,null)
                    Spacer(Modifier.width(7.dp));Text("Stop PHENOMENA audio")
                }
                if(notificationMessage.isNotBlank()) Text(notificationMessage,
                    color=Brass,fontSize=11.sp,lineHeight=17.sp)
                Text("The background player resumes from the saved TTS chunk, not an exact word.",
                    color=Muted,fontSize=11.sp,lineHeight=17.sp)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("HANDS-FREE TOOLS")
                OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_BOOKMARK)},
                    modifier=Modifier.fillMaxWidth().heightIn(min=55.dp)){
                    Icon(Icons.Default.BookmarkAdd,null)
                    Spacer(Modifier.width(8.dp));Text("Bookmark what I'm hearing")
                }
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    listOf(0,15,30,60,120).forEach { min->
                        OutlinedButton(onClick={
                            send(SeminarPlaybackService.ACTION_SLEEP,minutes=min)
                        },contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp)){
                            Text(if(min==0)"Timer off" else "${min} min",fontSize=12.sp)
                        }
                    }
                }
                if(sleepUntil>System.currentTimeMillis()) {
                    val left=((sleepUntil-System.currentTimeMillis())/60000L+1).coerceAtLeast(1)
                    Text("Sleep timer active: around ${left} minutes remaining",
                        color=Mint,fontSize=11.sp)
                }
                if(bookmarkEntries.isNotEmpty()){
                    Eyebrow("RECENT AUDIO BOOKMARKS")
                    bookmarkEntries.forEach {(index,title)->
                        TextButton(onClick={
                            send(SeminarPlaybackService.ACTION_RESTORE_BOOKMARK,
                                bookmarkIndex=index)
                        },modifier=Modifier.fillMaxWidth().heightIn(min=44.dp)){
                            Icon(Icons.Default.Bookmark,null,Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(title,maxLines=2,color=Ink,
                                modifier=Modifier.weight(1f),fontSize=12.sp)
                            Icon(Icons.Default.PlayArrow,null,Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("YOUR NARRATOR · ELEVENREADER PREFERENCE")
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("Prefer ElevenReader for lecture listening",color=Ink,fontSize=13.sp,
                            fontWeight=FontWeight.SemiBold)
                        Text("Opens its own app for proprietary voices; native playback remains available.",
                            fontSize=11.sp,color=Muted,lineHeight=17.sp)
                    }
                    Switch(checked=preferReader,onCheckedChange={
                        preferReader=it
                        uiPrefs.edit().putBoolean("reader_first",it).apply()
                    })
                }
                Text(if(readerEngine)"ElevenReader offers an installed Android TTS service. It can be selected as an engine."
                else "ElevenReader does not appear as an Android TTS engine on this device. Its proprietary voices are available through the external app handoff, not inside PHENOMENA.",
                    color=Muted,fontSize=11.sp,lineHeight=18.sp)
                OutlinedButton(onClick={settingsOpen=!settingsOpen},
                    modifier=Modifier.fillMaxWidth().heightIn(min=49.dp)){
                    Icon(Icons.Default.Settings,null)
                    Spacer(Modifier.width(8.dp))
                    Text(if(settingsOpen)"Hide native voice settings" else "Android TTS voice and speech rate")
                }
                if(settingsOpen){
                    Eyebrow("OPTIONAL NATIVE ANDROID VOICE STYLES")
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        listOf("PROFESSOR","DIALECTICIAN","NOCTURNAL").forEach { style ->
                            LabelTag(style,voiceRole==style){
                                voiceRole=style
                                val rate=when(style){
                                    "DIALECTICIAN"->1.04f
                                    "NOCTURNAL"->.83f
                                    else->.95f
                                }
                                val pitch=when(style){
                                    "DIALECTICIAN"->1.04f
                                    "NOCTURNAL"->.87f
                                    else->.94f
                                }
                                nativeVoicePrefs.edit().putFloat("pitch",pitch)
                                    .putString("voice_role",style).apply()
                                player.setRate(rate)
                            }
                        }
                    }
                    Text("These styles adjust Android playback pace and pitch only. Select your ElevenReader proprietary voice within ElevenReader.",
                        color=Muted,fontSize=11.sp,lineHeight=17.sp)
                    NarratorSettingsPanel(player)
                    Text("Rate: ${"%.2f".format(player.speechRate)}×",color=Muted,fontSize=11.sp)
                    Slider(value=player.speechRate,onValueChange=player::setRate,
                        valueRange=.7f..1.4f)
                    OutlinedButton(onClick={
                        val intent=Intent("android.settings.TTS_SETTINGS")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching{context.startActivity(intent)}
                    },modifier=Modifier.fillMaxWidth()){
                        Text("Open Android default TTS engine settings")
                    }
                }
                Text("Your ElevenReader account and selected premium voice are controlled in ElevenReader. PHENOMENA neither extracts those voices nor bypasses vendor restrictions.",
                    color=Muted,fontSize=10.sp,lineHeight=16.sp)
            }
        }
        if(!minimal) item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("OPTIONAL TRANSCRIPT · READING COMFORT")
                OutlinedButton(onClick={transcriptOpen=!transcriptOpen},
                    modifier=Modifier.fillMaxWidth().heightIn(min=55.dp)){
                    Icon(Icons.Default.TextFields,null)
                    Spacer(Modifier.width(8.dp))
                    Text(if(transcriptOpen)"Hide long transcript" else "Show transcript (optional)")
                }
                if(transcriptOpen){
                    val current=when(mode){
                        "University" -> universityText
                        "Long Format" -> selectedModule?.chapters?.take(5)?.joinToString("\n\n"){it.text}
                            ?: "No seminar selected"
                        "Radio" -> "The app displays the current playing program above. Radio draws from the complete library."
                        else -> importedText ?: guidedText
                    }
                    SelectionContainer{
                        Text(current.take(28000),color=Ink,
                            fontFamily=FontFamily.Serif,fontSize=18.sp,lineHeight=30.sp)
                    }
                }
            }
        }
        item {
            Text("PHENOMENA 4.0 · Narration-first, original scholarly educational commentary. "
                +"ElevenReader is an independent app and may require you to choose a voice there. "
                +"In-app playback uses Android's compatible TTS engines.",
                color=Muted,fontSize=10.sp,lineHeight=17.sp)
        }
    }
}
