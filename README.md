# PHENOMENA · 28-hour phenomenology curriculum (source branch)

**Content-only development — no APK built or requested.** This branch holds a compressed, reproducible Android project at `source/phenomena.tar.xz.base64`. The source archive expands into the 8-work, 127-lesson, 112-term curriculum and an improved seminar player. The unpacked source currently visible in the repository may reflect the earlier 15-lesson release until the content is merged and unpacked.

**Curriculum:** 28 long-form modules, 280 semantic chapters, **218,734 authored spoken words**. At a modeled 130 words/minute the scripts total **28.04 hours** (31.70 hours at 115 wpm); real TTS timing varies. Material is original teaching commentary with explicit spiral rehearsal, not prerecorded MP3 files, verbatim copyrighted books or independently peer-reviewed lectures.

**No automatic APK build:** the workflow is manual-only in this branch. To inspect the source locally, decode the `source/phenomena.tar.xz.base64` file and unpack it with `tar -xJf`, or use the ZIP provided in the ChatGPT conversation. Run `python tools/validate_catalog.py && python tools/validate_seminars.py` on the unpacked project.


Native Android learning app for phenomenology.

The source archive and automated APK build configuration will be added shortly.
