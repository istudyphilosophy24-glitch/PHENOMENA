package com.phenomena.study.ui

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.service.SeminarPlaybackService
import org.json.JSONArray
import org.json.JSONObject

/** Large touch targets and accessible semantics; never requires reading text
 * during playback. Bookmark is a chapter/chunk cursor, not exact-word timing. */
@Composable
fun AudioFirstTransport(title:String,source:String,chunk:Int,playing:Boolean,mode:String) {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("phenomena_radio_v2",Context.MODE_PRIVATE)}
    val markPrefs=remember {context.getSharedPreferences("phenomena_audio_marks_v4",Context.MODE_PRIVATE)}
    var bookmarks by remember {mutableStateOf(loadBookmarks(markPrefs.getString("marks","[]")))}
    var sleepMessage by remember {mutableStateOf("")}
    fun send(action:String,minutes:Int=0) =
        SeminarPlaybackService.send(context,action,sleepMinutes=minutes)
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)){
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq,"Audio now playing",tint=Mint,modifier=Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(source.ifBlank{"PHENOMENA · Grand Player"},color=Brass,fontSize=12.sp)
                Text(title.ifBlank{"Select a lecture"},color=Ink,fontSize=17.sp,lineHeight=22.sp)
                Text("${if(playing)"Playing" else "Paused / saved"} · ${mode.lowercase()} · segment ${chunk+1}",
                    color=Muted,fontSize=11.sp)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp),
            verticalAlignment=Alignment.CenterVertically){
            OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_PREVIOUS)},
                modifier=Modifier.weight(1f).height(62.dp),contentPadding=PaddingValues(4.dp)) {
                Icon(Icons.Default.SkipPrevious,"Previous chapter",Modifier.size(30.dp))
            }
            Button(onClick={send(SeminarPlaybackService.ACTION_RESUME)},
                modifier=Modifier.weight(1.4f).height(62.dp),contentPadding=PaddingValues(4.dp)){
                Icon(Icons.Default.PlayArrow,"Resume audio",Modifier.size(32.dp))
            }
            OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_PAUSE)},
                modifier=Modifier.weight(1.1f).height(62.dp),contentPadding=PaddingValues(4.dp)){
                Icon(Icons.Default.Pause,"Pause audio",Modifier.size(30.dp))
            }
            OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_NEXT)},
                modifier=Modifier.weight(1f).height(62.dp),contentPadding=PaddingValues(4.dp)){
                Icon(Icons.Default.SkipNext,"Next chapter",Modifier.size(30.dp))
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            OutlinedButton(onClick={
                val modules=runCatching {
                    JSONArray(prefs.getString("module_queue","[]")?:"[]").let{a->
                        (0 until a.length()).map{a.getString(it)}
                    }
                }.getOrDefault(emptyList())
                if(modules.isNotEmpty()) {
                    val item=JSONObject().put("title",title.ifBlank{"Untitled passage"})
                        .put("source",source).put("chunk",chunk)
                        .put("mode",mode).put("modules",JSONArray(modules))
                        .put("at",System.currentTimeMillis())
                    val updated=listOf(item)+bookmarks.take(29)
                    markPrefs.edit().putString("marks",JSONArray(updated).toString()).apply()
                    bookmarks=updated
                    sleepMessage="Audio bookmark saved at segment ${chunk+1}"
                } else sleepMessage="Start a lecture before saving a bookmark."
            },modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                Icon(Icons.Default.BookmarkAdd,"Bookmark current passage",Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp));Text("Bookmark",fontSize=12.sp)
            }
            OutlinedButton(onClick={send(SeminarPlaybackService.ACTION_STOP)},
                modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                Icon(Icons.Default.Stop,"Stop listening",Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp));Text("Stop",fontSize=12.sp)
            }
        }
        Eyebrow("SLEEP TIMER")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            listOf(15,30,60).forEach{minutes->
                OutlinedButton(onClick={
                    send(SeminarPlaybackService.ACTION_SLEEP,minutes)
                    sleepMessage="Sleep timer: $minutes minutes"
                },modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=5.dp,vertical=11.dp)){
                    Text("${minutes}m",fontSize=13.sp)
                }
            }
            OutlinedButton(onClick={
                send(SeminarPlaybackService.ACTION_END_CHAPTER)
                sleepMessage="Stopping after this chapter"
            },modifier=Modifier.weight(1.6f),contentPadding=PaddingValues(horizontal=5.dp,vertical=11.dp)){
                Text("End chapter",fontSize=11.sp)
            }
        }
        if(sleepMessage.isNotBlank()) Text(sleepMessage,color=Mint,fontSize=12.sp)
        if(bookmarks.isNotEmpty()){
            var show by remember {mutableStateOf(false)}
            TextButton(onClick={show=!show},modifier=Modifier.fillMaxWidth()){
                Text("Saved audio bookmarks (${bookmarks.size})",modifier=Modifier.weight(1f))
                Icon(if(show)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
            }
            if(show) bookmarks.take(12).forEach{mark->
                val label=mark.optString("title","Saved chapter")
                OutlinedButton(onClick={
                    val a=mark.optJSONArray("modules")?:JSONArray()
                    val ids=(0 until a.length()).map{a.getString(it)}
                    if(ids.isNotEmpty()) SeminarPlaybackService.send(context,
                        SeminarPlaybackService.ACTION_PLAY,ids,
                        seekCursor=mark.optInt("chunk",0))
                },modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.Replay,"Resume bookmark",Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)){
                        Text(label,color=Ink,fontSize=12.sp,maxLines=2)
                        Text(mark.optString("source",""),fontSize=10.sp,color=Muted,maxLines=1)
                    }
                }
            }
        }
        Text("Android media-session playback • lock-screen and headset controls. "
            +"Bookmark accuracy is to the speech chunk rather than the exact word.",
            color=Muted,fontSize=10.sp,lineHeight=16.sp)
    }
}
private fun loadBookmarks(raw:String?):List<JSONObject> = runCatching {
    val array=JSONArray(raw?:"[]")
    (0 until array.length()).map{array.getJSONObject(it)}
}.getOrDefault(emptyList())
