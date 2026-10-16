package com.phenomena.study.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.phenomena.study.data.Catalog
import com.phenomena.study.data.ImportedDocument
import com.phenomena.study.data.InstituteMaterials
import com.phenomena.study.model.Lesson
import com.phenomena.study.player.StudyPlayer
import com.phenomena.study.player.UnifiedPrograms
import com.phenomena.study.player.toSpeechChunks
import com.phenomena.study.service.SeminarPlaybackService

/** One media room; all packaged modes use the same Android foreground service. */
@Composable
fun UnifiedPlayerScreen(
    lesson:Lesson, imported:ImportedDocument?, player:StudyPlayer, catalog:Catalog,
    onChooseLesson:(String)->Unit, onAudioPick:()->Unit, onShare:(String)->Unit
) {
    val context=LocalContext.current
    val audioPrefs=remember { context.getSharedPreferences("phenomena_radio_v2",Context.MODE_PRIVATE) }
    val prefs=remember { context.getSharedPreferences("phenomena_unified_player_v3",Context.MODE_PRIVATE) }
    var mode by remember { mutableStateOf(prefs.getString("mode",UnifiedPrograms.GUIDED) ?: UnifiedPrograms.GUIDED) }
    var longSelection by remember(lesson.workId) { mutableStateOf("all") }
    var lessonSearch by remember { mutableStateOf("") }
    var chooseLesson by remember { mutableStateOf(false) }
    var longExpanded by remember { mutableStateOf(false) }
    var nowTitle by remember { mutableStateOf(audioPrefs.getString("current_title","").orEmpty()) }
    var nowProgramme by remember { mutableStateOf("") }
    var savedMode by remember { mutableStateOf(audioPrefs.getString("mode","").orEmpty()) }
    var progressIndex by remember { mutableIntStateOf(audioPrefs.getInt("chunk",0)) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isStarted by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }

    val work=catalog.work(lesson.workId)
    val longModules=remember(lesson.workId,catalog.seminars) {
        catalog.seminars.filter { it.workId==lesson.workId }
    }
    val original=remember(lesson.workId) { InstituteMaterials.lecturesFor(lesson.workId) }
    val guided=remember(lesson.id) { UnifiedPrograms.guidedSections(lesson) }
    val guideWords=remember(lesson.id) { UnifiedPrograms.sectionWordCount(guided) }
    val longIds=when(longSelection) {
        "all" -> (longModules.map { it.id } + original.map { it.id }).take(24)
        else -> listOf(longSelection)
    }
    val allCount=remember(catalog.lessons,catalog.seminars) {
        catalog.lessons.size + catalog.seminars.size + InstituteMaterials.lectures.size
    }
    val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // A denied permission does not preclude all TTS on Android 13+,
        // but proceeding without a visible media notification is confusing.
        if(granted) {
            permissionMessage=""
            player.stop()
            launchUnified(context,mode,lesson.id,longIds,imported,player)
            isStarted=true
        } else {
            permissionMessage="Allow notifications for visible lock-screen playback controls."
        }
    }
    DisposableEffect(context) {
        val receiver=object:BroadcastReceiver() {
            override fun onReceive(receiverContext:Context?,intent:Intent?) {
                nowTitle=audioPrefs.getString("current_title","").orEmpty()
                nowProgramme=intent?.getStringExtra("programme").orEmpty()
                savedMode=intent?.getStringExtra("mode").orEmpty()
                progressIndex=audioPrefs.getInt("chunk",0)
                progressTotal=intent?.getIntExtra("total",0)?:0
                isPaused=intent?.getBooleanExtra("paused",false)?:false
            }
        }
        ContextCompat.registerReceiver(context,receiver,
            IntentFilter(SeminarPlaybackService.ACTION_STATUS),ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    fun begin() {
        if(mode==UnifiedPrograms.LONG && longIds.isEmpty()) {
            permissionMessage="No long-form programmes are assigned to this work. Try Guided or Radio."
            return
        }
        if(mode==UnifiedPrograms.GUIDED && imported==null && guided.isEmpty()) {
            permissionMessage="No guided narration available for this section."
            return
        }
        if(Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED &&
            !(mode==UnifiedPrograms.GUIDED && imported!=null)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionMessage=""
            player.stop()
            launchUnified(context,mode,lesson.id,longIds,imported,player)
            isStarted=true
        }
    }
    LazyColumn(contentPadding=PaddingValues(horizontal=19.dp,vertical=22.dp),
        verticalArrangement=Arrangement.spacedBy(17.dp),modifier=Modifier.fillMaxSize()) {
        item {
            Eyebrow("PHENOMENA · THE LISTENING ROOM")
            Spacer(Modifier.height(5.dp))
            Display("One player. Every idea.",size=35)
            Body("Listen to a complete guided study, an extended lecture, or the entire library on shuffle.",
                color=Muted)
        }
        item {
            // A single, clearly legible three-mode selector.
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(UnifiedPrograms.GUIDED,"Guided","01"),
                    Triple(UnifiedPrograms.LONG,"Long Format","02"),
                    Triple(UnifiedPrograms.RADIO,"Radio","03")
                ).forEach { (id,name,number) ->
                    Surface(onClick={
                        if(mode!=id) {
                            mode=id
                            prefs.edit().putString("mode",id).apply()
                        }
                    },modifier=Modifier.weight(1f),
                        shape=RoundedCornerShape(17.dp),
                        color=if(mode==id) Moss else CardSurface,
                        border=BorderStroke(1.dp,if(mode==id) Mint else Line)) {
                        Column(Modifier.padding(horizontal=9.dp,vertical=14.dp),
                            verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(number,color=if(mode==id) Brass else Muted,fontSize=10.sp)
                            Icon(when(id) {
                                UnifiedPrograms.RADIO->Icons.Default.Shuffle
                                UnifiedPrograms.LONG->Icons.Default.Headphones
                                else->Icons.Default.AutoStories
                            },null,tint=if(mode==id) Color.White else Mint,
                                modifier=Modifier.size(23.dp))
                            Text(name,color=Ink,fontWeight=FontWeight.SemiBold,fontSize=11.sp,
                                maxLines=1)
                        }
                    }
                }
            }
        }
        item {
            Surface(color=CardRaised,shape=RoundedCornerShape(26.dp),
                border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(13.dp)) {
                        Surface(color=Moss,shape=RoundedCornerShape(15.dp),
                            modifier=Modifier.size(78.dp)) {
                            Box(contentAlignment=Alignment.Center) {
                                Icon(when(mode) {
                                    UnifiedPrograms.RADIO -> Icons.Default.Shuffle
                                    UnifiedPrograms.LONG -> Icons.Default.Headphones
                                    else -> Icons.Default.MenuBook
                                },null,tint=Color.White,modifier=Modifier.size(33.dp))
                            }
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Eyebrow(when(mode) {
                                UnifiedPrograms.RADIO -> "PHENOMENA RADIO"
                                UnifiedPrograms.LONG -> "THE GREAT SEMINAR"
                                else -> "THE GUIDED STUDY"
                            })
                            Text(when(mode) {
                                UnifiedPrograms.RADIO -> "A library without a fixed order"
                                UnifiedPrograms.LONG -> work?.title ?: lesson.title
                                else -> imported?.title ?: lesson.title
                            },fontFamily=FontFamily.Serif,color=Ink,fontSize=21.sp,lineHeight=26.sp,
                                maxLines=3,overflow=TextOverflow.Ellipsis)
                            Text(when(mode) {
                                UnifiedPrograms.RADIO -> "$allCount programmes · shuffled by programme"
                                UnifiedPrograms.LONG -> "${longModules.size} long seminars · ${original.size} authored lectures"
                                else -> if(imported!=null) "Personal import · read in app" else
                                    "${guided.size} ordered sections · ~${guideWords/125} min"
                            },fontSize=11.sp,color=Muted,lineHeight=17.sp)
                        }
                    }
                    HorizontalDivider(color=Line)
                    Text(when(mode) {
                        UnifiedPrograms.GUIDED -> "Synopsis → exposition → advanced analysis → example → comparisons → critique, with no manual track changes."
                        UnifiedPrograms.LONG -> "Whole lectures and semantic chapters for the selected work. Background playback included."
                        else -> "Guided explanations, long-form seminars and Institute lectures are shuffled without interrupting the order within each programme."
                    },fontSize=12.sp,color=Muted,lineHeight=18.sp)
                    Button(onClick={begin},modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=Mint,contentColor=Paper)) {
                        Icon(if(mode==UnifiedPrograms.RADIO) Icons.Default.Shuffle else Icons.Default.PlayArrow,
                            null,modifier=Modifier.size(20.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(when(mode) {
                            UnifiedPrograms.GUIDED->"Play continuous Guided"
                            UnifiedPrograms.LONG->"Play Long Format"
                            else->"Shuffle and play Radio"
                        })
                    }
                    if(permissionMessage.isNotBlank())
                        Text(permissionMessage,color=Brass,fontSize=11.sp)
                }
            }
        }
        if(mode!=UnifiedPrograms.RADIO) item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("YOUR SELECTED WORK")
                Text(lesson.title,color=Ink,fontFamily=FontFamily.Serif,fontSize=19.sp)
                Text("${work?.author.orEmpty()} · ${work?.title.orEmpty()}",color=Muted,fontSize=11.sp)
                if(imported==null || mode==UnifiedPrograms.LONG) {
                    OutlinedButton(onClick={chooseLesson=!chooseLesson},modifier=Modifier.fillMaxWidth()) {
                        Text("Change current study",modifier=Modifier.weight(1f))
                        Icon(if(chooseLesson) Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
                    }
                    if(chooseLesson) {
                        OutlinedTextField(value=lessonSearch,onValueChange={lessonSearch=it},
                            singleLine=true,modifier=Modifier.fillMaxWidth(),
                            leadingIcon={Icon(Icons.Default.Search,null)},
                            label={Text("Search study sections")})
                        val found=catalog.lessons.filter {
                            val author=catalog.work(it.workId)?.author.orEmpty()
                            lessonSearch.isBlank() ||
                                it.title.contains(lessonSearch,true) ||
                                it.section.contains(lessonSearch,true) ||
                                author.contains(lessonSearch,true)
                        }.take(70)
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max=250.dp)) {
                            items(found,key={it.id}) { candidate ->
                                TextButton(onClick={
                                    chooseLesson=false
                                    onChooseLesson(candidate.id)
                                },modifier=Modifier.fillMaxWidth()) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(candidate.title,color=Ink,fontSize=12.sp,maxLines=2)
                                        Text(catalog.work(candidate.workId)?.author.orEmpty(),
                                            color=Muted,fontSize=10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                if(mode==UnifiedPrograms.LONG) {
                    Rule()
                    OutlinedButton(onClick={longExpanded=!longExpanded},
                        modifier=Modifier.fillMaxWidth()) {
                        Text(if(longSelection=="all") "Entire selected work" else
                            (longModules.firstOrNull{it.id==longSelection}?.title
                                ?:original.firstOrNull{it.id==longSelection}?.title
                                ?: "Select lecture"),
                            modifier=Modifier.weight(1f),maxLines=2)
                        Icon(Icons.Default.ExpandMore,null)
                    }
                    if(longExpanded) {
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick={longSelection="all";longExpanded=false}) {
                                Text("Entire selected work (${longModules.size+original.size} programmes)")
                            }
                            longModules.forEach { item ->
                                TextButton(onClick={longSelection=item.id;longExpanded=false}) {
                                    Text(item.title,maxLines=2)
                                }
                            }
                            original.forEach { item ->
                                TextButton(onClick={longSelection=item.id;longExpanded=false}) {
                                    Text("Institute · ${item.title}",maxLines=2)
                                }
                            }
                        }
                    }
                }
                if(mode==UnifiedPrograms.GUIDED && imported==null) {
                    Rule()
                    Eyebrow("CONTINUOUS PLAY ORDER")
                    guided.forEach { section ->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle,null,tint=Mint,modifier=Modifier.size(15.dp))
                            Text(section.title,color=Ink,fontSize=12.sp)
                        }
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("NOW PLAYING · ${savedMode.uppercase()}")
                        Text(nowProgramme.ifBlank{ "Nothing playing" },color=Ink,fontSize=15.sp,
                            maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(nowTitle.ifBlank{ "Choose a mode and press Play" },
                            color=Muted,fontSize=11.sp,maxLines=3)
                    }
                    Icon(Icons.Default.GraphicEq,null,tint=Brass)
                }
                if(progressTotal>0) {
                    LinearProgressIndicator(progress={
                        ((progressIndex+1).toFloat()/progressTotal).coerceIn(0f,1f)
                    },modifier=Modifier.fillMaxWidth(),color=Mint)
                    Text("Speech chunk ${progressIndex+1} of $progressTotal · replay begins at chunk boundary",
                        fontSize=10.sp,color=Muted)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(onClick={
                        SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PREVIOUS)
                    },modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.SkipPrevious,"Previous programme or chapter",Modifier.size(18.dp))
                    }
                    OutlinedButton(onClick={
                        SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_RESUME)
                    },modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow,"Resume",Modifier.size(18.dp))
                    }
                    OutlinedButton(onClick={
                        SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PAUSE)
                    },modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.Pause,"Pause",Modifier.size(18.dp))
                    }
                    OutlinedButton(onClick={
                        SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_NEXT)
                    },modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.SkipNext,"Next programme or chapter",Modifier.size(18.dp))
                    }
                    OutlinedButton(onClick={
                        SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_STOP)
                        isStarted=false
                    },modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.Stop,"Stop",Modifier.size(18.dp))
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("VOICE & PLAYBACK")
                NarratorSettingsPanel(player)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Narration speed",modifier=Modifier.weight(1f),fontSize=12.sp)
                    Text("${"%.2f".format(player.speechRate)}×",color=Brass,fontSize=12.sp)
                }
                Slider(value=player.speechRate,onValueChange=player::setRate,
                    valueRange=.65f..1.5f)
                Text("This voice is used by the background listening service when playback starts. Changing it during a programme takes effect when that programme is restarted.",
                    color=Muted,fontSize=11.sp,lineHeight=16.sp)
                OutlinedButton(onClick=onAudioPick,modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.LibraryMusic,null)
                    Spacer(Modifier.width(8.dp));Text("Open your own audio file")
                }
                if(imported!=null && mode==UnifiedPrograms.GUIDED) {
                    OutlinedButton(onClick={onShare(imported.content)},
                        modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.IosShare,null)
                        Spacer(Modifier.width(7.dp));Text("Share imported text externally")
                    }
                }
            }
        }
    }
}

private fun launchUnified(
    context:Context,mode:String,lessonId:String,longIds:List<String>,
    imported:ImportedDocument?,player:StudyPlayer
) {
    if(mode==UnifiedPrograms.GUIDED && imported!=null) {
        SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_STOP)
        player.setSegments(toSpeechChunks(imported.content,2000))
        player.play()
        return
    }
    val list=when(mode) {
        UnifiedPrograms.GUIDED -> listOf(UnifiedPrograms.GUIDED_PREFIX+lessonId)
        UnifiedPrograms.LONG -> longIds
        else -> emptyList()
    }
    SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PLAY,list,mode)
}
