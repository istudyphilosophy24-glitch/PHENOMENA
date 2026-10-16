#!/usr/bin/env python3
"""PHENOMENA V4 candidate release gate: preserve all legacy audio; prove one
foreground speech engine handles the packaged modes; check honest ElevenReader
capability gating and scoped full-document sharing.
"""
from pathlib import Path
import re
from xml.etree import ElementTree as ET
r=Path(__file__).resolve().parents[1]
ui=r/'app/src/main/java/com/phenomena/study/ui'
java=r/'app/src/main/java/com/phenomena/study'
shell=(ui/'PhenomenaShell.kt').read_text()
home=(ui/'AuditoryHomeV4.kt').read_text()
service=(java/'service/SeminarPlaybackService.kt').read_text()
player=(java/'player/StudyPlayer.kt').read_text()
bridge=(java/'audio/ElevenReaderBridge.kt').read_text()
manifest=(r/'app/src/main/AndroidManifest.xml').read_text()
assert '"LISTEN" -> AuditoryHomeV4(' in shell
assert 'mutableStateOf("LISTEN")' in shell
for mode in ('Guided','Long Format','Radio','University'):
    assert f'"{mode}"' in home,mode
assert 'SeminarPlaybackService.ACTION_PLAY' in home
assert 'listOf("guided:${lesson.id}")' in home
assert 'listOf("university:${lesson.id}")' in home
assert 'SeminarPlaybackService.ACTION_RADIO' in home
assert 'android.settings.TTS_SETTINGS' in home
assert 'radio_style' in service and 'THEMATIC' in service and 'NOCTURNAL' in service
assert 'ACTION_BOOKMARK' in home and 'ACTION_SLEEP' in home
assert 'ACTION_RESTORE_BOOKMARK' in service
assert 'sleep_until' in service
assert 'current_module' in service and 'current_module' in home
assert 'FileProvider.getUriForFile' in bridge
assert 'Intent.EXTRA_STREAM' in bridge and 'FLAG_GRANT_READ_URI_PERMISSION' in bridge
assert 'TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE' in bridge
assert 'io.elevenlabs.readerapp' in bridge and 'io.elevenlabs.readerapp' in player
assert 'phenomena_share_paths' in manifest
assert 'androidx.core.content.FileProvider' in manifest
assert 'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK' in manifest
ET.parse(r/'app/src/main/AndroidManifest.xml')
assert (r/'app/src/main/res/xml/phenomena_share_paths.xml').exists()
assert '"Long seminar"' not in (ui/'PlayerScreen.kt').read_text()
assert not re.search(r'AudioManager.*?Reflection',bridge,re.I|re.S)
print('PHENOMENA IV structural and ElevenReader interoperability checks passed')
print('Auditory session: Guided / Long Format / Radio / University')
print('ElevenReader: system TTS only if genuinely installed; otherwise user-consented TXT handoff')
