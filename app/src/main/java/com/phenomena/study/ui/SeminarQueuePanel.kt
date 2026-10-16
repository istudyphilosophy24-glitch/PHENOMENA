package com.phenomena.study.ui

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.phenomena.study.model.Lesson
import com.phenomena.study.data.InstituteMaterials
import com.phenomena.study.data.Catalog
import com.phenomena.study.service.SeminarPlaybackService

/** Compact multi-module queue editor. Service owns the speech and survives the Activity. */
@Composable
fun SeminarQueuePanel(catalog: Catalog, lesson: Lesson?, onStart: () -> Unit = {}) {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("phenomena_radio_v2",Context.MODE_PRIVATE)}
    var expanded by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    // A queue is scoped to the selected lesson's work, NEVER the first global seminar.
    // Users may explicitly opt into other works through the editor.
    val authored = remember(lesson?.workId) { InstituteMaterials.lecturesFor(lesson?.workId.orEmpty()) }
    val initialIds = remember(lesson?.workId, catalog.seminars) {
        catalog.seminars.filter { it.workId == lesson?.workId }.map { it.id }.take(24)
    }
    val selected = remember(lesson?.workId) {
        mutableStateListOf<String>().apply { addAll(initialIds) }
    }
    var includeOtherWorks by remember(lesson?.workId) { mutableStateOf(false) }
    val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) {
            onStart()
            SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PLAY,selected.toList())
            permissionMessage=""
        } else permissionMessage="Enable notifications to display media controls while listening."
    }
    var savedChapter by remember { mutableStateOf(prefs.getString("current_title","").orEmpty()) }
    var queueCursor by remember { mutableIntStateOf(prefs.getInt("chunk",0)) }
    DisposableEffect(context) {
        val receiver=object:BroadcastReceiver(){
            override fun onReceive(ctx:Context?,intent:Intent?) {
                savedChapter=prefs.getString("current_title","").orEmpty()
                queueCursor=prefs.getInt("chunk",0)
            }
        }
        ContextCompat.registerReceiver(context,receiver,
            IntentFilter(SeminarPlaybackService.ACTION_STATUS), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("THE GREAT SEMINAR 3.0")
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Background listening queue",Modifier.weight(1f),color=Ink,fontSize=17.sp)
            Icon(Icons.Default.Headphones,null,tint=Brass)
        }
        val words = remember(catalog.seminars) { catalog.seminars.sumOf { it.spokenWords } }
        Text("${"%.1f".format(words / (125.0 * 60))} hours of TTS-ready scripts at 125 WPM · includes repeated exercises, not a verified count of unique lectures.",
            color = Brass, fontSize = 11.sp, lineHeight = 17.sp)
        Text("Selected topic: ${lesson?.title ?: "None"} · ${authored.size} new authored programs available for this work.", color=Mint, fontSize=12.sp, lineHeight=18.sp)
        Text("Narration continues with the screen off. Module and chapter progress are saved locally; " +
            "resuming restarts the saved speech chunk rather than the exact word.",
            color=Muted,fontSize=11.sp,lineHeight=17.sp)
        OutlinedButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth()) {
            Text("Edit queue · ${selected.size} seminar(s)",modifier=Modifier.weight(1f))
            Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
        }
        if(expanded) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=includeOtherWorks,onCheckedChange={includeOtherWorks=it})
                Text("Allow adding seminars from other works",color=Muted,fontSize=12.sp)
            }
            OutlinedTextField(value=search,onValueChange={search=it},singleLine=true,
                label={Text("Find a seminar")},modifier=Modifier.fillMaxWidth())
            // Curated Institute lectures use IDs distinct from the 85 legacy modules.
            // They enter the same persistable background queue only when selected.
            data class Option(val id:String,val title:String,val subtitle:String,val chapterCount:Int,val words:Int)
            val filtered = remember(search,includeOtherWorks,catalog.seminars,lesson?.workId) {
                val legacy = catalog.seminars.filter { includeOtherWorks || it.workId == lesson?.workId }
                    .map { Option(it.id,it.title,it.description,it.chapters.size,it.spokenWords) }
                val institute = InstituteMaterials.lectures.filter {
                    includeOtherWorks || it.workId == lesson?.workId
                }.map { Option(it.id,"NEW · "+it.title,it.subtitle,it.paragraphs.size,it.words) }
                (legacy+institute).filter {
                    it.title.contains(search,true) || it.subtitle.contains(search,true)
                }
            }
            LazyColumn(modifier=Modifier.fillMaxWidth().heightIn(max=300.dp),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                items(filtered,key={it.id}) { item ->
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=item.id in selected,onCheckedChange={isChecked ->
                            if(isChecked && selected.size < 24 && item.id !in selected) selected.add(item.id)
                            if(!isChecked) selected.remove(item.id)
                        })
                        Column(Modifier.weight(1f)) {
                            Text(item.title,color=Ink,fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                            Text("${item.chapterCount} chapters · ${item.words/125} estimated min",
                                color=Muted,fontSize=10.sp)
                        }
                    }
                }
            }
            Text("Original 70-hour modules and new authored lectures are separate. Other works require an explicit opt-in. Maximum 24 selections.",
                color=Muted,fontSize=10.sp)
        }
        Button(onClick={
            if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(
                context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onStart()
                SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PLAY,selected.toList())
            }
            savedChapter="Starting narrated queue…"
        },enabled=selected.isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PlayArrow,null,modifier=Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp));Text("Play selected seminars in background")
        }
        if(permissionMessage.isNotBlank()) Text(permissionMessage,color=Muted,fontSize=11.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick={SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_RESUME)},modifier=Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow,null,modifier=Modifier.size(15.dp));Text("Resume",fontSize=11.sp)
            }
            OutlinedButton(onClick={SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PAUSE)},modifier=Modifier.weight(1f)) {
                Icon(Icons.Default.Pause,null,modifier=Modifier.size(15.dp));Text("Pause",fontSize=11.sp)
            }
            OutlinedButton(onClick={SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_STOP)},modifier=Modifier.weight(1f)) {
                Icon(Icons.Default.Stop,null,modifier=Modifier.size(15.dp));Text("Stop",fontSize=11.sp)
            }
        }
        if(savedChapter.isNotBlank()) Text("Saved position: ${savedChapter} · speech chunk ${queueCursor+1}",
            color=Muted,fontSize=11.sp,lineHeight=17.sp,maxLines=2)
    }
}
