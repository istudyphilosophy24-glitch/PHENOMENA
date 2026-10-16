# PHENOMENA — Extended Phenomenological Library (60–80-hour narration manuscript)

**Editorial release — no APK built or generated.**

This repository contains an offline-first, native Kotlin/Compose Android study companion and an independent browser preview. The original pedagogy covers foundational, realist, existential, social, embodied, ethical, late, and contemporary phenomenology.

## Current auditable content

- **56 works**: primary texts, lectures, and important contemporary phenomenological scholarship (study-guide entries, **not complete bundled books**).
- **355** conceptual study lessons using structured synopses, expositions, methodological scaffolding, argument steps, counterexamples and critical conversations.
- **223** searchable, cross-referenced philosophical vocabulary entries with original-language labels where available.
- **85** long-form seminar modules with **850** semantic chapters and **521,319 narratable script words**.
- Estimated duration: **75.55 h at 115 wpm**, **69.51 h at 125 wpm**, **66.84 h at 130 wpm**. The actual TTS timing is device/voice-dependent. Length includes explicitly identified conceptual rehearsal, repeated examples and teaching exercises; it is not 70 hours of nonrepetitive scholarly lectures.
- Native player includes semantic chapter skipping, automatic advancement, selectable local voices, manuscript navigation/search and optional import/share features. Audio is produced by the Android text-to-speech engine; no MP3 files are bundled.

**The original complete primary works and licensed translations are not bundled.** The interpretive content is authored study material, not a primary-text quotation, scholarly critical edition or professionally recorded lecture series.

## Documentation and verification

- `docs/READING_PATHWAYS.md`: full 56-work bibliography and 85-module word-count table, intellectual pathways and editorial disclaimers.
- `docs/SCHOLARLY_VERIFICATION.md`: bibliography verification resources, research limits and expert-review checklist.
- `tools/expand_to_70h.py`: reproducible additive content authoring; idempotent on repeated runs.
- `tools/create_reading_pathways.py`: regenerate indexes directly from packaged manuscript.
- `tools/validate_catalog.py`, `tools/validate_seminars.py`, `tools/validate_70h_catalog.py`: structural, reference and duration checks.

To validate **without** Android SDK or Gradle:

```bash
python3 tools/validate_catalog.py
python3 tools/validate_seminars.py
python3 tools/validate_70h_catalog.py
```

**APK build deliberately deferred.** The Android build workflow uses `workflow_dispatch` only. Do not run it or merge this editorial branch until you are ready for a separate compilation milestone.