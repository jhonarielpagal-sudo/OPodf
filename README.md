# GoOdf Android

GrandOrgue ODF (.organ) creator para sa Android. Kotlin + Jetpack Compose.

## Paano i-build sa GitHub
1. Burahin ang lahat ng lumang file sa repo.
2. I-upload ang LAMAN ng folder na ito (hindi ang zip mismo), para nasa root ang `settings.gradle.kts`
   at kasama ang `.github/workflows/build.yml` (hidden folder; kung hindi lumabas, gamitin ang GitHub Desktop/git).
3. Actions tab -> "Build APK" -> kapag tapos, i-download ang artifact `GoOdf-debug-apk`.

Walang Gradle wrapper; ang workflow mismo ang nag-i-install ng Gradle 8.7 (JDK 17).

## Gamit
- Organ tab: pangalan, manual (auto: Pedal + 2 manual), i-save bilang .organ.
- Ranks / Stops tab: gumawa o i-edit.
- Import tab: buksan ang lumang .organ, lagyan ng check ang stops/ranks, i-import.
- Ilagay ang nasave na .organ sa parehong folder ng samples dahil relative ang pipe paths.
