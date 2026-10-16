package com.phenomena.study.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.phenomena.study.data.ImportedDocument
import com.phenomena.study.model.Lesson
import com.phenomena.study.model.Beat
import com.phenomena.study.player.StudyPlayer
import com.phenomena.study.player.toSpeechChunks
import com.phenomena.study.reader.ParagraphIndex

/** PHENOMENA 2.0 source-aware reading environment.
 * Source passage means the exact imported plaintext span OR authored study-guide text;
 * it must never imply original-book quotations were bundled by the app. */
@Composable
fun GreatReaderScreen(
    lesson: Lesson,
    imported: ImportedDocument?,
    player: StudyPlayer,
    onSaveNote: (String, String) -> Unit,
    onChooseImport: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("phenomena_reader_v2",0) }
    var section by rememberSaveable(lesson.id, imported?.id) { mutableStateOf("Exposition") }
    val document = remember(lesson.id, imported?.id, imported?.content, section) {
        if (imported != null) ParagraphIndex.forImport(imported) else ParagraphIndex.forLesson(lesson, section)
    }
    var index by rememberSaveable(document.id) {
        mutableIntStateOf(prefs.getInt("cursor:${document.id}",0))
    }
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var tab by rememberSaveable(document.id) { mutableStateOf("INTERPRETATION") }
    var noteOpen by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    val current = document.paragraphs.getOrNull(index.coerceIn(0, (document.paragraphs.size-1).coerceAtLeast(0)))
    fun moveTo(next: Int) {
        index = next.coerceIn(0, (document.paragraphs.size-1).coerceAtLeast(0))
        prefs.edit().putInt("cursor:${document.id}",index).apply()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Eyebrow("THE GREAT READER · VERSION 2")
            Spacer(Modifier.height(6.dp))
            Display("The discipline of attention", size = 31)
            Text("Select a paragraph, follow its argument, and record your reading.",
                color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow(document.provenance)
                Text(document.title, color = Ink, fontFamily = FontFamily.Serif,
                    fontSize = 23.sp, lineHeight = 29.sp)
                if (imported == null) {
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Synopsis", "Exposition", "Advanced", "Comparison", "Criticism").forEach { label ->
                            LabelTag(label, selected = section == label, onClick = { section = label })
                        }
                    }
                } else {
                    Text("Your authorized import · Source offsets are local to the extracted text.",
                        color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onChooseImport) {
                    Icon(Icons.Default.LibraryBooks,null,modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp)); Text("Open another imported work")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("PASSAGE NAVIGATOR")
                    Text("Paragraph ${(index+1).coerceAtMost(document.paragraphs.size)} of ${document.paragraphs.size}",
                        color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedIconButton(onClick = {moveTo(index-1)},enabled=index>0) {
                    Icon(Icons.Default.ChevronLeft,"Previous paragraph")
                }
                Spacer(Modifier.width(7.dp))
                OutlinedIconButton(onClick = {moveTo(index+1)},enabled=index+1<document.paragraphs.size) {
                    Icon(Icons.Default.ChevronRight,"Next paragraph")
                }
            }
            if (document.paragraphs.isNotEmpty()) LinearProgressIndicator(
                progress = { (index+1).toFloat()/document.paragraphs.size },
                modifier = Modifier.fillMaxWidth(), color = Brass)
        }
        if (current == null) {
            item { EmptyNotice("No readable paragraphs. Import a selectable-text edition or choose a guided lesson.") }
        } else {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow(if (imported == null) "AUTHORED COMMENTARY" else "EXTRACTED SOURCE TEXT",Modifier.weight(1f))
                        Text("§ ${current.ordinal}",color = Brass,fontSize=12.sp)
                    }
                    SelectionContainer {
                        Text(current.text, color = Ink, fontSize=(17*scale).sp,
                            lineHeight=(29*scale).sp, fontFamily = FontFamily.Serif)
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        OutlinedButton(onClick = {
                            player.setSegments(toSpeechChunks(current.text));player.play()
                        }) {
                            Icon(Icons.Default.VolumeUp,null,Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp));Text("Listen")
                        }
                        OutlinedButton(onClick = { noteOpen = true }) {
                            Icon(Icons.Default.EditNote,null,Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp));Text("Annotate")
                        }
                    }
                    Text("Text positions: ${current.startOffset}–${current.endOffsetExclusive} (extracted characters)",
                        color=Muted,fontSize=10.sp)
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Eyebrow("INTERPRETIVE SCAFFOLD")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        listOf("INTERPRETATION","ARGUMENT","OBJECTIONS","PROVENANCE").forEach { label ->
                            LabelTag(label,label==tab){tab=label}
                        }
                    }
                    when(tab) {
                        "INTERPRETATION" -> if (imported != null) {
                            Body("This is imported primary or secondary text. The app cannot guarantee " +
                                "which printed paragraph this extracted segment corresponds to. " +
                                "Read it carefully before applying the nearby lesson's general commentary.", color=Muted)
                        } else {
                            Body(lesson.scaffold.getOrNull(index%lesson.scaffold.size.coerceAtLeast(1))?.body
                                ?: lesson.exposition)
                        }
                        "ARGUMENT" -> {
                            lesson.argument.forEachIndexed { i, step ->
                                Text("${i+1} · ${step.title}",color=Brass,fontWeight=FontWeight.SemiBold)
                                Body(step.detail)
                            }
                        }
                        "OBJECTIONS" -> {
                            Body(lesson.critique)
                            Rule()
                            Eyebrow("COMPARATIVE READING")
                            Body(lesson.comparison)
                        }
                        "PROVENANCE" -> Body(document.sourceNote,color=Muted)
                    }
                    if (imported != null) Text("The adjacent analysis is thematic guidance from the selected study lesson, " +
                        "not a verified commentary on this exact imported paragraph.",color=Muted,fontSize=11.sp)
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("READING COMFORT")
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatSize,null,tint=Brass)
                    Slider(value=scale,onValueChange={scale=it},valueRange=0.85f..1.4f,
                        modifier=Modifier.weight(1f).padding(horizontal=12.dp))
                    Text("${(scale*100).toInt()}%",color=Muted,fontSize=12.sp)
                }
            }
        }
    }
    if (noteOpen && current != null) AlertDialog(
        onDismissRequest = {noteOpen=false},
        title = { Text("Paragraph ${current.ordinal} · Field note") },
        text = { OutlinedTextField(value=noteText,onValueChange={noteText=it},
            label={ Text("Your interpretation or objection") }, minLines=4,maxLines=8) },
        confirmButton = { TextButton(enabled=noteText.isNotBlank(),onClick={
            onSaveNote("${document.title} · P${current.ordinal}",
                "${current.id}\n${noteText.trim()}")
            noteText="";noteOpen=false
        }){Text("Save annotation")} },
        dismissButton = { TextButton(onClick={noteOpen=false}){Text("Cancel")} }
    )
}
