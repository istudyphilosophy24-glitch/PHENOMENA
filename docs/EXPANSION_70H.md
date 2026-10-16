# PHENOMENA 60–80-hour editorial expansion

This branch is **content development only**. No APK build was requested or started.

## What is new

- 56 phenomenological works represented by thematic study guides, spanning foundational and secondary traditions.
- 355 guided lessons with synopsis, exposition, vocabulary, scaffolding, argument reconstruction, examples, objections and comparisons.
- 223 linked vocabulary entries in German, French, English, Greek and Czech where meaningful.
- 85 original study seminars with 850 navigable chapters, containing 521,319 narratable words.

## Realistic narration duration

| Speech rate | Calculated duration |
|---|---:|
| 115 words/minute | 75.55 hours |
| 125 words/minute | 69.51 hours |
| 130 words/minute | 66.84 hours |

**These are manuscript word-count estimates**, not measured recordings. The scripts include intentional conceptual revision and method exercises; total duration does not equal 70 hours of nonrepetitive academic lecture. There are no prerecorded MP3s or bundled copyrighted original texts.

## Source distribution — no building

The entire updated native Android Studio source (assets, model, study player and validators) is in [the content archive](../source/PHENOMENA-70h-Android-Source.tar.xz.base64), stored as base64-encoded xz tar.

To unpack it **locally** from the root of this branch:

```bash
bash tools/unpack_editorial_70h.sh
```

This unpacks the source, verifies SHA-256 and runs Python content tests. It **does not use Gradle, run GitHub Actions or build an APK**.

Browse the work-by-work [reading pathways and module index](READING_PATHWAYS.md).

## Scholarly boundaries

- The 56 work entries are guides, not full electronic copies of the works.
- Newly added lessons are topic-based, not verified paragraph-by-paragraph commentaries.
- Complex translation choices and competing interpretations need expert editing.
- Clinical and psychological discussions are explanatory philosophy, not diagnosis.
- Scripts are device-narratable by Android TTS, but engine output and voice quality are unverified.
