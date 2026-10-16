# PHENOMENA 2.0 · Engineering roadmap

**Branch:** `feature/phenomena-2-foundation`. **Baseline:** PHENOMENA 1.3.1 Nocturne. **Release policy:** do not replace the published 1.3.1 APK until the Android build, device smoke tests, narration persistence tests, and study-content audit pass.

## Non-negotiable invariants
1. Do not overwrite or delete the original 56-work/355-lesson/223-term/85-seminar content assets. All original seminar chapters and narration remain accessible as the **Legacy 70-hour curriculum**.
2. Preserve the existing package ID and Room database compatibility. Import files are private, user-supplied, and never treated as redistribution licenses or exported automatically.
3. The existing 521,319-word estimate is **script playback length**, not independently certified original lecture content. Preserve hours estimates but display separate narration/repetition statistics.
4. No bundled copyrighted English translations; scholarly editions and paragraph-level source correspondence require user-authorized imports with explicit provenance.

## Phase A — 2.0 Foundation (implemented in this branch)
- Add an auditable, offline corpus verification script producing a repetition report, citation-check queue, and machine-readable duration metadata.
- Add a paragraph model with stable source offsets and clear separation between imported source text and original instructional commentary.
- Add the Great Reader's side-by-side passage/scaffold UI, paragraph navigation, notes, and reading-by-selected TTS.
- Add a native Android foreground TTS service for long seminars with a persistent module/chapter/utterance queue, interruption handling, notification controls, and recovery by replaying the last chunk.
- Add an audio queue surface available from the listening area and allow a lesson to jump into the Great Reader.
- Introduce dynamic Nocturne 2 accent/typography components without rewriting or removing the 1.3.1 design.

## Phase B — Engineering hardening (gate before public beta)
- Compile/test on Android API 26, 33 and 35; verify screen off, phone calls/audio focus, TTS-engine switch, process removal, reboot, notification permission, and imported file progress.
- Handle stale voice initialization events and lengthy passages without losing progress; persist a canonical queue key and exact chapter/chunk cursor.
- Add WorkManager/Room-based searchable paragraph offsets if imported documents become too long; avoid parsing 1.5 MB of text on each recomposition.
- Ensure foreground notification actions work with the screen locked; actual synthesis position is chunk-accurate, **not word-accurate**.
- Add accessibility semantics, adjustable text scale, large-font layouts, contrast inspection, and physical device screenshots.

## Phase C — Scholarly upgrade (content gate)
- Review citations against named editions, exact work structure, paragraphs and page ranges, marking unverified claims accordingly.
- Separate unique exposition from rehearsals, guided reflection, and repeated templates. No renewed claim of 60–80 hours of unique lecture text until verified.
- Replace repetitive script templates with sustained arguments, interpretive disagreements, objections, author-to-author comparisons and primary-edition links.
- Prioritize a fully verified *Being and Time* and *Ideas I* work companion before expanding to more unverified works.

## Phase D — Release readiness
- Use `python tools/validate_catalog.py`, `python tools/validate_seminars.py`, `python tools/validate_70h_catalog.py`, and `python tools/audit_v2.py`.
- Android instrumentation tests for the reader, service command lifecycle, and saved queue restoration; integration tests for EPUB and searchable PDF imports.
- Release signed artifacts only after a clean build, accessibility inspection, and Android device tests. Keep GitHub debug APKs labeled as experimental.

## Technical architecture

`app/src/main/assets/{library,lexicon,seminars}.json` (immutable legacy corpus) → `Catalog` → `GreatReader` / `SeminarPlaybackService`; `StudyDatabase` retains notes, progress, and imported plaintext; `SeminarPlaybackService` owns system TTS while a notification is playing and stores queue cursor in `SharedPreferences`; original `StudyPlayer` remains the foreground lesson / user audio fallback. No automatic transfer to ElevenReader or external servers.

## Definition of done for PHENOMENA 2.0

The four chosen priorities count as complete only when background playback continues after closing the Activity, notification actions resume the correct passage, imported paragraph offsets survive re-entry, the new reader reliably distinguishes licensed imports from commentary, and the repetition/citation audit is displayed in app with honest labeling. Partial implementations must be described as such.