#!/usr/bin/env python3
"""Transparent PHENOMENA 2.0 manuscript audit. Never conflates length with novelty.

Results are descriptive; audit does not certify correctness of philosophical claims.
No network access and no user-imported documents are inspected.
"""
from __future__ import annotations
import collections, hashlib, json, re, pathlib
ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
OUT = ROOT / 'docs/audit'
OUT.mkdir(parents=True, exist_ok=True)
workdata = json.loads((ASSETS/'library.json').read_text(encoding='utf-8'))
seminar = json.loads((ASSETS/'seminars.json').read_text(encoding='utf-8'))
lexicon = json.loads((ASSETS/'lexicon.json').read_text(encoding='utf-8'))
modules = seminar['modules']
chapters = [(m, c) for m in modules for c in m['chapters']]
words = sum(len(c['text'].split()) for _, c in chapters)
reported_words = sum(m.get('spokenWords', 0) for m in modules)
assert reported_words == words, f'Metadata mismatch {reported_words} vs {words}'
assert len({w['id'] for w in workdata['works']}) == len(workdata['works'])
assert len({m['id'] for m in modules}) == len(modules)
ids = {w['id'] for w in workdata['works']}
assert all(m['workId'] in ids for m in modules)
assert all(len(c['text'].split()) == c['wordCount'] for _, c in chapters)

# Exact normalized sentence repetition (a reproducible, conservative lower bound).
def tokens(t):
    return re.findall(r"[\w]+(?:['’\-][\w]+)*", t.lower(), flags=re.UNICODE)
sentences = []
for m, c in chapters:
    for s in re.split(r'(?<=[.!?])\s+|\n{2,}', c['text']):
        ts = tokens(s)
        if len(ts) >= 8:
            sentences.append((m['id'], c['id'], ' '.join(ts), len(ts)))
freq = collections.Counter(s[2] for s in sentences)
repeated_sents = sum(1 for s in sentences if freq[s[2]] > 1)
repeated_word_occurrences = sum(s[3] for s in sentences if freq[s[2]] > 1)
redundant_sentence_words = sum(s[3] for s in sentences if freq[s[2]] > 1) - sum(
    len(k.split()) for k,v in freq.items() if v > 1
)

# Distinctive 8-token shingle fraction (not a deduplicated "audio duration").
shingle = set()
shingles_total = 0
for _, c in chapters:
    t = tokens(c['text'])
    for i in range(max(0, len(t)-7)):
        shingle.add(hashlib.blake2b(' '.join(t[i:i+8]).encode(),digest_size=8).digest())
        shingles_total += 1

# Scholarly references have not undergone independently verified edition/page audit.
source_notes = [l.get('sourceNote','').strip() for l in workdata['lessons']]
missing_source_notes = sum(not x for x in source_notes)
# A claim of verified pagination needs edition + page and an external editorial check.
citation_records = []
for l in workdata['lessons']:
    citation_records.append({
        'lesson_id':l['id'], 'work_id':l['workId'], 'source_note':l.get('sourceNote',''),
        'primary_edition_verified':False, 'exact_page_verified':False,
        'editorial_status':'needs_source_verification'
    })
report={
  'status':'computational_only_not_peer_reviewed',
  'scope':{'works':len(workdata['works']), 'lessons':len(workdata['lessons']),
           'terms':len(lexicon['terms']), 'seminars':len(modules),'chapters':len(chapters)},
  'narration':{'script_words_total':words,'hours_at_115_wpm':round(words/115/60,2),
               'hours_at_125_wpm':round(words/125/60,2),'hours_at_130_wpm':round(words/130/60,2),
               'prerecorded_audio_hours':0,'unique_lecture_hours_verified':None},
  'repetition':{'sentences_analyzed_min_8_tokens':len(sentences),
                'sentence_occurrences_repeated':repeated_sents,
                'repeated_sentence_occurrence_words':repeated_word_occurrences,
                'redundant_repeated_sentence_words_lower_bound':redundant_sentence_words,
                'distinct_eight_token_shingles':len(shingle),
                'total_eight_token_shingles':shingles_total,
                'distinctive_shingle_ratio':round(len(shingle)/shingles_total,4) if shingles_total else 0},
  'references':{'missing_lesson_source_notes':missing_source_notes,
                'lessons_pending_edition_check':len(citation_records),
                'exact_pages_editorially_verified':0},
  'limitations':['Estimates are script words divided by WPM, not measured synthesis time.',
                 '8-token novelty is an indicator, not a measure of unique philosophical arguments.',
                 'No source citations or historical claims were verified by this mechanical audit.',
                 'Licensed user imports remain locally stored and are not included in this report.']
}
(OUT/'v2_audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(OUT/'citation_queue.json').write_text(json.dumps(citation_records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
lines=['# PHENOMENA 2.0 — Manuscript Audit (mechanical, not peer review)','',
       f"**Words in narratable scripts:** {words:,}",
       f"**Script time:** {report['narration']['hours_at_115_wpm']} h at 115 WPM; {report['narration']['hours_at_125_wpm']} h at 125 WPM; {report['narration']['hours_at_130_wpm']} h at 130 WPM.",
       '**Unique lecture hours independently certified:** not established.',
       f"**Exact repeated sentence occurrences:** {repeated_sents:,} of {len(sentences):,} eligible sentences; {redundant_sentence_words:,} extra repeated sentence words (lower bound).",
       f"**8-token shingle distinctiveness:** {report['repetition']['distinctive_shingle_ratio']:.1%} ({len(shingle):,}/{shingles_total:,}).",
       f"**Lessons awaiting scholarly source verification:** {len(citation_records)}.",
       '', 'Interpretation: lengthy narrated review loops count toward playback duration but not toward independently authored unique scholarship. There is no editorial certification of 60–80 distinct lecture hours.','',
       'See v2_audit.json for machine-readable metrics and citation_queue.json for the complete source-check queue.']
(OUT/'AUDIT_REPORT.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('\n'.join(lines[:9]))
