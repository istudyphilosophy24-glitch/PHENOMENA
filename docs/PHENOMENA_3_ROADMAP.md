# PHENOMENA 3.0 — The Institute
## Engineering and editorial roadmap · 2026-10-16

**Base:** PHENOMENA 2.0 alpha · branch `feature/phenomena-2-foundation`.
**Status:** Development branch; not a released APK.
**Nonnegotiable:** Preserve all 56 existing works, 355 lessons, 85 seminar modules and their 850 semantic chapters. The current 521,319-word narration corpus remains intact and is reported as *TTS-ready script length*, not uniquely authored lectures.

### 0. Immediate correctness defect — Current Study versus Long Seminar
The legacy Long Seminar selector initializes its seminar ID from `catalog.seminars.firstOrNull()` while the Current Study title refers to the selected lesson. This mixes unrelated works. Do **not** silently default to the global first seminar. PHENOMENA 3.0 removes Long Seminar from the lesson-local track selector and places long-form narration exclusively in the explicitly topic-scoped Great Seminar queue. The queue is initially populated only with modules whose `workId == lesson.workId`. A user may deliberately add modules from other works, but playback must label this as a cross-work queue. Imported texts never trigger a seminar by default.

### Phase I — Data integrity and navigation (required before APK)
1. Assert referential integrity for all lessons, works, terms, seminar modules, and chapters.
2. Wire lesson context into the seminar queue. Change lesson selection only when the user explicitly switches work/lesson.
3. Eliminate misleading Long Seminar and global-first-module defaults. Preserve the complete audio corpus.
4. Add tests proving the selected work controls the initial queue, and the other works' modules cannot enter without explicit selection.

### Phase II — Core Institute modules
**Great Commentary 3.0.** Section-by-section original interpretations with separate literal problem, inferential reconstruction, competing readings, objections and editorial provenance. Any explicit original-book quote must carry edition evidence; generated commentary may never masquerade as primary text.

**Great Seminar 3.0.** Topic-scoped authored lecture and dialogue programs, reproducible word/time accounting, stable chapter queue and MediaSession background service. Distinguish existing guided rehearsal from newly authored lectures. Persist module, segment, narration settings and explicit selections. Never claim ElevenReader as an in-app engine.

**Philological Observatory.** German/French term records, morphological notes and independent translation choices. Import-based parallel text comparison by edition and work; *no bundled copyrighted translations*. No automatic paragraph correspondence claim unless verified.

**Research Laboratory.** Guided phenomenological-description protocols and premise–inference–objection reconstruction, with on-device private notes and traceable lesson/work identifiers. Human assessment is required for claims of conceptual originality.

**Nocturne III.** Recompose navigation around Library, Listen, Commentary, Observatory and Laboratory. Respect dark-system-bar and accessibility contrast; lazy lists; clear provenance badges and discoverable back navigation.

### Phase III — Stability gate
- Android SDK 35 CI: `:app:testDebugUnitTest :app:assembleDebug`, manifest foreground-media permissions, no ANRs or startup crashes during emulator smoke run.
- Real device: Android 13–16, locked screen, incoming call/audio focus, headset skip/pause, no internet, TTS service unavailable, interrupted import, process death/relaunch, rotation/font scale.
- Data integrity: previous user notes/imports retained on update; database migration dry-run; progress cursor stable after app update.
- Content audit: deterministic SHA-256 checks of legacy assets, count parity and exact module IDs; author/citation review queue; track unique lecture prose separately from review.

### Phase IV — release gate
Publish a distinct, versioned **3.0 preview** only after CI compiles and the full catalog passes validation. A **production 3.0** must additionally pass physical-device smoke tests and documented editorial review. Reproducible APK packaging should pin JDK/Gradle/Android SDK, produce an artifact digest, and never modify old release tags.

### Traceability / acceptance
1. Selecting Heidegger §7 then opening Listen shows **Heidegger §7** in Current Study, not Husserl's first seminar.
2. Its Great Seminar queue initially contains only modules from `heidegger`.
3. No Long Seminar tab appears in the lesson-local track selector.
4. 56 works, 355 lessons, 85 modules, 850 semantic chapters and 521,319 words are preserved.
5. Imported text is always marked user-supplied with offsets, not attributed to a printed edition.
6. Every authored Commentary and Observatory entry contains a provenance/source note. All illustrative translation proposals are labeled editorial examples, not claims about licensed published translations.
