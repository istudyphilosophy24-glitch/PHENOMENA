# PHENOMENA — Audio curriculum

**Development branch:** `content/phenomenology-24h`. **Build status:** content-only; no APK generated. Branch CI configured for manual workflow dispatch only.

## Verified inventory

- Eight philosophical books and 127 study lessons, including the original 15 section-specific guides and 112 new thematic lessons.
- 112 cross-linked technical glossary entries.
- 28 full seminar modules with 280 chapter-level semantic breaks.
- 218,734 words of teaching text, counting actual whitespace-separated words in the bundled spoken scripts.
- Modeled duration: **28.04h at 130 words/minute**, **31.70h at 115 words/minute**, or **36.46h at 100 words/minute**. Device synthesizers vary.

## Course pathways

1. **Husserl, Ideas I** — Natural Attitude; Epoché and Reduction; Noesis and Noema; Eidetic Variation.
2. **Husserl, Logical Investigations** — Directedness; Signification and Fulfillment; Categorial Intuition; Anti-Psychologism.
3. **Husserl, Internal Time-Consciousness** — Retention, Primal Impression and Protention.
4. **Heidegger, Being and Time** — Question of Being; Dasein; Being-in-the-World; Equipment; the They; Care; Death; Temporality.
5. **Merleau-Ponty, Phenomenology of Perception** — Perception; Body Schema; Lived Space; Intersubjectivity and Expression.
6. **Sartre, Being and Nothingness** — Pre-reflective Cogito; Bad Faith; the Look.
7. **Edith Stein, On the Problem of Empathy** — Empathy; Person, Community and Motivation.
8. **Levinas, Totality and Infinity** — Face and Ethical Demand; Time, Alterity, Responsibility.

## Production note

This curriculum supplies **narration-ready manuscripts**, not pre-generated audio files. It contains substantial, intentionally repeated exposition, methodological drills, conceptual comparisons and exercises. Word counts therefore measure playable study narration, **not 28 hours of conceptually nonrepetitive original scholarship**. The material should undergo editorial/philosophical review and Android compilation checks before a release.

The Android project source in `source/phenomena.tar.xz.base64` is the expanded version. After extracting it, review `docs/AUDIO_CURRICULUM.md`, `docs/COURSE_INDEX.md`, `tools/expand_curriculum.py`, and `tools/validate_seminars.py`.

**No APK build should be triggered until explicitly requested.**
