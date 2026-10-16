#!/usr/bin/env python3
"""Idempotent, assertive PHENOMENA 2.0 reader + foreground queue integration.
Apply after unpacking the 70h curriculum and overlaying Nocturne 1.3 source.
Fail CI instead of silently shipping an unconnected UI.
"""
from pathlib import Path
R=Path(__file__).resolve().parents[1]
UI=R/'app/src/main/java/com/phenomena/study/ui'
def patch(file,old,new,expected=1):
    text=file.read_text(encoding='utf-8')
    if new in text: return
    actual=text.count(old)
    if actual != expected:raise ValueError(f'{file.name}: expected {expected} instances, got {actual} of {old[:50]!r}')
    file.write_text(text.replace(old,new),encoding='utf-8')

shell=UI/'PhenomenaShell.kt'
patch(shell,'    var activeImport by remember { mutableStateOf<Long?>(null) }',
      '    var activeImport by remember { mutableStateOf<Long?>(null) }\n    var readerImportId by remember { mutableStateOf<Long?>(null) }')
patch(shell,'Triple("ATLAS", "Atlas", Icons.Default.AccountTree)',
      'Triple("READER", "Reader", Icons.Default.ChromeReaderMode)')
patch(shell,'                    onPlay = { activeImport = null; page = "LISTEN" },',
      '                    onPlay = { activeImport = null; page = "LISTEN" },\n                    onReader = { readerImportId = null; page = "READER" },')
patch(shell,'                "LEXICON" -> LexiconScreen(catalog, activeTerm, onSelect = { activeTerm = it })',
      '''                "READER" -> GreatReaderScreen(lesson, imports.firstOrNull { it.id == readerImportId },
                    player, onSaveNote = { title,body -> scope.launch {
                        dao.addNote(StudyNote(lessonId = lesson.id, heading = title, body = body))
                        snackbar.showSnackbar("Saved paragraph annotation")
                    } }, onChooseImport = { page = "NOTEBOOK" })
                "LEXICON" -> LexiconScreen(catalog, activeTerm, onSelect = { activeTerm = it })''')
patch(shell,'                    onListenImport = { activeImport = it; page = "LISTEN" }',
      '                    onListenImport = { activeImport = it; page = "LISTEN" },\n                    onReadImport = { readerImportId = it; page = "READER" }')

lesson=UI/'ReadingScreens.kt'
patch(lesson,'onTerm: (String) -> Unit, onAtlas: () -> Unit, onCoach: () -> Unit,',
      'onTerm: (String) -> Unit, onAtlas: () -> Unit, onCoach: () -> Unit, onReader: () -> Unit,')
patch(lesson,'                OutlinedIconButton(onClick = { writing = true })',
      '''                OutlinedIconButton(onClick = onReader) {
                    Icon(Icons.Default.ChromeReaderMode, "Open Great Reader")
                }
                OutlinedIconButton(onClick = { writing = true })''')

notebook=UI/'KnowledgeScreens.kt'
patch(notebook,'onGoLesson: (String) -> Unit, onListenImport: (Long) -> Unit',
      'onGoLesson: (String) -> Unit, onListenImport: (Long) -> Unit, onReadImport: (Long) -> Unit')
patch(notebook,'OutlinedButton(onClick = { expandedDocument = item.id }) { Text("Read") }',
      'OutlinedButton(onClick = { onReadImport(item.id) }) { Text("Great Reader") }')

player=UI/'PlayerScreen.kt'
text=player.read_text(encoding='utf-8')
needle='''        item {
            Surface(color = Color(0xFF1C4948), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, Color(0xFF3A6862)))'''
if needle not in text:
    needle='''        item {
            Surface(color = Moss, shape = RoundedCornerShape(18.dp))'''
patch(player,needle,'        item { SeminarQueuePanel(catalog, onStart = { player.stop() }) }\n'+needle)
print('PHENOMENA 2.0 Great Reader and Great Seminar UI integration ready')
