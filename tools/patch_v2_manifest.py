#!/usr/bin/env python3
from pathlib import Path
from xml.etree import ElementTree as ET
p=Path(__file__).resolve().parents[1]/'app/src/main/AndroidManifest.xml'
s=p.read_text(encoding='utf-8')
permissions=['android.permission.FOREGROUND_SERVICE','android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK','android.permission.POST_NOTIFICATIONS']
for permission in permissions:
    entry=f'<uses-permission android:name="{permission}"/>'
    if entry not in s:
        s=s.replace('<application ',f'    {entry}\n    <application ',1)
service='<service android:name=".service.SeminarPlaybackService" android:exported="false" android:foregroundServiceType="mediaPlayback"/>'
if service not in s:
    s=s.replace('</application>',f'        {service}\n    </application>',1)
if 'android.intent.action.TTS_SERVICE' not in s:
    s=s.replace('<application ','''    <queries><intent><action android:name="android.intent.action.TTS_SERVICE"/></intent></queries>
    <application ''',1)
p.write_text(s,encoding='utf8')
ET.parse(p)
print('Media foreground permissions and service declaration valid')
