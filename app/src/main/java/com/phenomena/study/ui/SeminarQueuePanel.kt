package com.phenomena.study.ui

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
import com.phenomena.study.data.Catalog
import com.phenomena.study.service.SeminarPlaybackService

/** Compact multi-module queue editor. Service owns the speech and survives the Activity. */
@Composable
fun SeminarQueuePanel(catalog: Catalog, onStart: () -> Unit = {}) {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("phenomena_radio_v2",Context.MODE_PRIVATE)}
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val selected=remember { mutableStateListOf<String>() }
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
    if(selected.isEmpty() && catalog.seminars.isNotEmpty()) {
        // Initial selection only. Further selection changes are controlled by user.
        LaunchedEffect(catalog.seminars.first().id) { if(selected.isEmpty()) selected.add(catalog.seminars.first().id) }
    }
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("THE GREAT SEMINAR 2.0")
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Background listening queue",Modifier.weight(1f),color=Ink,fontSize=17.sp)
            Icon(Icons.Default.Headphones,null,tint=Brass)
        }
        val words = remember(catalog.seminars) { catalog.seminars.sumOf { it.spokenWords } }
        Text("${"%.1f".format(words / (125.0 * 60))} hours of TTS-ready scripts at 125 WPM · includes repeated exercises, not a verified count of unique lectures.",
            color = Brass, fontSize = 11.sp, lineHeight = 17.sp)
        Text("Narration continues with the screen off. Module and chapter progress are saved locally; " +
            "resuming restarts the saved speech chunk rather than the exact word.",
            color=Muted,fontSize=11.sp,lineHeight=17.sp)
        OutlinedButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth()) {
            Text("Edit queue · ${selected.size} seminar(s)",modifier=Modifier.weight(1f))
            Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
        }
        if(expanded) {
            OutlinedTextField(value=search,onValueChange={search=it},singleLine=true,
                label={Text("Find a seminar")},modifier=Modifier.fillMaxWidth())
            val filtered=remember(search,catalog.seminars) {
                catalog.seminars.filter { it.title.contains(search,true) || it.description.contains(search,true) }
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
                            Text("${item.chapters.size} chapters · ${item.spokenWords/125} estimated min",
                                color=Muted,fontSize=10.sp)
                        }
                    }
                }
            }
            Text("Maximum 24 modules per queue; selections play in the order chosen.",
                color=Muted,fontSize=10.sp)
        }
        Button(onClick={
            onStart()
            SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PLAY,selected.toList())
            savedChapter="Starting narrated queue…"
        },enabled=selected.isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PlayArrow,null,modifier=Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp));Text("Play selected seminars in background")
        }
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
