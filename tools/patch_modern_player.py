#!/usr/bin/env python3
"""Modernize the complete 70-hour seminar player after restoring its source archive.

The human-readable repository PlayerScreen.kt is a compact earlier source.
Editing the expanded UI in place avoids accidentally discarding 70-hour
semantic chapters, seminar search, and the SOPHIA narrator picker.
"""
from pathlib import Path

file = Path("app/src/main/java/com/phenomena/study/ui/PlayerScreen.kt")
text = file.read_text(encoding="utf-8")
changes = [
    ('Eyebrow("AN AUDIO-FIRST PHILOSOPHICAL SEMINAR")',
     'Eyebrow("THE LISTENING ROOM")'),
    ('Display("The study player", Modifier.padding(top = 7.dp), 33)',
     'Display("Listen with intention.", Modifier.padding(top = 7.dp), 35)'),
    ('Surface(color = Moss, shape = RoundedCornerShape(18.dp))',
     'Surface(color = Color(0xFF1C4948), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, Color(0xFF3A6862)))'),
    ('RowHeadline("Narration track")',
     'RowHeadline("Choose your perspective")'),
    ('RowHeadline("Semantic chapters",',
     'RowHeadline("Chapter navigator",'),
    ('else Color.White.copy(alpha = .67f)', 'else CardSurface'),
    ('Text("This uses Android\'s sharing chooser. The target reading app must accept shared text; automatic ElevenReader playback cannot be guaranteed.",',
     'Text("External sharing is optional. Your installed Android TTS voice reads lessons directly inside PHENOMENA.",'),
]
for old, new in changes:
    if old not in text:
        raise RuntimeError("Expected PlayerScreen element missing: " + old[:80])
    text = text.replace(old, new)
file.write_text(text, encoding="utf-8")
print("Expanded player preserved and refined for nocturne design")
