#!/usr/bin/env python3
"""Post-archive, post-V3 and post-V3.1, required V4 audio-first navigation."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
p=r/'app/src/main/java/com/phenomena/study/ui/PhenomenaShell.kt'
s=p.read_text()
def patch(old,new):
    global s
    if s.count(old)!=1: raise RuntimeError("V4 expected one source anchor: "+repr(old[:100]))
    s=s.replace(old,new,1)
patch('var page by remember { mutableStateOf("LIBRARY") }',
      'var page by remember { mutableStateOf("LISTEN") }')
patch('Triple("LIBRARY", "Library", Icons.Default.AutoStories),\n'
      '                    Triple("LISTEN", "Player", Icons.Default.Headphones),',
      'Triple("LISTEN", "Listen", Icons.Default.Headphones),\n'
      '                    Triple("LIBRARY", "Library", Icons.Default.AutoStories),')
patch('Text("UNIVERSITY · RADIO · SEMINAR · RESEARCH", fontSize = 8.sp,',
      'Text("THE AUDITORY INSTITUTE · FINAL EDITION", fontSize = 8.sp,')
patch('Text("PHENOMENA III", color = Ink,','Text("PHENOMENA", color = Ink,')
assert '"LISTEN" -> UnifiedListeningScreen(' in s
assert '"UNIVERSITY" -> UniversityScreen(' in s
assert 'var page by remember { mutableStateOf("LISTEN") }' in s
p.write_text(s)
print("V4 opens directly into the Grand Player, with Listen first in navigation")
