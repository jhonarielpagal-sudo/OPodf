# GoOdf Android – Basic Organ Definition File Editor

Simple Android app for editing GrandOrgue `.organ` files.  
Focused on **Stops**, **Ranks**, **Couplers**, **Tremulants** and **MIDI mapping**.

## Features (v0.2)

- Open `.organ` files from storage
- Parse and display:
  - Organ info (name, church, manuals count)
  - **Stops** with linked Ranks + MIDI key
  - **Couplers** (source → destination, type, MIDI key)
  - **Tremulants** (type, MIDI key)
- Edit name and `StopControlMIDIKeyNumber` for Stops / Couplers / Tremulants
- Save modified `.organ` file
- Lightweight – no heavy GUI panel designer

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Min SDK 26 (Android 8.0)

## How to open in Android Studio

1. Unzip this project
2. Open Android Studio → **Open** → select the `GoOdf-Android` folder
3. Wait for Gradle sync
4. Run on emulator or real device

## Important notes

- This is a **starter / basic** editor. It does **not** fully support every GrandOrgue feature yet.
- Always work on **copies** of your `.organ` files.
- Complex sections (Panels, Enclosures, etc.) are preserved as raw data when saving.
- The parser focuses on Stops, Ranks, Couplers, Tremulants + MIDI mapping.

## Sample file

A demo `.organ` file is included in the `sample/` folder so you can test immediately.

## License

You can use and modify this freely for your own GitHub repository.

---

Made as a starting point for Android-based GrandOrgue ODF editing.
