package com.example.goodfandroid.model

/**
 * Models focused on creating/editing organ structure:
 * Manuals, Windchestgroups, Tremulants, Ranks
 */

data class Organ(
    var churchName: String = "",
    var organName: String = "New Organ",
    var organFileName: String = "NewOrgan",   // without .organ
    var organPath: String = "",
    var numberOfManuals: Int = 0,
    var hasPedals: Boolean = false,
    var amplitudeLevel: Double = 100.0,
    var manuals: MutableList<Manual> = mutableListOf(),
    var windchests: MutableList<Windchest> = mutableListOf(),
    var tremulants: MutableList<Tremulant> = mutableListOf(),
    var ranks: MutableList<Rank> = mutableListOf(),
    var otherSections: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
)

data class Manual(
    var number: Int = 0,                    // 0 = Pedal usually
    var name: String = "",
    var numberOfLogicalKeys: Int = 61,
    var firstAccessibleKeyMIDINoteNumber: Int = 36,
    var numberOfAccessibleKeys: Int = 61,
    var midiInputNumber: Int = 1,
    var numberOfStops: Int = 0,
    var numberOfCouplers: Int = 0,
    var numberOfTremulants: Int = 0,
    var stopNumbers: MutableList<Int> = mutableListOf(),
    var couplerNumbers: MutableList<Int> = mutableListOf(),
    var tremulantNumbers: MutableList<Int> = mutableListOf(),
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)

data class Windchest(
    var number: Int = 1,
    var name: String = "",
    var numberOfEnclosures: Int = 0,
    var numberOfTremulants: Int = 0,
    var enclosureNumbers: MutableList<Int> = mutableListOf(),
    var tremulantNumbers: MutableList<Int> = mutableListOf(),
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)

data class Tremulant(
    var number: Int = 1,
    var name: String = "Tremulant",
    var tremulantType: String = "Synth",    // Synth or Wave
    var period: Int = 200,
    var startRate: Int = 30,
    var stopRate: Int = 30,
    var ampModDepth: Int = 20,
    var stopControlMIDIKeyNumber: Int? = null,
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)

data class Rank(
    var number: Int = 1,
    var name: String = "",
    var firstMidiNoteNumber: Int = 36,
    var numberOfLogicalPipes: Int = 61,
    var windchestGroup: Int = 1,
    // Optional: path pattern for samples (e.g. pipes/Principal8/)
    var samplePath: String = "",
    var rawProperties: MutableMap<String, String> = mutableMapOf()
)
