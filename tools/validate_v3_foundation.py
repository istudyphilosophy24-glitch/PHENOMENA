#!/usr/bin/env python3
"""V3 release gate: preserve legacy archive byte-for-byte; reject misleading UI,
missing reader or Institute wiring, and untraceable new editorial material.
Run AFTER unpacking the archived 70-hour source and applying V2/V3 patches.
"""
import hashlib,json,re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
expected={
 'library.json':'fac89c6db14254d512ed565befe468f3b5ebc9bc5004db571130b55f86bb763b',
 'lexicon.json':'1bff8c0b3153d3082de9bb9be7b5236416b726f4c93d4ab03f62536a2403ebf7',
 'seminars.json':'48c4562ec339087058e540f531792d8c833020d38db41767c637ce7e82a5f600',
}
for name,sha in expected.items():
    actual=hashlib.sha256((assets/name).read_bytes()).hexdigest()
    assert actual==sha, f'{name}: legacy curriculum has changed: {actual}'
library=json.loads((assets/'library.json').read_text())
seminars=json.loads((assets/'seminars.json').read_text())['modules']
assert len(library['works'])==56
assert len(library['lessons'])==355
assert len(seminars)==85
assert sum(len(s['chapters']) for s in seminars)==850
assert sum(s['spokenWords'] for s in seminars)==521319
work_ids={w['id'] for w in library['works']}
lesson_ids={x['id'] for x in library['lessons']}
module_ids={s['id'] for s in seminars}
assert len(work_ids)==56 and len(lesson_ids)==355 and len(module_ids)==85
assert all(l['workId'] in work_ids for l in library['lessons'])
assert all(s['workId'] in work_ids for s in seminars)
assert all(s['chapters'] for s in seminars)
src=root/'app/src/main/java/com/phenomena/study'
player=(src/'ui/PlayerScreen.kt').read_text()
queue=(src/'ui/SeminarQueuePanel.kt').read_text()
shell=(src/'ui/PhenomenaShell.kt').read_text()
service=(src/'service/SeminarPlaybackService.kt').read_text()
materials=(src/'data/InstituteMaterials.kt').read_text()
assert '"Long seminar"' not in player, 'The local Current Study selector must not offer unrelated long form'
assert 'selectedSeminar' not in player
assert 'SeminarQueuePanel(catalog, lesson' in player
assert 'SeminarTopicSelection.initialIds(lesson?.workId' in queue
assert 'includeOtherWorks' in queue, 'cross-work opt-in required'
assert 'InstituteMaterials.lectures' in service and 'InstituteMaterials.lectures' in queue
assert '"INSTITUTE" -> InstituteScreen(' in shell
assert 'current_lesson_id' in shell and 'studyPreferences.edit()' in shell
assert 'mutableStateOf("bt05")' not in shell
assert '"READER" -> GreatReaderScreen(' in shell
assert '"ATLAS" -> AtlasScreen(' in shell
assert '"LEXICON" -> LexiconScreen(' in shell
assert all((src/'ui'/p).exists() for p in ['InstituteScreen.kt','GreatReaderScreen.kt'])
ids=re.findall(r'InstituteLecture\("(v3-[^"]+)"\s*,\s*"([^"]+)"',materials)
assert len(ids)>=4 and len(ids)==len(set(i for i,_ in ids))
assert all(w in work_ids for _,w in ids)
dossiers=re.findall(r'CommentaryDossier\("([^"]+)"',materials)
assert all(l in lesson_ids for l in dossiers)
report={
 "version":"3.0-development",
 "curriculum_immutable":True,
 "legacy_sha256":expected,
 "works":len(work_ids),"lessons":len(lesson_ids),"seminar_modules":len(module_ids),
 "semantic_chapters":sum(len(s['chapters']) for s in seminars),
 "script_words":sum(s['spokenWords'] for s in seminars),
 "estimated_audio_hours_at_125wpm":round(sum(s['spokenWords'] for s in seminars)/7500,2),
 "new_authored_programs":len(ids),
 "source_notes":"Estimates include repetition; do not report as unique lecture hours.",
 "physical_device_tested":False,
 "approved_for_production_release":False
}
out=root/'docs/audit/PHENOMENA_3_INVARIANTS.json'
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(report,indent=2)+'\n')
print('PHENOMENA 3 content hashes and integration invariants PASSED')
print(json.dumps(report,indent=2))
