package com.example.goodfandroid.model

/**
 * Basic data models for GrandOrgue .organ files.
 * Focused on Stops, Ranks, Couplers, Tremulants and MIDI mapping.
 */

data class Organ(
    var churchName: String = "",
    var organName: String = "",
    var numberOfManuals: Int = 0,
    var hasPedals: Boolean = false,
    var manuals: MutableList<Manual> = mutableListOf(),
    var stops: MutableList<Stop> = mutableListOf(),
    var ranks: MutableList<Rank> = mutableListOf(),
    var couplers: MutableList<Coupler> = mutableListOf(),
    var tremulants: MutableList<Tremulant> = mutableListOf(),
    // Keep original sections that we don't fully parse yet
    var otherSections: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
)

data class Manual(
    var number: Int = 0,                    // 000, 001, ...
    var name: String = "",
    var numberOfLogicalKeys: Int = 61,
    var firstAccessibleKeyMIDINoteNumber: Int = 36,
    var numberOfAccessibleKeys: Int = 61,
    var midiInputNumber: Int = 1,           // MIDI channel related
    var numberOfStops: Int = 0,
    var stopNumbers: MutableList<Int> = mutableListOf(),
    var numberOfCouplers: Int = 0,
    var couplerNumbers: MutableList<Int> = mutableListOf(),
    var numberOfTremulants: Int = 0,
    var tremulantNumbers: MutableList<Int> = mutableListOf(),
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)

data class Stop(
    var number: Int = 0,                    // 001, 002, ...
    var name: String = "",
    var numberOfRanks: Int = 0,
    var rankNumbers: MutableList<Int> = mutableListOf(),
    var stopControlMIDIKeyNumber: Int? = null,  // Important for MIDI mapping
    var firstAccessiblePipeLogicalKeyNumber: Int? = null,
    var numberOfAccessiblePipes: Int? = null,
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)

data class Rank(
    var number: Int = 0,                    // 001, 002, ...
    var name: String = "",
    var firstMidiNoteNumber: Int = 36,
    var numberOfLogicalPipes: Int = 61,
    var windchestGroup: Int? = null,
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)

data class Coupler(
    var number: Int = 0,                    // 001, 002, ...
    var name: String = "",
    var sourceManual: Int? = null,          // Which manual it comes from
    var destinationManual: Int? = null,     // Which manual it goes to
    var couplerType: String = "",           // e.g. Normal, Octave, etc.
    var unisonOff: Boolean = false,
    var destinationKeyShift: Int = 0,
    var stopControlMIDIKeyNumber: Int? = null,  // MIDI mapping
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)

data class Tremulant(
    var number: Int = 0,                    // 001, 002, ...
    var name: String = "",
    var tremulantType: String = "Synth",    // Synth or Wave
    var period: Int? = null,
    var startRate: Int? = null,
    var stopRate: Int? = null,
    var ampModDepth: Int? = null,
    var stopControlMIDIKeyNumber: Int? = null,  // MIDI mapping
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)
