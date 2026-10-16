#!/usr/bin/env python3
"""V2 static wiring smoke checks: compile/instrumentation checks are separate."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/com/phenomena/study'
ui=java/'ui'
manifest=root/'app/src/main/AndroidManifest.xml'
m=ET.parse(manifest).getroot()
ns={'a':'http://schemas.android.com/apk/res/android'}
services=m.findall('.//service')
assert any(x.get('{http://schemas.android.com/apk/res/android}name')=='.service.SeminarPlaybackService' and
           x.get('{http://schemas.android.com/apk/res/android}foregroundServiceType')=='mediaPlayback' for x in services)
for permission in ['FOREGROUND_SERVICE','FOREGROUND_SERVICE_MEDIA_PLAYBACK','POST_NOTIFICATIONS']:
    assert permission in manifest.read_text(),permission
files={
    'SeminarPlaybackService.kt':java/'service/SeminarPlaybackService.kt',
    'SeminarQueuePanel.kt':ui/'SeminarQueuePanel.kt',
    'ParagraphIndex.kt':java/'reader/ParagraphIndex.kt',
    'GreatReaderScreen.kt':ui/'GreatReaderScreen.kt',
    'PhenomenaShell.kt':ui/'PhenomenaShell.kt',
    'PlayerScreen.kt':ui/'PlayerScreen.kt'
}
for n,p in files.items():
    assert p.exists(),n
shell=(ui/'PhenomenaShell.kt').read_text()
assert '"READER" -> GreatReaderScreen(' in shell
assert 'readerImportId' in shell and 'onReadImport' in shell
assert 'SeminarQueuePanel(catalog' in (ui/'PlayerScreen.kt').read_text()
assert 'onReader: () -> Unit' in (ui/'ReadingScreens.kt').read_text()
assert 'onReadImport: (Long) -> Unit' in (ui/'KnowledgeScreens.kt').read_text()
assert (root/'docs/audit/v2_audit.json').exists()
print('V2 structural checks passed: background service manifest; Reader routing; import provenance; radio queue; audit records')
