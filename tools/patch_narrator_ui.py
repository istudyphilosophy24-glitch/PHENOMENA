#!/usr/bin/env python3
"""Apply the upgraded narrator UI to the 70-hour release archive after unpacking.

The main branch keeps an older materialized PlayerScreen.kt. The full seminar UI
is stored in the 70-hour archive. Patch only its VOICE/PACE controls, preserving
long-form seminar and semantic-chapter navigation.
"""
from pathlib import Path

path = Path("app/src/main/java/com/phenomena/study/ui/PlayerScreen.kt")
text = path.read_text(encoding="utf-8")
old = '''                Eyebrow("VOICE / PACE")
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("British", "American").forEach { LabelTag(it, selected = player.accent == it) { player.setVoice(it) } }
                }'''
new = '''                Eyebrow("VOICE / PACE")
                NarratorSettingsPanel(player)'''
assert text.count(old) == 1, "Cannot locate the old British/American-only voice selector"
text = text.replace(old, new, 1)
text = text.replace(
    'Text("Voices depend on those installed on your device; a British voice is not guaranteed.", fontSize = 11.sp, color = Muted)',
    'Text("Playback uses your selected installed Android speech engine, with no lesson export required.", fontSize = 11.sp, color = Muted)',
)
text = text.replace("Share text with ElevenReader / other readers", "Optional: share reading externally")
path.write_text(text, encoding="utf-8")
print("Installed engine and voice controls integrated into full seminar player")
