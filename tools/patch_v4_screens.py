#!/usr/bin/env python3
"""V4 final navigation after the restored 70h source is overlaid and V3.1
patches are applied. Preserve previous lessons, notes, progress, and imports.
"""
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/java/com/phenomena/study/ui/PhenomenaShell.kt'
s=p.read_text(encoding='utf8')
def replace(before,after):
    global s
    assert s.count(before)==1,(before,s.count(before))
    s=s.replace(before,after,1)
replace('    var page by remember { mutableStateOf("LIBRARY") }',
    '    var page by remember { mutableStateOf("LISTEN") }')
replace('                "LISTEN" -> UnifiedListeningScreen(lesson, imported, player, catalog,',
    '                "LISTEN" -> AuditoryHomeV4(lesson, imported, player, catalog,')
replace('Text("PHENOMENA III", color = Ink,',
    'Text("PHENOMENA IV", color = Ink,')
replace('Text("UNIVERSITY · RADIO · SEMINAR · RESEARCH", fontSize = 8.sp,',
    'Text("THE AUDITORY INSTITUTE · FINAL EDITION", fontSize = 8.sp,')
replace('Triple("LISTEN", "Player", Icons.Default.Headphones)',
    'Triple("LISTEN", "Listen", Icons.Default.Headphones)')
p.write_text(s,encoding='utf8')
assert 'AuditoryHomeV4(' in s and 'page = "LISTEN"' not in s[:500]
print('Nocturne IV enters directly into the Grand Player with a preserved user-selected study')
