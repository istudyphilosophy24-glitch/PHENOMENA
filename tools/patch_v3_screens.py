#!/usr/bin/env python3
"""Assertive, idempotent V3 migration of the fully expanded 70-hour UI.

Must execute *after* patch_narrator_ui.py, patch_modern_player.py,
patch_v2_screens.py. Do not silently ship a half-patched application.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=root/'app/src/main/java/com/phenomena/study/ui'
def replace(path,old,new,number=1):
    s=path.read_text(encoding='utf8')
    if old not in s:
        if new in s:return
        raise RuntimeError(f'{path.name}: missing anchor {old[:110]!r}')
    count=s.count(old)
    if count!=number: raise RuntimeError(f'{path.name}: expected {number} anchors, got {count}')
    path.write_text(s.replace(old,new,number),encoding='utf8')

player=ui/'PlayerScreen.kt'
# The old selector initializes from the first global seminar, not the active
# lesson. In V3, only the lesson's short tracks may appear as CURRENT STUDY.
replace(player,
    '    var chooseSeminar by remember { mutableStateOf(false) }\n'
    '    var seminarSearch by remember { mutableStateOf("") }\n'
    '    var selectedSeminar by remember { mutableStateOf(catalog.seminars.firstOrNull()?.id ?: "") }\n'
    '    val seminar = catalog.seminar(selectedSeminar)\n','')
replace(player,'        "Long seminar" -> seminar?.description.orEmpty()\n','')
replace(player,'remember(lesson.id, imported?.id, track, selectedSeminar)',
    'remember(lesson.id, imported?.id, track)')
replace(player,
    '            track == "Long seminar" -> seminar?.chapters.orEmpty().flatMap { ch ->\n'
    '                speechSafeChapter(Beat(ch.title, ch.text, ch.kind))\n'
    '            }\n','')
replace(player,
    'if (track == "Long seminar") (seminar?.title ?: lesson.title) else lesson.title',
    'lesson.title')
replace(player,
    'listOf("Synopsis", "Exposition", "Advanced", "Seminar", "Debate", "Guided", "Long seminar")',
    'listOf("Synopsis", "Exposition", "Advanced", "Seminar", "Debate", "Guided")')
s=player.read_text(encoding='utf8')
start=s.index('                if (track == "Long seminar") {')
end=s.index('                OutlinedButton(onClick = { chooseLesson = true }',start)
s=s[:start]+s[end:]
player.write_text(s,encoding='utf8')
replace(player,
    '        item { SeminarQueuePanel(catalog, onStart = { player.stop() }) }',
    '        if (imported == null) item { SeminarQueuePanel(catalog, lesson, onStart = { player.stop() }) }')
replace(player,'Eyebrow("THE LISTENING ROOM")',
    'Eyebrow("THE LISTENING ROOM · NOCTURNE III")')
replace(player,'Display("Listen with intention.", Modifier.padding(top = 7.dp), 35)',
    'Display("The sound of inquiry.", Modifier.padding(top = 7.dp), 35)')

shell=ui/'PhenomenaShell.kt'
# Persist the user's actual selected lesson; never silently return to bt05 after
# process death. This also keeps the work and lesson fields internally coherent.
replace(shell,'    var activeWork by remember { mutableStateOf("heidegger") }\n'
    '    var activeLesson by remember { mutableStateOf("bt05") }',
    '''    val studyPreferences=remember { context.getSharedPreferences("phenomena_study_v3",0) }
    val initialLessonId=remember(catalog.lessons) {
        studyPreferences.getString("current_lesson_id",null)
            ?.takeIf { catalog.lesson(it)!=null }
            ?: catalog.lessons.first().id
    }
    var activeLesson by remember { mutableStateOf(initialLessonId) }
    var activeWork by remember { mutableStateOf(catalog.lesson(initialLessonId)?.workId
        ?: catalog.works.first().id) }''')
replace(shell,'        activeLesson = l.id\n        activeWork = l.workId',
    '        activeLesson = l.id\n        activeWork = l.workId\n'
    '        studyPreferences.edit().putString("current_lesson_id",l.id).apply()')
replace(shell,
    '                "ATLAS" -> AtlasScreen(lesson, catalog, onPickLesson = { activeLesson = it })',
    '''                "ATLAS" -> AtlasScreen(lesson, catalog, onPickLesson = { id ->
                    catalog.lesson(id)?.let { picked ->
                        activeLesson=picked.id
                        activeWork=picked.workId
                        studyPreferences.edit().putString("current_lesson_id",picked.id).apply()
                    }
                })''')
replace(shell,'Triple("LEXICON", "Lexicon", Icons.Default.MenuBook),',
    'Triple("INSTITUTE", "Institute", Icons.Default.School),')
replace(shell,'                "LEXICON" -> LexiconScreen(catalog, activeTerm, onSelect = { activeTerm = it })',
'''                "INSTITUTE" -> InstituteScreen(lesson, catalog, imports, player,
                    onSaveNote = { title, body -> scope.launch {
                        dao.addNote(StudyNote(lessonId=lesson.id,heading=title,body=body))
                        snackbar.showSnackbar("Saved to your private research notebook")
                    } },
                    onOpenListener = { activeImport=null; page="LISTEN" })
                "LEXICON" -> LexiconScreen(catalog, activeTerm, onSelect = { activeTerm = it })''')
replace(shell,'Text("A PHILOSOPHICAL LISTENING SPACE", fontSize = 8.sp,',
    'Text("THE PRIVATE PHENOMENOLOGICAL INSTITUTE", fontSize = 8.sp,')
replace(shell,'Text("PHENOMENA", color = Ink,',
    'Text("PHENOMENA III", color = Ink,')
lib=ui/'ModernLibraryScreen.kt'
replace(lib,'Text("VOL. 01",','Text("VOL. III",')
replace(lib,'Display("An atlas of\\nappearing.", size = 45)',
    'Display("The life of\\ninquiry.", size = 45)')
replace(lib,'Text("A quieter way to study difficult ideas.",',
    'Text("A private institute for phenomenological research.",')
# Structural release invariants.
p=player.read_text()
assert '"Long seminar"' not in p and 'selectedSeminar' not in p
assert 'SeminarQueuePanel(catalog, lesson' in p
s=shell.read_text()
assert '"INSTITUTE" -> InstituteScreen(' in s
assert 'current_lesson_id' in s
assert '"ATLAS" -> AtlasScreen(' in s and '"LEXICON" -> LexiconScreen(' in s
print('V3 UI routed: current-study topic fix, scoped seminar queue, Nocturne III, Institute.')
