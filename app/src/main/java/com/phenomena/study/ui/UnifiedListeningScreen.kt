package com.phenomena.study.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.phenomena.study.data.*
import com.phenomena.study.learning.GuidedTranscript
import com.phenomena.study.learning.UniversityPedagogy
import com.phenomena.study.integration.ElevenReaderBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.phenomena.study.model.*
import com.phenomena.study.player.*
import com.phenomena.study.service.SeminarPlaybackService

/** Three intentional modes share one control room. Guided is an unbroken
 * lesson-specific narration chain; Long and Radio use the foreground media
 * service. Imported books only participate in Guided; no mismatched long form.
 */
@Composable
fun UnifiedListeningScreen(
    lesson:Lesson, imported:ImportedDocument?, player:StudyPlayer, catalog:Catalog,
    onChooseLesson:(String)->Unit,onAudioPick:()->Unit,onShare:(String)->Unit
) {
    val context= LocalContext.current
    val scope=rememberCoroutineScope()
    var eyesFree by remember { mutableStateOf(false) }
    var handoffStatus by remember { mutableStateOf("") }
    val listenPreferences=remember { context.getSharedPreferences("phenomena_unified_v31",Context.MODE_PRIVATE) }
    var mode by remember(lesson.id,imported?.id) {
        mutableStateOf(if(imported!=null) "Guided"
            else listenPreferences.getString("last_mode","Guided").orEmpty().takeIf {
                it=="Guided" || it=="Long" || it=="Radio" || it=="University"
            } ?: "Guided")
    }
    var showSettings by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val guidedSections=remember(lesson.id,imported?.id) {
        if(imported!=null) emptyList() else GuidedTranscript.sections(lesson)
    }
    val guidedBeats=remember(lesson.id,imported?.id) {
        if(imported!=null) toSpeechChunks(imported.content) else
            GuidedTranscript.beats(lesson) { content-> toSpeechChunks(content,maxChunk=2000) }
    }
    val modules=remember(lesson.workId,catalog.seminars) {
        catalog.seminars.filter { it.workId==lesson.workId }
    }
    val authorPrograms=remember(lesson.workId) { InstituteMaterials.lecturesFor(lesson.workId) }
    val prefs=remember { context.getSharedPreferences("phenomena_radio_v2",Context.MODE_PRIVATE) }
    var currentTitle by remember { mutableStateOf(prefs.getString("current_title","").orEmpty()) }
    var currentChunk by remember { mutableIntStateOf(prefs.getInt("chunk",0)) }
    var currentModule by remember { mutableStateOf(prefs.getString("current_module","").orEmpty()) }
    var backgroundMode by remember { mutableStateOf(prefs.getString("play_mode","LONG").orEmpty()) }
    var activeStatus by remember { mutableStateOf(prefs.getBoolean("running",false)) }
    DisposableEffect(context) {
        val receiver=object:BroadcastReceiver(){
            override fun onReceive(c:Context?,intent:Intent?) {
                currentTitle=prefs.getString("current_title","").orEmpty()
                currentChunk=prefs.getInt("chunk",0)
                currentModule=prefs.getString("current_module","").orEmpty()
                backgroundMode=prefs.getString("play_mode","LONG").orEmpty()
                activeStatus=prefs.getBoolean("running",false)
            }
        }
        ContextCompat.registerReceiver(context,receiver,
            IntentFilter(SeminarPlaybackService.ACTION_STATUS),ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val notificationRequest=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Android 13+ may hide media controls when permission is denied.
        // Playback is user-initiated and remains available through the foreground service.
    }
    fun startRadio(action:String, ids:List<String> = emptyList()) {
        player.stop()
        if(Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        SeminarPlaybackService.send(context,action,ids)
        currentTitle="Starting narration…"
    }
    LaunchedEffect(mode,lesson.id,imported?.id) {
        player.stop()
        if(imported!=null && mode=="Guided") {
            player.autoAdvance=true
            player.setSegments(guidedBeats)
        }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(17.dp)) {
        item {
            Eyebrow("PHENOMENA · THE UNIFIED PLAYER")
            Spacer(Modifier.height(5.dp))
            Display("The listening room.",size=36)
            Text("One place for guided study, long-form seminars, and unscripted radio.",
                color=Muted,fontSize=12.sp,lineHeight=18.sp)
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow(if(mode=="Radio") "YOUR LIBRARY STUDY · RADIO PLAYS ACROSS WORKS"
                    else "CURRENT STUDY · SELECTED WORK")
                Text(imported?.title?:lesson.title,fontSize=24.sp,lineHeight=29.sp,
                    color=Ink,fontFamily=FontFamily.Serif)
                Text(if(imported!=null)"Your imported ${imported.format} document"
                    else "${catalog.work(lesson.workId)?.title} · ${catalog.work(lesson.workId)?.author}",
                    fontSize=11.sp,color=Muted,lineHeight=17.sp)
                if(imported==null) {
                    OutlinedButton(onClick={showChooser=!showChooser},modifier=Modifier.fillMaxWidth()){
                        Icon(Icons.Default.LibraryBooks,null,Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Change lesson",modifier=Modifier.weight(1f))
                        Icon(Icons.Default.ExpandMore,null)
                    }
                    if(showChooser){
                        OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),
                            label={Text("Find section / thinker / concept")},singleLine=true)
                        val matches=remember(search,lesson.workId) {
                            catalog.lessons.filter {
                                it.title.contains(search,true) ||
                                it.section.contains(search,true) ||
                                (catalog.work(it.workId)?.author?.contains(search,true)==true)
                            }.take(40)
                        }
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max=240.dp)) {
                            items(matches,key={it.id}) { candidate->
                                TextButton(onClick={
                                    showChooser=false
                                    onChooseLesson(candidate.id)
                                },modifier=Modifier.fillMaxWidth()){
                                    Column(Modifier.fillMaxWidth()){
                                        Text(candidate.title,fontSize=13.sp,maxLines=2,
                                            color=Ink,overflow=TextOverflow.Ellipsis)
                                        Text(catalog.work(candidate.workId)?.title.orEmpty(),
                                            fontSize=10.sp,color=Muted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween) {
                Eyebrow("GRAND PLAYER · FOUR LISTENING MODES")
                OutlinedButton(onClick={eyesFree=!eyesFree}) {
                    Icon(if(eyesFree)Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle eyes-free interface",Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if(eyesFree)"Show text" else "Eyes-free",fontSize=11.sp)
                }
            }
            val options=listOf(listOf("Guided","Long"),listOf("Radio","University"))
            options.forEach { pair ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    pair.forEach { option ->
                        Surface(onClick={
                            if(mode!=option) {
                                player.stop()
                                SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PAUSE)
                                mode=option
                                if(imported==null) listenPreferences.edit()
                                    .putString("last_mode",option).apply()
                            }
                        },modifier=Modifier.weight(1f).heightIn(min=79.dp),
                            color=if(option==mode)Moss else CardSurface,
                            shape=RoundedCornerShape(16.dp),
                            border=BorderStroke(1.dp,if(option==mode)Mint else Line)) {
                            Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.spacedBy(7.dp)){
                                Icon(when(option){
                                    "Guided"->Icons.Default.AutoStories
                                    "Long"->Icons.Default.Headphones
                                    "Radio"->Icons.Default.Shuffle
                                    else->Icons.Default.School
                                },null,tint=if(option==mode)Mint else Muted,modifier=Modifier.size(28.dp))
                                Text(when(option){"Long"->"Long Format";else->option},
                                    fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }
        when(mode) {
            "Guided" -> {
                item {
                    Panel(Modifier.fillMaxWidth()){
                        Eyebrow("GUIDED · CONTINUOUS ORIGINAL COMMENTARY")
                        Display("From question to criticism.",size=25)
                        if(!eyesFree) Text("Synopsis, foundations, exposition, advanced interpretation, "
                            +"arguments, examples and objections flow in one continuous lecture.",
                            color=Muted,fontSize=12.sp,lineHeight=19.sp)
                        Button(onClick={
                            if(imported!=null) {
                                SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PAUSE)
                                player.setSegments(guidedBeats)
                                player.autoAdvance=true
                                player.play()
                            } else startRadio(SeminarPlaybackService.ACTION_PLAY,
                                listOf("guided:${lesson.id}"))
                        },modifier=Modifier.fillMaxWidth().heightIn(min=68.dp),
                            enabled=guidedBeats.isNotEmpty()){
                            Icon(Icons.Default.PlayArrow,"Begin guided lecture",Modifier.size(27.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Listen to complete Guided lecture",fontSize=15.sp)
                        }
                        if(imported!=null) {
                            Text("Imported files use the local narrator. Built-in lessons use the "
                                +"background audio service.",color=Muted,fontSize=11.sp)
                        } else AudioFirstTransport(currentTitle,currentModule,currentChunk,
                            activeStatus,backgroundMode)
                    }
                }
                if(!eyesFree) {
                    item {
                        Panel(Modifier.fillMaxWidth()){
                            Eyebrow("LESSON TRANSCRIPT · EDITORIAL COMMENTARY")
                            if(imported!=null) Body(guidedBeats.getOrNull(player.beatIndex)?.content
                                ?:"No extracted text is available.")
                            else {
                                Text("The entire script is arranged into ${guidedSections.size} conceptual movements.",
                                    color=Muted,fontSize=12.sp)
                                guidedSections.forEach { section ->
                                    Text(section.heading,color=Brass,fontSize=13.sp,
                                        fontWeight=FontWeight.SemiBold)
                                    Text(section.text.take(420)+(if(section.text.length>420)"…" else ""),
                                        color=Ink,fontSize=13.sp,lineHeight=21.sp)
                                }
                            }
                            Text("Transcript is available for optional study; no sustained reading is required.",
                                color=Muted,fontSize=10.sp)
                        }
                    }
                }
            }
            "Long" -> item {
                Panel(Modifier.fillMaxWidth()) {
                    Eyebrow("LONG FORMAT · SELECTED WORK ONLY")
                    Display("The full seminar.",size=26)
                    Body("Narration uses the persistent media service, including lock-screen playback. Only seminars belonging to your selected work appear here.",color=Muted)
                    if(imported!=null) EmptyNotice("Long Format belongs to the selected study work, not to arbitrary imported text. Choose a library lesson for a matching seminar.")
                    else {
                        if(modules.isEmpty()&&authorPrograms.isEmpty())
                            EmptyNotice("No long-format seminar is indexed to this work. Guided study remains available.")
                        modules.forEach { module ->
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Column(Modifier.weight(1f)){
                                    Text(module.title,color=Ink,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                                    Text("${module.chapters.size} chapters · ${module.spokenWords/125} estimated minutes",color=Muted,fontSize=10.sp)
                                }
                                IconButton(onClick={startRadio(SeminarPlaybackService.ACTION_PLAY,listOf(module.id))}) {
                                    Icon(Icons.Default.PlayArrow,"Play ${module.title}",tint=Mint)
                                }
                            }
                            Rule()
                        }
                        authorPrograms.forEach { item ->
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text("NEW · ${item.title}",Modifier.weight(1f),color=Ink,fontSize=13.sp)
                                IconButton(onClick={startRadio(SeminarPlaybackService.ACTION_PLAY,listOf(item.id))}) {
                                    Icon(Icons.Default.PlayArrow,"Play authored lecture",tint=Mint)
                                }
                            }
                        }
                        Button(onClick={startRadio(SeminarPlaybackService.ACTION_PLAY,
                            modules.map{it.id}+authorPrograms.map{it.id})},
                            modifier=Modifier.fillMaxWidth(),enabled=modules.isNotEmpty()||authorPrograms.isNotEmpty()) {
                            Icon(Icons.Default.PlayArrow,null)
                            Spacer(Modifier.width(8.dp));Text("Play complete work seminar")
                        }
                    }
                    AudioFirstTransport(currentTitle,currentModule,currentChunk,activeStatus,backgroundMode)
                }
            }
            "Radio" -> item {
                Panel(Modifier.fillMaxWidth()) {
                    Eyebrow("RADIO · RANDOMIZED PHILOSOPHICAL DISCOVERY")
                    Display("Let inquiry wander.",size=29)
                    Body("Shuffle across the whole archive: long-form seminars, guided lessons, and newly authored Institute lectures. Radio picks a new random mix, plays complete source passages, then reshuffles at the end.",color=Muted)
                    Text("The currently selected study does not restrict Radio. Every excerpt remains labeled with its true source.",
                        color=Brass,fontSize=11.sp,lineHeight=18.sp)
                    Button(onClick={startRadio(SeminarPlaybackService.ACTION_RADIO)},
                        modifier=Modifier.fillMaxWidth()){
                        Icon(Icons.Default.Shuffle,null,Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp));Text("Start a new random station")
                    }
                    BackgroundTransport(currentTitle,currentModule,currentChunk,activeStatus,backgroundMode)
                }
            }
            "University" -> item {
                Panel(Modifier.fillMaxWidth()){
                    Eyebrow("UNIVERSITY · LEARN THROUGH LISTENING")
                    Display("A professor in your pocket.",size=27)
                    if(!eyesFree) Body("Twelve progressive teaching movements: lived example, "
                        +"conceptual distinctions, original exposition, argument reconstruction, "
                        +"criticism, counterexample and teach-back.",color=Muted)
                    if(imported!=null) EmptyNotice("Select a PHENOMENA study lesson to hear a "
                        +"structured University course. Your imported text remains in Guided.")
                    else {
                        Text("${UniversityPedagogy.stages(lesson).size} teaching stages · ${lesson.title}",
                            color=Brass,fontSize=12.sp)
                        Button(onClick={startRadio(SeminarPlaybackService.ACTION_PLAY,
                            listOf("university:${lesson.id}"))},
                            modifier=Modifier.fillMaxWidth().heightIn(min=68.dp)){
                            Icon(Icons.Default.PlayArrow,"Begin University course",Modifier.size(27.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Listen to full University lecture",fontSize=15.sp)
                        }
                        AudioFirstTransport(currentTitle,currentModule,currentChunk,
                            activeStatus,backgroundMode)
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("ELEVENREADER · PREMIUM NARRATOR HANDOFF")
                Text("Hear the selected lecture using your voices in the ElevenReader app.",
                    color=Ink,fontSize=14.sp)
                Text("PHENOMENA sends a UTF-8 lecture file through Android's documented "
                    +"sharing system. Playback and voice choice take place in ElevenReader. "
                    +"Audio position will not automatically sync back.",
                    color=Muted,fontSize=11.sp,lineHeight=18.sp)
                Button(onClick={
                    scope.launch {
                        try {
                            val title= when(mode){
                                "Guided"->"Guided · ${lesson.title}"
                                "University"->"University · ${lesson.title}"
                                "Long"->"Long seminar · ${catalog.work(lesson.workId)?.title ?: lesson.title}"
                                else->"Radio · current study guide"
                            }
                            val manuscript=withContext(Dispatchers.Default) {
                                when {
                                    imported!=null->imported.content
                                    mode=="Guided"->GuidedTranscript.transcript(lesson)
                                    mode=="University"->UniversityPedagogy.transcript(lesson)
                                    mode=="Long"->{
                                        val main=modules.joinToString("\n\n") { module->
                                            module.title+"\n\n"+module.chapters.joinToString("\n\n"){ch->
                                                ch.title+"\n"+ch.text
                                            }
                                        }
                                        main.ifBlank{GuidedTranscript.transcript(lesson)}
                                    }
                                    else->GuidedTranscript.transcript(lesson)
                                }
                            }
                            val export=withContext(Dispatchers.IO){
                                ElevenReaderBridge.prepare(context,title,manuscript)
                            }
                            player.stop()
                            SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PAUSE)
                            val destination=ElevenReaderBridge.open(context,export)
                            handoffStatus=if(destination==ElevenReaderBridge.Destination.ELEVENREADER)
                                "Shared to ElevenReader. Select your preferred voice there."
                            else "Choose ElevenReader from the Android share sheet."
                        } catch(e:Exception) {
                            handoffStatus="Couldn't share lecture: ${e.message ?: "unknown error"}"
                        }
                    }
                },modifier=Modifier.fillMaxWidth().heightIn(min=58.dp)) {
                    Icon(Icons.Default.Share,"Send lecture to ElevenReader",Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Open lecture in ElevenReader",fontSize=15.sp)
                }
                if(handoffStatus.isNotBlank()) Text(handoffStatus,color=Brass,fontSize=11.sp)
            }
        }
        if(!eyesFree) item {
            OutlinedButton(onClick=onAudioPick,modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AudioFile,null)
                Spacer(Modifier.width(8.dp));Text("Play your own audio recording")
            }
            if(player.mediaPlaying) TextButton(onClick={player.toggleAudio()}) {
                Text("Pause / resume recording")
            }
            if(imported!=null) TextButton(onClick={onShare(imported.content)}) {
                Text("Share imported passage with an external reader")
            }
        }
    }
}

