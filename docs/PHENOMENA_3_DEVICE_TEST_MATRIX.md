# PHENOMENA 3.0 — Release-Blocking Physical Device Test Matrix

**Current state:** NOT PERFORMED. Passing GitHub Actions means only compilation,
unit tests, structural audit and APK packaging have been validated.

| Scenario | Device protocol | Expected outcome | Result |
|---|---|---|---|
| Launch after V2 install | Upgrade to V3 debug build without clearing app data | Existing notes/imports/progress preserved | NOT TESTED |
| Topic alignment | Select Heidegger §7, open Listen and queue | Current Study stays Heidegger; queue contains only Heidegger modules by default | NOT TESTED |
| Cross-work deliberate consent | Expand queue; enable other works; add Husserl | Only user-selected cross-work material plays | NOT TESTED |
| Foreground TTS | Play current chapter then leave and return | No overlapping narration; start/stop responsive | NOT TESTED |
| Background TTS | Start queue; lock screen 15 min; use notification skip | Service remains available; queue position advances | NOT TESTED |
| Notification denied | Deny Android 13+ notifications, then try queue | Clear user feedback; no silent crash | NOT TESTED |
| Incoming call/audio focus | Initiate call or other media | Queue yields audio focus appropriately, resumable | NOT TESTED |
| Process death | Start queue, swipe app away, reopen | Stable cursor at speech-chunk boundary; no duplicate concurrent service | NOT TESTED |
| TTS engine missing | Disable speech engine; test reader and queue | Clear fallback/error, never crash | NOT TESTED |
| Offline operation | Disable network and play packaged seminar | Fully usable without network for locally installed TTS engine | NOT TESTED |
| EPUB/PDF import | Authorized EPUB; searchable PDF; image-only PDF | Respect permissions, display extracted text or clear limitation | NOT TESTED |
| Parallel philology | Import two editions; manually page through each | Neither claims automatic edition alignment | NOT TESTED |
| Research notebook | Save and reopen observation/objection | Correct note and context retained across restart | NOT TESTED |
| Large text / accessibility | Max font scale, long titles, large imports | No inaccessible buttons, clipping or navigation loops | NOT TESTED |
| Orientation / background | Rotate while playing and return | Stable chapter, queue and selection state | NOT TESTED |

## Build reproducibility
- Android toolchain: Java 17, Gradle 8.11.1, Android SDK 35 pinned by workflow.
- Preserve source SHA-256 of library.json, lexicon.json and seminars.json.
- Save release APK SHA-256 and Actions run identifier before promotion.
- Never publish V3 final over 1.3.1 or V2 prerelease tags.

## Scholarly editorial gate
- Segment program word counts into new authored lecture, preserved legacy exposition and spiral rehearsal; do not add the two totals and call them unique hours.
- Dossiers include section locators only until editions can be independently checked.
- Translation proposals are editorial examples; no unlicensed published text.
- A domain reviewer checks conceptual fidelity of Husserlian, Heideggerian and Merleau-Pontian material before final release.
- Distinguish close reading of *user-provided source* from *PHENOMENA's authored commentary*.

**Release policy:** CI-complete = technical development build, not production-ready.
The end-user must verify physical-device behavior or provide instrumented emulator
logs before the final release is represented as device-tested.
