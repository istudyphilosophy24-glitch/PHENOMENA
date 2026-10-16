package com.phenomena.study.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.model.*
import com.phenomena.study.data.*
import com.phenomena.study.player.*

@Composable
fun ListenScreen(
    lesson: Lesson, imported: ImportedDocument?, player: StudyPlayer, catalog: Catalog,
    onChooseLesson: (String) -> Unit, onAudioPick: () -> Unit, onShare: (String) -> Unit
) {
    var track by remember(lesson.id, imported?.id) { mutableStateOf("Seminar") }
    var chooseLesson by remember { mutableStateOf(false) }
    val text = if (imported != null) imported.content else when (track) {
        "Synopsis" -> lesson.synopsis
        "Exposition" -> lesson.exposition
        "Advanced" -> lesson.advanced
        "Seminar" -> lesson.seminar
        "Debate" -> lesson.debate
        else -> lesson.seminar
    }
    val trackBeats = remember(lesson.id, imported?.id, track) {
        when {
            imported != null -> toSpeechChunks(imported.content)
            track == "Guided" -> lesson.beats
            else -> listOf(Beat(track, text, "Narration"))
        }
    }
    LaunchedEffect(trackBeats) { player.setSegments(trackBeats) }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Eyebrow("AN AUDIO-FIRST PHILOSOPHICAL SEMINAR")
            Display("The study player", Modifier.padding(top = 7.dp), 33)
            Body("Think through an argument with guided listening, commentary, and semantic navigation.", color = Muted)
        }
        item {
            Surface(color = Moss, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(color = Color(0xFF34584C), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(width = 90.dp, height = 111.dp)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.GraphicEq, null, tint = Color(0xFFE1C891))
                                Text("PHENO\nMENA", color = Color.White, fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 19.sp)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("CURRENT STUDY", fontSize = 10.sp, color = Color(0xFFE1C891), letterSpacing = 1.4.sp)
                            Text(imported?.title ?: lesson.title, fontSize = 22.sp, lineHeight = 26.sp,
                                fontFamily = FontFamily.Serif, color = Color.White, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            Text(if (imported != null) "IMPORTED ${imported.format.uppercase()}" else "${lesson.section} · ${catalog.work(lesson.workId)?.author}",
                                color = Color.White.copy(alpha = .65f), fontSize = 11.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .2f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (imported != null) "IMPORTED READING" else "${track.uppercase()} TRACK", color = Color(0xFFE1C891), fontSize = 11.sp,
                            modifier = Modifier.weight(1f))
                        Text("${if (trackBeats.isEmpty()) 0 else player.beatIndex + 1} / ${trackBeats.size}", color = Color.White, fontSize = 11.sp)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { player.jump(-1) }) { Icon(Icons.Default.SkipPrevious, "Previous concept", tint = Color.White, modifier = Modifier.size(30.dp)) }
                        Spacer(Modifier.width(14.dp))
                        FilledIconButton(onClick = { player.toggleSpeech() }, modifier = Modifier.size(62.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE1C891), contentColor = Moss)) {
                            Icon(if (player.speaking) Icons.Default.Stop else Icons.Default.PlayArrow, "Play or stop narration", modifier = Modifier.size(34.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        IconButton(onClick = { player.jump(1) }) { Icon(Icons.Default.SkipNext, "Next concept", tint = Color.White, modifier = Modifier.size(30.dp)) }
                    }
                    Text(player.status, fontSize = 11.sp, color = Color.White.copy(alpha = .8f), modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("Android TTS replays the current segment after stopping; it cannot pause at a precise word.",
                        fontSize = 10.sp, color = Color.White.copy(alpha = .54f), lineHeight = 15.sp)
                }
            }
        }
        if (imported == null) item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                RowHeadline("Narration track")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Synopsis", "Exposition", "Advanced", "Seminar", "Debate", "Guided").forEach { item ->
                        LabelTag(item, track == item) { track = item }
                    }
                }
                OutlinedButton(onClick = { chooseLesson = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Switch study section", modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ExpandMore, null)
                }
                DropdownMenu(expanded = chooseLesson, onDismissRequest = { chooseLesson = false }) {
                    catalog.lessons.forEach { l -> DropdownMenuItem(
                        text = { Text("${l.section} · ${l.title}", maxLines = 1) },
                        onClick = { chooseLesson = false; onChooseLesson(l.id) }) }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("VOICE / PACE")
                NarratorSettingsPanel(player)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Speaking rate", Modifier.weight(1f), fontSize = 12.sp)
                    Text("${"%.2f".format(player.speechRate)}×", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Slider(value = player.speechRate, onValueChange = player::setRate, valueRange = 0.65f..1.5f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = player.autoAdvance, onCheckedChange = { player.autoAdvance = it })
                    Text("Automatically advance to next semantic chapter", fontSize = 12.sp)
                }
                Text("Playback uses your selected installed Android speech engine, with no lesson export required.", fontSize = 11.sp, color = Muted)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                RowHeadline("Semantic chapters", "${trackBeats.size} MARKERS")
                trackBeats.forEachIndexed { i, beat ->
                    Surface(onClick = { player.jumpTo(i) }, color = if (player.beatIndex == i) Cream else Color.White.copy(alpha = .67f),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}".padStart(2, '0'), fontSize = 12.sp, color = Brass, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(beat.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(beat.kind, fontSize = 11.sp, color = Muted)
                            }
                            Icon(if (player.beatIndex == i) Icons.Default.VolumeUp else Icons.Default.ChevronRight,
                                null, tint = Moss, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("SYNCHRONIZED PASSAGE / CONTENT")
                Body(trackBeats.getOrNull(player.beatIndex)?.content ?: "Choose an audio track", color = Ink)
                Text("Transcript follows the selected semantic segment. Word-by-word TTS highlighting is not implemented.",
                    fontSize = 11.sp, color = Muted)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAudioPick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.LibraryMusic, null); Spacer(Modifier.width(8.dp)); Text("Play your own MP3 / recording with Media3")
                }
                if (player.mediaPlaying) OutlinedButton(onClick = { player.toggleAudio() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Pause, null); Spacer(Modifier.width(8.dp)); Text("Pause imported recording")
                }
                OutlinedButton(onClick = { onShare(text) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.IosShare, null); Spacer(Modifier.width(8.dp)); Text("Optional: share reading externally")
                }
                Text("This uses Android's sharing chooser. The target reading app must accept shared text; automatic ElevenReader playback cannot be guaranteed.",
                    fontSize = 11.sp, lineHeight = 17.sp, color = Muted)
            }
        }
    }
}
