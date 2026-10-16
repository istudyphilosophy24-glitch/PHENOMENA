#!/usr/bin/env python3
"""Required post-V3 + post-University manifest integration for ElevenReader.
Use standard Android FileProvider; no ElevenReader private storage access.
"""
from pathlib import Path
from xml.etree import ElementTree as ET
p=Path(__file__).resolve().parents[1]/"app/src/main/AndroidManifest.xml"
s=p.read_text()
query='''<queries><package android:name="io.elevenlabs.readerapp"/></queries>'''
# merge package visibility through existing queries block without duplicating queries
if 'io.elevenlabs.readerapp' not in s:
    s=s.replace('</queries>','  <package android:name="io.elevenlabs.readerapp"/>\n    </queries>',1)
provider='''<provider android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.exportprovider"
            android:exported="false" android:grantUriPermissions="true">
            <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/elevenreader_export_paths"/>
        </provider>'''
if 'exportprovider' not in s:
    s=s.replace('</application>',provider+'\n    </application>',1)
# Manifest placeholders substitute at build-time, but Android Studio expects
# the applicationId authority literal interpolated by Android Gradle plugin.
ET.fromstring(s)
p.write_text(s)
print("ElevenReader package discovery and permission-scoped TXT FileProvider added.")
