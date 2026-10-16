#!/usr/bin/env python3
"""Validate PHENOMENA's offline content schema without requiring Android SDK."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent/'app/src/main/assets'
books=json.loads((ROOT/'library.json').read_text(encoding='utf8'))
terms=json.loads((ROOT/'lexicon.json').read_text(encoding='utf8'))['terms']
works={x['id'] for x in books['works']}
terms_by_id={x['id'] for x in terms}
lessons=books['lessons']
assert len(works)==len(books['works']), 'Duplicate work id'
assert len(terms_by_id)==len(terms), 'Duplicate term id'
assert len({x['id'] for x in lessons})==len(lessons), 'Duplicate lesson id'
for l in lessons:
    assert l['workId'] in works, f"Unknown work in {l['id']}"
    for k in ['synopsis','exposition','advanced','example','critique','comparison','seminar','debate','sourceNote']:
        assert len(l[k].strip())>=50, f"Short or missing {k} in {l['id']}"
    assert len(l['argument'])>=3 and len(l['scaffold'])>=3, f"Missing reasoning structure in {l['id']}"
    assert len(l['beats'])>=len(l['argument']), f"Insufficient semantic beats in {l['id']}"
    for x in l['vocabulary']:
        assert x in terms_by_id, f"Unknown vocabulary reference {x} in {l['id']}"
for t in terms:
    for rel in t['relations']:
        assert rel in terms_by_id, f"Unknown cross-reference {rel} in {t['id']}"
print(f"OK: {len(books['works'])} works, {len(lessons)} lessons, {len(terms)} lexicon entries; all references resolve.")
