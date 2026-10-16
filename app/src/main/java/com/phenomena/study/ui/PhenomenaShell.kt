package com.phenomena.study.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.data.*
import com.phenomena.study.player.StudyPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhenomenaShell(
    catalog: Catalog, dao: StudyDao, player: StudyPlayer,
    notes: List<StudyNote>, progress: List<LessonProgress>, imports: List<ImportedDocument>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var page by remember { mutableStateOf("LIBRARY") }
    var activeWork by remember { mutableStateOf("heidegger") }
    var activeLesson by remember { mutableStateOf("bt05") }
    var activeTerm by remember { mutableStateOf("dasein") }
    var activeImport by remember { mutableStateOf<Long?>(null) }
    var coachOpen by remember { mutableStateOf(false) }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val document = withContext(Dispatchers.IO) { DocumentImporter.import(context, uri) }
                dao.addImport(ImportedDocument(title = document.title, content = document.content, format = document.format))
                snackbar.showSnackbar("Imported ${document.title} into your private library")
            } catch (e: Exception) { snackbar.showSnackbar("Import failed: ${e.message}") }
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            player.pickAudio(uri)
        }
    }
    val lesson = catalog.lesson(activeLesson) ?: catalog.lessons.first()
    val imported = imports.firstOrNull { it.id == activeImport }
    fun goLesson(id: String) {
        val l = catalog.lesson(id) ?: return
        activeLesson = l.id
        activeWork = l.workId
        activeImport = null
        page = "LESSON"
        scope.launch {
            val old = progress.firstOrNull { it.lessonId == id }
            dao.updateProgress((old ?: LessonProgress(id)).copy(lastOpened = System.currentTimeMillis()))
        }
    }
    BackHandler(enabled = page == "LESSON" || page == "DETAIL" || coachOpen) {
        if (coachOpen) coachOpen = false else page = if (page == "LESSON") "DETAIL" else "LIBRARY"
    }
    Scaffold(
        containerColor = Paper,
        topBar = {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 13.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = Moss, shape = androidx.compose.foundation.shape.RoundedCornerShape(9.dp)) {
                            Icon(Icons.Default.AutoStories, null, Modifier.padding(8.dp), tint = Color.White)
                        }
                        Column {
                            Text("PHENOMENA", color = Ink, style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black, letterSpacing = 2.sp)
                            Text("A LABORATORY OF APPEARING", fontSize = 8.5.sp, color = Muted,
                                letterSpacing = 1.sp)
                        }
                    }
                    IconButton(onClick = { page = "NOTEBOOK" }) { Icon(Icons.Default.BookmarkBorder, "Open notebook", tint = Moss) }
                }
                Rule()
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
                val actions = listOf(
                    Triple("LIBRARY", "Library", Icons.Default.AutoStories),
                    Triple("LISTEN", "Player", Icons.Default.Headphones),
                    Triple("LEXICON", "Lexicon", Icons.Default.MenuBook),
                    Triple("ATLAS", "Atlas", Icons.Default.AccountTree),
                    Triple("NOTEBOOK", "Notes", Icons.Default.EditNote)
                )
                actions.forEach { (id,label,icon) ->
                    NavigationBarItem(selected = page == id || (page in listOf("DETAIL", "LESSON") && id == "LIBRARY"),
                        onClick = { if (id == "LISTEN") activeImport = null; page = id },
                        icon = { Icon(icon, label, modifier = Modifier.size(21.dp)) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Moss, selectedTextColor = Moss, indicatorColor = Cream, unselectedIconColor = Muted))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inset ->
        Box(Modifier.fillMaxSize().padding(inset)) {
            when (page) {
                "LIBRARY" -> LibraryScreen(catalog, progress,
                    onWork = { activeWork = it; page = "DETAIL" }, onLesson = { goLesson(it) })
                "DETAIL" -> WorkDetailScreen(catalog, activeWork, progress,
                    onBack = { page = "LIBRARY" }, onLesson = { goLesson(it) })
                "LESSON" -> LessonScreen(lesson, catalog, progress.firstOrNull { it.lessonId == lesson.id },
                    onBack = { page = "DETAIL" },
                    onProgress = { complete, bookmark -> scope.launch { dao.updateProgress(LessonProgress(lesson.id, complete, bookmark)) } },
                    onPlay = { activeImport = null; page = "LISTEN" },
                    onTerm = { activeTerm = it; page = "LEXICON" },
                    onAtlas = { page = "ATLAS" },
                    onCoach = { coachOpen = true },
                    onSaveNote = { title, body -> scope.launch { dao.addNote(StudyNote(lessonId = lesson.id, heading = title, body = body)); snackbar.showSnackbar("Saved to notebook") } }
                )
                "LISTEN" -> ListenScreen(lesson, imported, player, catalog,
                    onChooseLesson = { goLesson(it); page = "LISTEN" },
                    onAudioPick = { audioPicker.launch(arrayOf("audio/*")) },
                    onShare = { text ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Open in a text reader (including ElevenReader if supported)"))
                    })
                "LEXICON" -> LexiconScreen(catalog, activeTerm, onSelect = { activeTerm = it })
                "ATLAS" -> AtlasScreen(lesson, catalog, onPickLesson = { activeLesson = it })
                "NOTEBOOK" -> NotebookScreen(notes, imports, catalog,
                    onImport = { docPicker.launch(arrayOf("application/epub+zip", "application/pdf", "text/plain", "text/html", "application/octet-stream")) },
                    onDeleteNote = { scope.launch { dao.deleteNote(it) } },
                    onDeleteImport = { scope.launch { dao.deleteImport(it) } },
                    onGoLesson = { goLesson(it) },
                    onListenImport = { activeImport = it; page = "LISTEN" }
                )
            }
            if (coachOpen) {
                AlertDialog(onDismissRequest = { coachOpen = false }, confirmButton = {},
                    title = { Text("Phenomenological Tutor") },
                    text = { TutorScreen(lesson, catalog.lexicon) },
                    dismissButton = { TextButton(onClick = { coachOpen = false }) { Text("Close") } })
            }
        }
    }
}
