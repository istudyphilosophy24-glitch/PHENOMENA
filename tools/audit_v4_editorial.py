#!/usr/bin/env python3
"""Reproducible editorial accounting for V4 masterclasses.

Keep retained 70-hour TTS corpus and derived University pedagogy distinct from
new original lectures. No claims of independently verified unique 120-150h.
"""
from pathlib import Path
import re,json,collections
root=Path(__file__).resolve().parents[1]
s=(root/'app/src/main/java/com/phenomena/study/learning/MasterclassesV4.kt').read_text()
ids=re.findall(r'MasterclassLecture\("(v4-[^"]+)"',s)
sections=re.findall(r'"""(.*?)"""',s,flags=re.S)
tokens=[[x.casefold() for x in re.findall(r"\b[\wÀ-ſ-]+\b",v)] for v in sections]
all_tokens=[word for section in tokens for word in section]
ngrams=[tuple(all_tokens[i:i+8]) for i in range(len(all_tokens)-7)]
counts=collections.Counter(ngrams)
report={
    "edition":"PHENOMENA 4.0 release candidate",
    "masterclass_programmes":len(ids),
    "masterclass_sections":len(sections),
    "new_original_masterclass_words":len(all_tokens),
    "new_masterclass_audio_hours_at_125_wpm":round(len(all_tokens)/7500,3),
    "unique_eight_word_windows_ratio":round(len(counts)/max(1,len(ngrams)),3),
    "legacy_seminar_script_words_preserved":521319,
    "legacy_seminar_estimated_hours_at_125_wpm":round(521319/7500,2),
    "university_derived_transcripts":"355 existing lessons, 12 teaching stages each; not counted as independently authored hours",
    "original_institute_programmes":"4 V3 authored lecture/dialogue programs; counted separately",
    "verified_unique_hours_target_met":False,
    "copyright_notice":"Only original educational material; edition locator does not certify an exact translated passage.",
    "editorial_review_status":"preliminary machine audit only"
}
assert len(ids)==10 and len(set(ids))==10,report
assert len(sections)==len(ids)*4,report
assert min(len(x) for x in tokens)>=75,report
assert len(all_tokens)>=4300,report
assert report["unique_eight_word_windows_ratio"]>.90,report
out=root/'docs/audit/PHENOMENA_V4_EDITORIAL.json'
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(report,indent=2)+'\n')
print('Editorial integrity OK')
print(json.dumps(report,indent=2))
