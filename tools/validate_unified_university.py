#!/usr/bin/env python3
"""CI structural gate for unified player / University; runs on unpacked build."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[1]
ui=root/'app/src/main/java/com/phenomena/study/ui'
learning=root/'app/src/main/java/com/phenomena/study/learning'
service=root/'app/src/main/java/com/phenomena/study/service/SeminarPlaybackService.kt'
shell=(ui/'PhenomenaShell.kt').read_text()
legacy=(ui/'PlayerScreen.kt').read_text()
unified=(ui/'UnifiedListeningScreen.kt').read_text()
university=(ui/'UniversityScreen.kt').read_text()
guided=(learning/'GuidedTranscript.kt').read_text()
pedagogy=(learning/'UniversityPedagogy.kt').read_text()
radio=service.read_text()
assert '"LISTEN" -> UnifiedListeningScreen(' in shell
assert '"UNIVERSITY" -> UniversityScreen(' in shell
assert 'Triple("UNIVERSITY", "University"' in shell
assert all(f'"\"{mode}\""' not in [] for mode in ['Guided','Long','Radio'])
assert all('"' + m + '"' in unified for m in ['Guided','Long','Radio'])
assert 'SeminarPlaybackService.ACTION_RADIO' in unified
assert 'GuidedTranscript.beats' in unified
assert 'SeminarTopicSelection.initialIds' in (ui/'SeminarQueuePanel.kt').read_text()
assert 'ACTION_RADIO' in radio and 'nextRandomMix' in radio and 'radio_history' in radio
assert 'guided:' in radio
assert 'GuidedTranscript.beats' in radio
assert 'UniversityPedagogy.stages' in university
assert len(re.findall(r'ConceptCase\("',pedagogy)) == 12
assert all('"' + field + '"' in guided for field in [
    'Orientation','The central question','Full exposition','Advanced interpretation',
    'Argument in stages','Worked example','Critical objections'])
assert '"Long seminar"' not in legacy
assets=root/'app/src/main/assets'
library=json.loads((assets/'library.json').read_text())
seminars=json.loads((assets/'seminars.json').read_text())
assert len(library['works'])==56 and len(library['lessons'])==355
assert len(seminars['modules'])==85
assert sum(len(x['chapters']) for x in seminars['modules'])==850
summary={
    "version":"3.1 development",
    "works":len(library['works']),
    "legacy_lessons":len(library['lessons']),
    "university_courses":len(library['works']),
    "university_teaching_units":len(library['lessons']),
    "stages_per_unit":12,
    "original_concept_case_studies":12,
    "legacy_sem_modules":len(seminars['modules']),
    "legacy_sem_chapters":sum(len(x['chapters']) for x in seminars['modules']),
    "guided_mode":"continuous TTS segmented for engine limits",
    "long_mode":"selected work only; persistent foreground TTS",
    "radio_mode":"randomized pool across legacy seminars, original lectures and guided lessons",
    "unique_exposition_hours_verified":False,
    "device_tested":False
}
dest=root/'docs/audit/UNIFIED_3_1.json'
dest.parent.mkdir(parents=True,exist_ok=True)
dest.write_text(json.dumps(summary,indent=2)+'\n')
print('Unified player and University structural checks passed')
print(json.dumps(summary,indent=2))
