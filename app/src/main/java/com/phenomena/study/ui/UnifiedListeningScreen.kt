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
    val listenPreferences=remember { context.getSharedPreferences("phenomena_unified_v31",Context.MODE_PRIVATE) }
    var mode by remember(lesson.id,imported?.id) {
        mutableStateOf(if(imported!=null) "Guided"
            else listenPreferences.getString("last_mode","Guided").orEmpty().takeIf {
                it=="Guided" || it=="Long" || it=="Radio"
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
        if(mode=="Guided"){
            player.autoAdvance=true
            player.setSegments(guidedBeats)
        } else player.stop()
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
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Guided","Long","Radio").forEach { option ->
                    Surface(onClick={
                        if(mode!=option) {
                            player.stop()
                            if(option=="Guided")
                                SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PAUSE)
                            mode=option
                            if(imported==null) listenPreferences.edit().putString("last_mode",option).apply()
                        }
                    },modifier=Modifier.weight(1f),
                        color=if(option==mode)Moss else CardSurface,
                        shape=RoundedCornerShape(16.dp),
                        border=BorderStroke(1.dp,if(option==mode)Mint else Line)) {
                        Column(Modifier.padding(vertical=14.dp,horizontal=5.dp),
                            horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(5.dp)){
                            Icon(when(option){
                                "Guided"->Icons.Default.School
                                "Long"->Icons.Default.Headphones
                                else->Icons.Default.Shuffle
                            },null,tint=if(option==mode)Mint else Muted)
                            Text(when(option){"Long"->"Long Format";else->option},fontSize=11.sp,
                                fontWeight=FontWeight.SemiBold,color=Ink)
                        }
                    }
                }
            }
        }
        when(mode) {
            "Guided" -> {
                item {
                    Panel(Modifier.fillMaxWidth()){
                        Eyebrow("GUIDED · ONE CONTINUOUS TRANSCRIPT")
                        Display(if(imported!=null)"Read your document." else "From question to criticism.",size=25)
                        Text(if(imported!=null)
                            "Your imported text is narrated continuously through TTS-safe chunks."
                        else "Synopsis → foundations → exposition → advanced analysis → argument → example → comparison → critique → seminar → debate.",
                            color=Muted,fontSize=12.sp,lineHeight=19.sp)
                        LinearProgressIndicator(progress={
                            if(guidedBeats.isEmpty())0f else (player.beatIndex+1f)/guidedBeats.size
                        },modifier=Modifier.fillMaxWidth(),color=Mint,trackColor=Cream)
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.Center) {
                            IconButton(onClick={player.jump(-1)},enabled=guidedBeats.isNotEmpty()){
                                Icon(Icons.Default.SkipPrevious,"Previous guided passage",tint=Mint)
                            }
                            Spacer(Modifier.width(20.dp))
                            FilledIconButton(onClick={
                                SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_STOP)
                                player.toggleSpeech()
                            },enabled=guidedBeats.isNotEmpty(),modifier=Modifier.size(62.dp),
                                colors=IconButtonDefaults.filledIconButtonColors(containerColor=Mint)) {
                                Icon(if(player.speaking)Icons.Default.Stop else Icons.Default.PlayArrow,
                                    "Play / stop continuous guided study",Modifier.size(32.dp))
                            }
                            Spacer(Modifier.width(20.dp))
                            IconButton(onClick={player.jump(1)},enabled=guidedBeats.isNotEmpty()){
                                Icon(Icons.Default.SkipNext,"Next guided passage",tint=Mint)
                            }
                        }
                        Text(player.status,color=Muted,fontSize=11.sp)
                        Text("Passage ${if(guidedBeats.isEmpty())0 else player.beatIndex+1} of ${guidedBeats.size} · Auto-advance enabled",
                            color=Brass,fontSize=11.sp)
                        Text("This is one logically continuous narration; speech is chunked only to meet Android TTS engine limits.",
                            color=Muted,fontSize=10.sp,lineHeight=16.sp)
                    }
                }
                item {
                    Panel(Modifier.fillMaxWidth()){
                        Eyebrow("SYNCHRONIZED TRANSCRIPT")
                        Text(guidedBeats.getOrNull(player.beatIndex)?.title?:"No passage available",
                            fontWeight=FontWeight.SemiBold,color=Brass,fontSize=14.sp)
                        SelectionContainer {
                            Body(guidedBeats.getOrNull(player.beatIndex)?.content.orEmpty())
                        }
                        OutlinedButton(onClick={showSettings=!showSettings},
                            modifier=Modifier.fillMaxWidth()) {
                            Text(if(showSettings)"Hide narrator settings" else "Narrator · voice and speed",
                                modifier=Modifier.weight(1f))
                            Icon(Icons.Default.Tune,null)
                        }
                        if(showSettings){
                            NarratorSettingsPanel(player)
                            Text("Speaking rate: ${"%.2f".format(player.speechRate)}×",color=Muted,fontSize=11.sp)
                            Slider(player.speechRate,onValueChange=player::setRate,valueRange=0.65f..1.5f)
                        }
                    }
                }
                if(imported==null) {
                    item {RowHeadline("Teaching sequence","${guidedSections.size} MOVEMENTS")}
                    items(guidedSections,key={it.heading}) { part ->
                        Surface(color=CardSurface,shape=RoundedCornerShape(15.dp),
                            border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                Text(part.heading,color=Ink,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                                Text(part.text,color=Muted,fontSize=11.sp,lineHeight=17.sp,
                                    maxLines=3,overflow=TextOverflow.Ellipsis)
                            }
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
                    BackgroundTransport(currentTitle,currentModule,currentChunk,activeStatus,backgroundMode)
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
                    BackgroundTransport(currentTitle,currentChunk,activeStatus,backgroundMode)
                }
            }
        }
        item {
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

@Composable
private fun BackgroundTransport(title:String,module:String,chunk:Int,active:Boolean,mode:String) {
    val context=LocalContext.current
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Rule()
        Eyebrow("BACKGROUND AUDIO CONTROLS")
        if(module.isNotBlank()) Text(module,
            color=Brass,fontSize=11.sp,lineHeight=16.sp)
        Text(if(title.isNotBlank()) title else "No background chapter selected",
            color=Ink,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
        Text("${if(active)"Active" else "Last saved"} · ${mode.lowercase()} · segment ${chunk+1}",
            color=Muted,fontSize=11.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PREVIOUS)},
                modifier=Modifier.weight(1f),contentPadding=PaddingValues(9.dp)){
                Icon(Icons.Default.SkipPrevious,"Previous",Modifier.size(18.dp))
            }
            OutlinedButton(onClick={SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_RESUME)},
                modifier=Modifier.weight(1f),contentPadding=PaddingValues(9.dp)){
                Icon(Icons.Default.PlayArrow,"Resume",Modifier.size(18.dp))
            }
            OutlinedButton(onClick={SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PAUSE)},
                modifier=Modifier.weight(1f),contentPadding=PaddingValues(9.dp)){
                Icon(Icons.Default.Pause,"Pause",Modifier.size(18.dp))
            }
            OutlinedButton(onClick={SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_NEXT)},
                modifier=Modifier.weight(1f),contentPadding=PaddingValues(9.dp)){
                Icon(Icons.Default.SkipNext,"Next",Modifier.size(18.dp))
            }
            OutlinedButton(onClick={SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_STOP)},
                modifier=Modifier.weight(1f),contentPadding=PaddingValues(9.dp)){
                Icon(Icons.Default.Stop,"Stop",Modifier.size(18.dp))
            }
        }
        Text("Requires a compatible installed Android speech engine. Long and Radio keep running with the screen off; Guided is controlled by the in-app player.",
            color=Muted,fontSize=10.sp,lineHeight=17.sp)
    }
}
