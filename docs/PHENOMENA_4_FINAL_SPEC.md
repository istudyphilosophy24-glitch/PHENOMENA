# PHENOMENA 4.0 — The Auditory Institute
## Final-iteration engineering specification · 2026-10-17

### Release status
This branch produces a **4.0.0-rc1 Android development/release-candidate APK**.
Automated Kotlin compilation, catalogue validation and JVM tests are necessary
but are not evidence of stable real-device playback. Do not label device-tested
or production-signed without passing the physical-device matrix.

### Core architecture
- The existing Android `SeminarPlaybackService` is reused for **Guided**, **Long Format**, **University** and **Radio**; no second dedicated narration controller is created. Packaged lecture playback continues after screen lock when a suitable installed TTS engine is available. Playback restarts from the last TTS segment rather than an exact phoneme.
- The foreground service owns audio focus and its MediaSession; the Grand Player uses its existing pause/resume/previous/next/stop actions.
- The app launches into **Grand Player**, not the text library. Eyes-free mode is enabled by default, with large controls and optional transcripts.
- Guided combines existing lesson content without switching to a different work.
- Long Format initially chooses only modules matching the selected work and permits selecting original V4 masterclasses.
- Discovery Radio shuffles programmes across 85 legacy seminar modules, 355 Guided lessons, 355 University lessons and newly authored masterclasses and debates. Thematic Radio is restricted to the selected work. Nocturnal Radio excludes shorter Guided programs. Radio speaks a program identification before each new source.
- University is an audio-first, 12-stage pedagogy across 355 lessons, with configurable Socratic thinking pauses, rather than 355 independently reviewed new scholarly monographs.
- Audio bookmarks and sleep timers are held in local preferences and available through simple large controls.
- Laboratory protocols have background audio, while research dictation calls the system's speech-recognition activity with an explicit tap.

### ElevenReader voice compatibility
ElevenReader's proprietary voices are provided **inside ElevenReader**. A normal
Android application cannot install or select them as a system TTS engine
without an actual published `TextToSpeechService`. The app scans installed
TTS services and selects ElevenReader's package as its preferred native engine
**only when that service is truly present**. Otherwise it uses the configured
Android engine and offers an **ElevenReader-first full-TXT share flow**.
Every share requires user interaction, passes via FileProvider with temporary
read permission, and keeps the proprietary voice selection inside ElevenReader.
No synthetic voice bundles, vendor SDK bypasses or extracted assets.

### Editorial expansion and accounting
- 56 preserved work records, 355 lessons, 85 legacy seminars, 850 legacy chapters,
  521319 original script words. SHA-256 checks protect source assets from change.
- 10 newly authored four-part V4 masterclasses, over 4300 new words. Their
  duration is measured and counted separately.
- 4 V3 authored seminar programs also retained.
- Guided and University programs recombine or scaffold retained scholarship;
  they **must not** be added as uniquely authored hours.
- The proposed 120–150 **unique editorially verified** lecture hours have
  **not** been achieved. Meeting that target needs human editorial authorship
  and independent review; inflate-no-hour policy overrides marketing targets.

### Physical-device acceptance — NOT YET VERIFIED
1. Launch on Android 13, 14, 15 and 16; upgrade from 3.1 without data loss.
2. 2-hour continuous speech with screen off, no crashes, stalling or overlapping TTS.
3. Bluetooth/headset pause, call audio-focus interruption, recovery, stop.
4. Missing or disabled Android TTS engine; informative fallback/no crash.
5. ElevenReader installed vs uninstalled: sharing permission, TXT full import,
   selecting proprietary voice in ElevenReader and no misleading in-app promise.
6. Voice settings persistence; notification permission denial; battery saving modes.
7. Guided and Long Format work-topic alignment; Radio announces actual program.
8. Radio thematic work restriction, Nocturnal content, deterministic saved queue.
9. Sleep timer 15/30/60/120 min; bookmark/save/restore across app relaunch.
10. University voice pauses, narrated Lab, optional speech recognition cancellation.
11. PDF/EPUB import quality and annotation preservation.
12. Accessibility font scaling and TalkBack content descriptions.

### Reproducibility
GitHub Actions pins JDK 17, Gradle 8.11.1, SDK 35; build command
`gradle :app:testDebugUnitTest :app:assembleDebug`. A SHA-256 record accompanies
the CI APK. Debug-signed sideloads can conflict with differently signed existing
installs: preserve exports, notes and source data first.

### What is not promised
- ElevenReader proprietary voices running inside PHENOMENA absent a true Android TTS provider.
- Offline availability of an external subscription voice; ElevenReader controls downloads.
- Complete device certification or editorial sign-off solely from an Actions green check.
- An entirely rewritten 355-work primary-text library.
