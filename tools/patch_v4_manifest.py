#!/usr/bin/env python3
"""Add the minimum Android package visibility and FileProvider for ElevenReader
user-directed sharing. No third-party APIs or voice extraction.
Execute AFTER the original 70-hour archive overlay and V2 manifest patch.
"""
from pathlib import Path
import xml.etree.ElementTree as ET
p=Path(__file__).resolve().parents[1]/'app/src/main/AndroidManifest.xml'
s=p.read_text(encoding='utf8')
provider='''        <provider android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.shared"
            android:exported="false" android:grantUriPermissions="true">
            <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/phenomena_share_paths"/>
        </provider>'''
if 'phenomena_share_paths' not in s:
    assert '</application>' in s
    s=s.replace('</application>',provider+'\n    </application>')
query='<package android:name="io.elevenlabs.readerapp"/>'
if query not in s:
    if '<queries>' in s:
        s=s.replace('<queries>','<queries>'+query,1)
    else:
        s=s.replace('<application ',f'<queries>{query}</queries>\n    <application ',1)
p.write_text(s,encoding='utf8')
ET.parse(p)
assert (p.parent/'res/xml/phenomena_share_paths.xml').exists()
print('V4 user-consented FileProvider and ElevenReader package visibility valid')
