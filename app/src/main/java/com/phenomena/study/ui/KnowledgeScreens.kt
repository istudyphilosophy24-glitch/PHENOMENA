package com.phenomena.study.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.model.*
import com.phenomena.study.data.*

@Composable
fun LexiconScreen(catalog: Catalog, selectedId: String, onSelect: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    val selected = catalog.term(selectedId)
    val results = catalog.lexicon.filter {
        search.isBlank() || listOf(it.title, it.original, it.definition, it.language).any { v -> v.contains(search, ignoreCase = true) }
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Eyebrow("PHILOSOPHICAL PHILOLOGY"); Display("The lexicon", Modifier.padding(top = 8.dp), 34)
            Body("Original languages · translational ambiguity · conceptual relationships", color = Muted) }
        item {
            OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search concepts or original terms") }, shape = RoundedCornerShape(13.dp))
        }
        if (selected != null) item {
            Surface(color = Moss, shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SELECTED CONCEPT", color = Color(0xFFE1C891), fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(selected.title, color = Color.White, fontFamily = FontFamily.Serif, fontSize = 31.sp)
                    Text(selected.original + " · " + selected.language, color = Color(0xFFE1C891), fontSize = 13.sp)
                    Text(selected.definition, color = Color.White, fontSize = 14.sp, lineHeight = 23.sp)
                    Text(selected.nuance, color = Color.White.copy(alpha = .8f), fontSize = 12.sp, lineHeight = 19.sp)
                    Text("CONCEPTUAL RELATIONS", color = Color(0xFFE1C891), fontSize = 10.sp, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        selected.relations.mapNotNull(catalog::term).forEach { other ->
                            Surface(onClick = { onSelect(other.id) }, color = Color.White.copy(alpha = .14f),
                                shape = RoundedCornerShape(30.dp)) {
                                Text(other.title, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(9.dp))
                            }
                        }
                    }
                }
            }
        }
        item { RowHeadline("Conceptual index", "${results.size} ENTRIES") }
        items(results) { term ->
            Surface(onClick = { onSelect(term.id) }, color = if (selectedId == term.id) Cream else CardSurface,
                shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(term.title, fontFamily = FontFamily.Serif, fontSize = 19.sp)
                        Text(term.original, color = Brass, fontSize = 12.sp)
                        Text(term.definition, color = Muted, fontSize = 11.sp, maxLines = 2, lineHeight = 16.sp)
                    }
                    Icon(Icons.Default.NorthEast, null, tint = Moss, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
fun AtlasScreen(lesson: Lesson, catalog: Catalog, onPickLesson: (String) -> Unit) {
    var activeIndex by remember(lesson.id) { mutableIntStateOf(0) }
    var chooseLesson by remember { mutableStateOf(false) }
    val steps = lesson.argument
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Eyebrow("CONCEPTUAL TOPOGRAPHY")
            Display("The argument atlas", Modifier.padding(top = 8.dp), 32)
            Body("Trace premises, problems and conclusions. Tap a node to inspect its function.", color = Muted)
        }
        item {
            OutlinedButton(onClick = { chooseLesson = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AccountTree, null, Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("${lesson.section} · ${lesson.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, null)
            }
            DropdownMenu(chooseLesson, onDismissRequest = { chooseLesson = false }) {
                catalog.lessons.forEach { l ->
                    DropdownMenuItem(text = { Text("${l.section} · ${l.title}", maxLines = 1) },
                        onClick = { chooseLesson = false; onPickLesson(l.id) })
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("LOGICAL / INTERPRETIVE RECONSTRUCTION")
                val nodeHeight = 86.dp
                val totalHeight = 24.dp + nodeHeight * steps.size
                Canvas(Modifier.fillMaxWidth().height(totalHeight).pointerInput(lesson.id) {
                    detectTapGestures { tap ->
                        val i = ((tap.y - 19.dp.toPx()) / nodeHeight.toPx()).toInt()
                        if (i in steps.indices) activeIndex = i
                    }
                }) {
                    val nH = nodeHeight.toPx()
                    val marginY = 19.dp.toPx()
                    val left = 7.dp.toPx()
                    val right = size.width - 7.dp.toPx()
                    val cy = 49.dp.toPx()
                    steps.forEachIndexed { i, node ->
                        val y = marginY + i * nH
                        val isSelected = i == activeIndex
                        if (i < steps.lastIndex) {
                            drawLine(Brass.copy(alpha = .8f), Offset(size.width/2, y + cy),
                                Offset(size.width/2, y + nH + 19.dp.toPx()), strokeWidth = 2.dp.toPx())
                        }
                        drawRoundRect(if (isSelected) Moss else Cream, topLeft = Offset(left, y),
                            size = androidx.compose.ui.geometry.Size(right - left, 63.dp.toPx()), cornerRadius = CornerRadius(12.dp.toPx()))
                        drawCircle(if (isSelected) Brass else Moss, radius = 5.dp.toPx(),
                            center = Offset(left + 21.dp.toPx(), y + 31.dp.toPx()))
                        drawIntoCanvas {
                            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                            p.color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.rgb(239, 244, 241)
                            p.textSize = 13.sp.toPx(); p.isFakeBoldText = true
                            it.nativeCanvas.drawText(node.title.take(30), left + 35.dp.toPx(), y + 28.dp.toPx(), p)
                            p.textSize = 10.sp.toPx(); p.isFakeBoldText = false
                            p.color = if (isSelected) android.graphics.Color.LTGRAY else android.graphics.Color.LTGRAY
                            it.nativeCanvas.drawText(node.kind.uppercase(), left + 35.dp.toPx(), y + 46.dp.toPx(), p)
                        }
                    }
                }
                if (steps.isNotEmpty()) {
                    Eyebrow("NODE ${activeIndex + 1} / ${steps.size} · ${steps[activeIndex].kind}")
                    AccentTitle(steps[activeIndex].title)
                    Body(steps[activeIndex].detail)
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("THE PHENOMENOLOGICAL TEST")
                AccentTitle("Return to experience")
                Body(lesson.example)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("POSSIBLE OBJECTION")
                Body(lesson.critique)
            }
        }
    }
}

@Composable
fun NotebookScreen(
    notes: List<StudyNote>, imports: List<ImportedDocument>, catalog: Catalog,
    onImport: () -> Unit, onDeleteNote: (Long) -> Unit, onDeleteImport: (Long) -> Unit,
    onGoLesson: (String) -> Unit, onListenImport: (Long) -> Unit
) {
    var filter by remember { mutableStateOf("Notes") }
    var expandedDocument by remember { mutableStateOf<Long?>(null) }
    val activeDocument = imports.firstOrNull { it.id == expandedDocument }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Eyebrow("FIELD RECORDS / PERSONAL ARCHIVE")
            Display("The notebook", Modifier.padding(top = 8.dp), 33)
            Body("Your annotations and user-authorized documents stay on this device.", color = Muted)
        }
        item {
            PrimaryAction("Import EPUB, PDF or text", Modifier.fillMaxWidth(), onClick = onImport)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Notes", "Imported books").forEach { label -> LabelTag(label, label == filter) { filter = label } }
            }
        }
        if (filter == "Notes") {
            if (notes.isEmpty()) item { EmptyNotice("No field notes yet. Open a section and tap the writing icon to make an interpretation, observation or objection.") }
            items(notes, key = { "note-${it.id}" }) { note ->
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow(catalog.lesson(note.lessonId)?.let { "${it.section} · ${it.title}" } ?: "PERSONAL NOTE")
                            Text(note.heading, fontFamily = FontFamily.Serif, fontSize = 22.sp)
                        }
                        IconButton(onClick = { onDeleteNote(note.id) }) { Icon(Icons.Default.DeleteOutline, "Delete note", tint = Muted) }
                    }
                    Body(note.body)
                    if (catalog.lesson(note.lessonId) != null) TextButton(onClick = { onGoLesson(note.lessonId) }) { Text("Back to study section →") }
                }
            }
        } else {
            if (imports.isEmpty()) item { EmptyNotice("Import a book you have permission to use. EPUB and text files can be read aloud. Searchable PDFs are supported; scanned pages need OCR first.") }
            items(imports, key = { "book-${it.id}" }) { item ->
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, null, tint = Brass)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            AccentTitle(item.title)
                            Text("${item.format.uppercase()} · ${item.content.length} characters", color = Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onDeleteImport(item.id) }) { Icon(Icons.Default.DeleteOutline, "Delete imported file", tint = Muted) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { expandedDocument = item.id }) { Text("Read") }
                        Button(onClick = { onListenImport(item.id) }) {
                            Icon(Icons.Default.Headphones, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Listen")
                        }
                    }
                }
            }
        }
        item { Text("Imported text is stored privately in a local Room database. Only text you explicitly submit to an online tutor is sent to the configured service.", fontSize = 11.sp, lineHeight = 17.sp, color = Muted) }
    }
    if (activeDocument != null) AlertDialog(
        onDismissRequest = { expandedDocument = null },
        title = { Text(activeDocument.title, fontFamily = FontFamily.Serif) },
        text = { Box(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            Text(activeDocument.content, fontSize = 13.sp, lineHeight = 22.sp)
        } },
        confirmButton = { TextButton(onClick = { onListenImport(activeDocument.id); expandedDocument = null }) { Text("Listen") } },
        dismissButton = { TextButton(onClick = { expandedDocument = null }) { Text("Close") } }
    )
}
