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
import com.phenomena.study.data.*
import com.phenomena.study.model.*

@Composable
fun BookTile(work: Work, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, color = Color(work.color), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.height(204.dp).padding(17.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text(work.year, color = Color(0xFFDBD6C8), fontSize = 11.sp)
                Icon(Icons.Default.ArrowOutward, null, tint = Color(0xFFEAE6DD), modifier = Modifier.size(17.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(work.title, color = Color.White, fontFamily = FontFamily.Serif,
                    fontSize = 25.sp, lineHeight = 28.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                HorizontalDivider(color = Color.White.copy(alpha = .3f))
                Text(work.author.uppercase(), color = Color.White.copy(alpha = .84f), fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun LibraryScreen(catalog: Catalog, progress: List<LessonProgress>, onWork: (String) -> Unit, onLesson: (String) -> Unit) {
    val done = progress.count { it.completed }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(23.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("YOUR PHENOMENOLOGICAL LABORATORY")
                Display("Read deeper.\nListen closer.", size = 42)
                Body("An audio-first, section-by-section companion to the phenomenological tradition.", color = Muted)
            }
        }
        item {
            Surface(color = Moss, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("CONTINUE STUDYING", fontSize = 10.sp, color = Color(0xFFD9C18E), letterSpacing = 1.5.sp)
                        Text("§5 · The Ontological Analytic of Dasein", color = Color.White, fontFamily = FontFamily.Serif,
                            fontSize = 21.sp, lineHeight = 27.sp)
                        Text("HEIDEGGER · BEING AND TIME", fontSize = 10.sp, color = Color.White.copy(alpha = .7f))
                    }
                    IconButton(onClick = { onLesson("bt05") }) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Open section 5", tint = Color.White,
                            modifier = Modifier.size(38.dp))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RowHeadline("The library", "03 PRIMARY WORKS")
                Text("Curated interpretive study guides; the copyrighted source books are not embedded.", fontSize = 12.sp, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    catalog.works.take(2).forEach { BookTile(it, { onWork(it.id) }, Modifier.weight(1f)) }
                }
                catalog.works.drop(2).forEach { work ->
                    Surface(onClick = { onWork(work.id) }, color = Cream, shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(color = Color(work.color), shape = RoundedCornerShape(7.dp)) {
                                Icon(Icons.Default.AutoStories, null, tint = Color.White,
                                    modifier = Modifier.padding(14.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(work.title, fontFamily = FontFamily.Serif, fontSize = 19.sp)
                                Text(work.author, color = Muted, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Moss)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                RowHeadline("Your progress", "$done / ${catalog.lessons.size} COMPLETE")
                LinearProgressIndicator(progress = { if (catalog.lessons.isEmpty()) 0f else done.toFloat() / catalog.lessons.size },
                    modifier = Modifier.fillMaxWidth().height(5.dp), color = Brass, trackColor = Cream)
                Text("Each section offers ten different routes into the argument.", fontSize = 12.sp, color = Muted)
            }
        }
        item { RowHeadline("A section to begin with") }
        items(catalog.lessons.filter { it.id in listOf("id02", "mp01", "bt07") }) { lesson ->
            Panel(Modifier.fillMaxWidth().clickable { onLesson(lesson.id) }) {
                Eyebrow("${catalog.work(lesson.workId)?.author} · ${lesson.section}")
                AccentTitle(lesson.title)
                Text(lesson.synopsis, fontSize = 12.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = Brass, modifier = Modifier.size(14.dp))
                    Text("${lesson.minutes} min study", fontSize = 11.sp, color = Muted)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, null, tint = Moss)
                }
            }
        }
    }
}

@Composable
fun WorkDetailScreen(catalog: Catalog, workId: String, progress: List<LessonProgress>, onBack: () -> Unit, onLesson: (String) -> Unit) {
    val work = catalog.work(workId) ?: return
    val lessons = catalog.lessons.filter { it.workId == workId }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Library")
            }
            Spacer(Modifier.height(10.dp))
            Eyebrow("THE PHENOMENOLOGICAL LIBRARY · ${work.year}")
            Display(work.title, Modifier.padding(top = 8.dp), size = 34)
            Text(work.author, color = Muted, modifier = Modifier.padding(top = 8.dp))
        }
        item { Panel(Modifier.fillMaxWidth()) { Body(work.description); Eyebrow("${lessons.size} CURATED STUDY UNITS") } }
        item { RowHeadline("Table of contents") }
        itemsIndexed(lessons) { index, lesson ->
            val finished = progress.any { it.lessonId == lesson.id && it.completed }
            Surface(onClick = { onLesson(lesson.id) }, color = if (index % 2 == 0) Color.White.copy(alpha = .65f) else Cream,
                shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(lesson.section, color = Brass, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.width(86.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(lesson.title, fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 23.sp)
                        Text("${lesson.minutes} min · ${lesson.tags.take(2).joinToString(" / ")}", color = Muted, fontSize = 10.sp)
                    }
                    Icon(if (finished) Icons.Default.CheckCircle else Icons.Default.ArrowForward, null,
                        tint = if (finished) Moss else Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
        item { Text("Edition reminder: Consult your own authorized text. Section boundaries and terms may differ across translations.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp) }
    }
}

private val lessonTabs = listOf(
    "Synopsis", "Exposition", "Advanced", "Scaffolding", "Vocabulary",
    "Argument", "Demonstration", "Criticism", "Comparison", "Seminar"
)

@Composable
fun LessonScreen(
    lesson: Lesson, catalog: Catalog, progress: LessonProgress?,
    onBack: () -> Unit, onProgress: (Boolean, Boolean) -> Unit, onPlay: () -> Unit,
    onTerm: (String) -> Unit, onAtlas: () -> Unit, onCoach: () -> Unit,
    onSaveNote: (String, String) -> Unit
) {
    var tab by remember(lesson.id) { mutableStateOf("Exposition") }
    var writing by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Table of contents")
            }
            Eyebrow("${catalog.work(lesson.workId)?.author} · ${lesson.section}")
            Display(lesson.title, Modifier.padding(top = 8.dp), 33)
            Text("${catalog.work(lesson.workId)?.title} · ${lesson.minutes}-minute study unit", color = Muted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onPlay, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Headphones, null, Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)); Text("Listen")
                }
                OutlinedIconButton(onClick = { onProgress(progress?.completed ?: false, !(progress?.bookmarked ?: false)) }) {
                    Icon(if (progress?.bookmarked == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmark")
                }
                OutlinedIconButton(onClick = { writing = true }) { Icon(Icons.Default.EditNote, "Write a note") }
                OutlinedIconButton(onClick = onCoach) { Icon(Icons.Default.Psychology, "Study tutor") }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 3.dp)) {
                items(lessonTabs) { label -> LabelTag(label, label == tab, onClick = { tab = label }) }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("${lesson.section} / ${tab.uppercase()}")
                when (tab) {
                    "Synopsis" -> Body(lesson.synopsis)
                    "Exposition" -> Body(lesson.exposition)
                    "Advanced" -> Body(lesson.advanced)
                    "Scaffolding" -> lesson.scaffold.forEachIndexed { i, step ->
                        AccentTitle("${i + 1}. ${step.title}"); Body(step.body); if (i != lesson.scaffold.lastIndex) Rule()
                    }
                    "Vocabulary" -> {
                        Body("Tap a term to enter the cross-linked phenomenological lexicon.", color = Muted)
                        lesson.vocabulary.mapNotNull(catalog::term).forEach { term ->
                            Row(Modifier.fillMaxWidth().clickable { onTerm(term.id) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(term.title, fontWeight = FontWeight.SemiBold)
                                    Text(term.original, color = Brass, fontSize = 12.sp)
                                    Text(term.definition, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Moss)
                            }
                            Rule()
                        }
                    }
                    "Argument" -> {
                        lesson.argument.forEachIndexed { i, step ->
                            Eyebrow("${i + 1} / ${step.kind}")
                            AccentTitle(step.title)
                            Body(step.detail)
                            if (i < lesson.argument.lastIndex) { Spacer(Modifier.height(4.dp)); Rule() }
                        }
                        OutlinedButton(onClick = onAtlas, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AccountTree, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Open visual argument atlas")
                        }
                    }
                    "Demonstration" -> Body(lesson.example)
                    "Criticism" -> Body(lesson.critique)
                    "Comparison" -> Body(lesson.comparison)
                    "Seminar" -> {
                        Body(lesson.seminar)
                        Eyebrow("INTERPRETIVE DEBATE")
                        Body(lesson.debate)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelTag("Original commentary")
                LabelTag("No source quotations")
            }
        }
        item {
            Text(lesson.sourceNote, color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
        }
        item {
            OutlinedButton(onClick = { onProgress(!(progress?.completed ?: false), progress?.bookmarked ?: false) }, modifier = Modifier.fillMaxWidth()) {
                Icon(if (progress?.completed == true) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null)
                Spacer(Modifier.width(8.dp))
                Text(if (progress?.completed == true) "Completed · tap to undo" else "Mark section complete")
            }
        }
    }
    if (writing) AlertDialog(onDismissRequest = { writing = false }, title = { Text("Field note · ${lesson.section}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(noteTitle, { noteTitle = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(noteBody, { noteBody = it }, label = { Text("Observation or argument") }, minLines = 4, maxLines = 8)
            }
        }, confirmButton = {
            TextButton(enabled = noteBody.isNotBlank(), onClick = {
                onSaveNote(noteTitle.ifBlank { "Note on ${lesson.section}" }, noteBody)
                noteTitle = ""; noteBody = ""; writing = false
            }) { Text("Save note") }
        }, dismissButton = { TextButton(onClick = { writing = false }) { Text("Cancel") } })
}
