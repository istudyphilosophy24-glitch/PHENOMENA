#!/usr/bin/env python3
"""Assertive, post-archive overlay for unified audio and University navigation.

Runs after patch_v3_screens. Existing source is preserved and no legacy media
catalogue is modified.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=root/'app/src/main/java/com/phenomena/study/ui'
shell=ui/'PhenomenaShell.kt'
s=shell.read_text(encoding='utf8')
def replace(old,new):
    global s
    if s.count(old)!=1:
        raise RuntimeError(f'PHENOMENA Unified UI: expected one anchor, found {s.count(old)} for {old[:90]!r}')
    s=s.replace(old,new,1)

replace('Triple("READER", "Reader", Icons.Default.ChromeReaderMode)',
        'Triple("UNIVERSITY", "University", Icons.Default.School)')
replace('                "LISTEN" -> ListenScreen(lesson, imported, player, catalog,',
        '                "LISTEN" -> UnifiedListeningScreen(lesson, imported, player, catalog,')
replace('                "INSTITUTE" -> InstituteScreen(lesson, catalog, imports, player,',
'''                "UNIVERSITY" -> UniversityScreen(catalog, lesson, player,
                    onSetCurrentStudy = { id ->
                        goLesson(id)
                        page="UNIVERSITY"
                    },
                    onOpenPlayer = { activeImport=null;page="LISTEN" },
                    onSaveNote = { heading,body -> scope.launch {
                        dao.addNote(StudyNote(lessonId=lesson.id,heading=heading,body=body))
                        snackbar.showSnackbar("Saved university notebook entry")
                    } })
                "INSTITUTE" -> InstituteScreen(lesson, catalog, imports, player,''')
replace('Text("THE PRIVATE PHENOMENOLOGICAL INSTITUTE", fontSize = 8.sp,',
        'Text("UNIVERSITY · RADIO · SEMINAR · RESEARCH", fontSize = 8.sp,')
assert '"UNIVERSITY" -> UniversityScreen(' in s
assert '"LISTEN" -> UnifiedListeningScreen(' in s
assert 'Triple("UNIVERSITY", "University"' in s
shell.write_text(s,encoding='utf8')
print('University accessible in bottom nav; Guided/Long/Radio unified in one Listening Room.')
