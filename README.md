# GoOdf Android – Basic Organ Definition File Editor

Simple Android app for editing GrandOrgue `.organ` files.  
Focused on **Stops**, **Ranks**, **Couplers**, **Tremulants** and **MIDI mapping**.

## Features (v0.2)

- Open / Save `.organ` files
- Edit Stops, Ranks, Couplers, Tremulants
- MIDI mapping (`StopControlMIDIKeyNumber`)
- Lightweight UI (no heavy panel designer)

## Download APK (GitHub Actions)

Ang APK ay automatic na na-build sa GitHub.

### Paano i-download ang APK:

1. Pumunta sa **Actions** tab ng repo
2. Click ang pinakabagong **Build APK** workflow
3. Sa baba, under **Artifacts**, i-download ang `GoOdf-Android-debug`
4. I-extract ang zip → makikita mo ang `.apk` file
5. I-install sa phone (payagan ang "Unknown sources")

### Manual trigger:

1. Pumunta sa **Actions** → **Build APK**
2. Click **Run workflow** → **Run workflow**

### Release (optional):

Kapag gumawa ka ng **Release** sa GitHub, automatic na mai-attach ang APK doon.

## Build locally (optional)

Kung may Android Studio ka:

1. Open the project
2. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
3. APK location: `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- Android 8.0+ (API 26)

## License

Free to use and modify.
