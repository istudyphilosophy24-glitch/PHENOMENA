# PHENOMENA content-pack format

The fully offline curated lessons live in `app/src/main/assets/library.json`, and the concept dictionary is `lexicon.json`. They are human-reviewable data, not opaque AI responses.

## Work

`{id,author,title,shortTitle,year,color,description}` with `color` as six-digit hex RGB.

## Lesson

Every lesson has `id`, `workId`, `section`, `title`, `minutes` (self-paced study estimate, **not recorded audio duration**), `tags`, `synopsis`, `exposition`, `advanced`, `scaffold`, `vocabulary`, `argument`, `example`, `critique`, `comparison`, `seminar`, `debate`, `beats`, `sourceNote`.

`scaffold`: array of `{title, body}`. `argument`: array of `{title,detail,kind}` where kind is `premise`, `problem`, `method`, `objection`, or `conclusion`. `beats`: array of `{title,content,kind}` enabling semantic spoken navigation. `vocabulary`: array of term ids cross-linked to the lexicon.

## Lexicon term

`{id,title,original,language,definition,nuance,relations,works}`. Every relation must point to an existing term.

## Editorial quality safeguards

1. Keep the source and interpretation distinct. Never fabricate a quotation or page reference.
2. Provide at least one robust objection and comparative discussion per lesson.
3. Do not mistake an explanation of the philosopher for endorsement of a debated claim.
4. Distinguish direct terminology, translation choices, contemporary scholarship, and app-authored analytical scaffolds.
5. Get licensing clearance before bundling any primary copyrighted texts or speech recordings.
6. Run `python tools/validate_catalog.py` after changes.

This schema supports new content packs from, for example, Husserl's *Logical Investigations*, Merleau-Ponty's later work, Sartre, Stein, Levinas, and Michel Henry without changing the Android UI.
