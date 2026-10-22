package com.example.goodfandroid.parser

import com.example.goodfandroid.model.*

/**
 * Simple parser for GrandOrgue .organ files (INI-style format).
 * Supports: Organ, Manual, Stop, Rank, Coupler, Tremulant.
 * More flexible matching for real-world .organ files.
 */
object OrganParser {

    fun parse(content: String): Organ {
        val organ = Organ()
        // Normalize line endings and remove BOM if present
        val cleaned = content
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val lines = cleaned.lines()

        var currentSection: String? = null
        val sections = mutableMapOf<String, MutableMap<String, String>>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#") || line.startsWith("//")) continue

            // Section header: [Something]
            if (line.startsWith("[") && line.contains("]")) {
                val end = line.indexOf(']')
                currentSection = line.substring(1, end).trim()
                if (!sections.containsKey(currentSection)) {
                    sections[currentSection] = mutableMapOf()
                }
                continue
            }

            // Key=Value (also accept Key = Value)
            val eqIndex = line.indexOf('=')
            if (eqIndex > 0 && currentSection != null) {
                val key = line.substring(0, eqIndex).trim()
                val value = line.substring(eqIndex + 1).trim()
                // Skip pure comments on value side
                if (key.isNotEmpty()) {
                    sections[currentSection]!![key] = value
                }
            }
        }

        // Parse [Organ] (case-insensitive key lookup helper)
        fun Map<String, String>.getCI(key: String): String? {
            return entries.find { it.key.equals(key, ignoreCase = true) }?.value
        }

        sections.entries.find { it.key.equals("Organ", ignoreCase = true) }?.value?.let { props ->
            organ.churchName = props.getCI("ChurchName") ?: ""
            organ.organName = props.getCI("OrganName") ?: props.getCI("Name") ?: ""
            organ.numberOfManuals = props.getCI("NumberOfManuals")?.toIntOrNull() ?: 0
            organ.hasPedals = (props.getCI("HasPedals") ?: "N").uppercase() in listOf("Y", "YES", "TRUE", "1")
        }

        // Helper to extract trailing number from section name
        fun extractNumber(name: String, prefix: String): Int? {
            if (!name.startsWith(prefix, ignoreCase = true)) return null
            val numPart = name.substring(prefix.length).trimStart('0').ifEmpty { "0" }
            return numPart.toIntOrNull() ?: name.substring(prefix.length).toIntOrNull()
        }

        for ((sectionName, props) in sections) {
            when {
                sectionName.startsWith("Manual", ignoreCase = true) && sectionName.length > 6 -> {
                    val num = extractNumber(sectionName, "Manual") ?: continue
                    val manual = Manual(
                        number = num,
                        name = props.getCI("Name") ?: "Manual $num",
                        numberOfLogicalKeys = props.getCI("NumberOfLogicalKeys")?.toIntOrNull() ?: 61,
                        firstAccessibleKeyMIDINoteNumber = props.getCI("FirstAccessibleKeyMIDINoteNumber")?.toIntOrNull() ?: 36,
                        numberOfAccessibleKeys = props.getCI("NumberOfAccessibleKeys")?.toIntOrNull() ?: 61,
                        midiInputNumber = props.getCI("MIDIInputNumber")?.toIntOrNull() ?: 1,
                        numberOfStops = props.getCI("NumberOfStops")?.toIntOrNull() ?: 0,
                        numberOfCouplers = props.getCI("NumberOfCouplers")?.toIntOrNull() ?: 0,
                        numberOfTremulants = props.getCI("NumberOfTremulants")?.toIntOrNull() ?: 0,
                        rawProperties = props.toMutableMap()
                    )
                    props.forEach { (k, v) ->
                        when {
                            k.startsWith("Stop", ignoreCase = true) ->
                                v.toIntOrNull()?.let { manual.stopNumbers.add(it) }
                            k.startsWith("Coupler", ignoreCase = true) ->
                                v.toIntOrNull()?.let { manual.couplerNumbers.add(it) }
                            k.startsWith("Tremulant", ignoreCase = true) ->
                                v.toIntOrNull()?.let { manual.tremulantNumbers.add(it) }
                        }
                    }
                    organ.manuals.add(manual)
                }

                sectionName.startsWith("Stop", ignoreCase = true) && sectionName.length > 4 -> {
                    val num = extractNumber(sectionName, "Stop") ?: continue
                    val stop = Stop(
                        number = num,
                        name = props.getCI("Name") ?: "Stop $num",
                        numberOfRanks = props.getCI("NumberOfRanks")?.toIntOrNull() ?: 0,
                        stopControlMIDIKeyNumber = props.getCI("StopControlMIDIKeyNumber")?.toIntOrNull(),
                        firstAccessiblePipeLogicalKeyNumber = props.getCI("FirstAccessiblePipeLogicalKeyNumber")?.toIntOrNull(),
                        numberOfAccessiblePipes = props.getCI("NumberOfAccessiblePipes")?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    )
                    props.forEach { (k, v) ->
                        if (k.startsWith("Rank", ignoreCase = true)) {
                            v.toIntOrNull()?.let { stop.rankNumbers.add(it) }
                        }
                    }
                    organ.stops.add(stop)
                }

                sectionName.startsWith("Rank", ignoreCase = true) && sectionName.length > 4 -> {
                    val num = extractNumber(sectionName, "Rank") ?: continue
                    val rank = Rank(
                        number = num,
                        name = props.getCI("Name") ?: "Rank $num",
                        firstMidiNoteNumber = props.getCI("FirstMIDINoteNumber")?.toIntOrNull()
                            ?: props.getCI("FirstMidiNoteNumber")?.toIntOrNull() ?: 36,
                        numberOfLogicalPipes = props.getCI("NumberOfLogicalPipes")?.toIntOrNull() ?: 61,
                        windchestGroup = props.getCI("WindchestGroup")?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    )
                    organ.ranks.add(rank)
                }

                sectionName.startsWith("Coupler", ignoreCase = true) && sectionName.length > 7 -> {
                    val num = extractNumber(sectionName, "Coupler") ?: continue
                    val coupler = Coupler(
                        number = num,
                        name = props.getCI("Name") ?: "Coupler $num",
                        sourceManual = props.getCI("SourceManual")?.toIntOrNull(),
                        destinationManual = props.getCI("DestinationManual")?.toIntOrNull(),
                        couplerType = props.getCI("CouplerType") ?: props.getCI("Type") ?: "",
                        unisonOff = (props.getCI("UnisonOff") ?: "N").uppercase() in listOf("Y", "YES", "TRUE", "1"),
                        destinationKeyShift = props.getCI("DestinationKeyShift")?.toIntOrNull() ?: 0,
                        stopControlMIDIKeyNumber = props.getCI("StopControlMIDIKeyNumber")?.toIntOrNull()
                            ?: props.getCI("MIDIKeyNumber")?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    )
                    organ.couplers.add(coupler)
                }

                sectionName.startsWith("Tremulant", ignoreCase = true) && sectionName.length > 9 -> {
                    val num = extractNumber(sectionName, "Tremulant") ?: continue
                    val trem = Tremulant(
                        number = num,
                        name = props.getCI("Name") ?: "Tremulant $num",
                        tremulantType = props.getCI("TremulantType") ?: props.getCI("Type") ?: "Synth",
                        period = props.getCI("Period")?.toIntOrNull(),
                        startRate = props.getCI("StartRate")?.toIntOrNull(),
                        stopRate = props.getCI("StopRate")?.toIntOrNull(),
                        ampModDepth = props.getCI("AmpModDepth")?.toIntOrNull(),
                        stopControlMIDIKeyNumber = props.getCI("StopControlMIDIKeyNumber")?.toIntOrNull()
                            ?: props.getCI("MIDIKeyNumber")?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    )
                    organ.tremulants.add(trem)
                }

                else -> {
                    if (!sectionName.equals("Organ", ignoreCase = true)) {
                        organ.otherSections[sectionName] = props.toMutableMap()
                    }
                }
            }
        }

        organ.manuals.sortBy { it.number }
        organ.stops.sortBy { it.number }
        organ.ranks.sortBy { it.number }
        organ.couplers.sortBy { it.number }
        organ.tremulants.sortBy { it.number }

        return organ
    }

    fun write(organ: Organ): String {
        val sb = StringBuilder()

        sb.appendLine("[Organ]")
        if (organ.churchName.isNotBlank()) sb.appendLine("ChurchName=${organ.churchName}")
        if (organ.organName.isNotBlank()) sb.appendLine("OrganName=${organ.organName}")
        sb.appendLine("NumberOfManuals=${organ.numberOfManuals}")
        sb.appendLine("HasPedals=${if (organ.hasPedals) "Y" else "N"}")
        sb.appendLine()

        for (m in organ.manuals) {
            sb.appendLine("[Manual${m.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${m.name}")
            sb.appendLine("NumberOfLogicalKeys=${m.numberOfLogicalKeys}")
            sb.appendLine("FirstAccessibleKeyMIDINoteNumber=${m.firstAccessibleKeyMIDINoteNumber}")
            sb.appendLine("NumberOfAccessibleKeys=${m.numberOfAccessibleKeys}")
            sb.appendLine("MIDIInputNumber=${m.midiInputNumber}")
            if (m.numberOfStops > 0) sb.appendLine("NumberOfStops=${m.numberOfStops}")
            if (m.numberOfCouplers > 0) sb.appendLine("NumberOfCouplers=${m.numberOfCouplers}")
            if (m.numberOfTremulants > 0) sb.appendLine("NumberOfTremulants=${m.numberOfTremulants}")
            m.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "NumberOfLogicalKeys", "FirstAccessibleKeyMIDINoteNumber",
                        "NumberOfAccessibleKeys", "MIDIInputNumber", "NumberOfStops",
                        "NumberOfCouplers", "NumberOfTremulants")) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        for (s in organ.stops) {
            sb.appendLine("[Stop${s.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${s.name}")
            if (s.numberOfRanks > 0) sb.appendLine("NumberOfRanks=${s.numberOfRanks}")
            s.stopControlMIDIKeyNumber?.let { sb.appendLine("StopControlMIDIKeyNumber=$it") }
            s.firstAccessiblePipeLogicalKeyNumber?.let { sb.appendLine("FirstAccessiblePipeLogicalKeyNumber=$it") }
            s.numberOfAccessiblePipes?.let { sb.appendLine("NumberOfAccessiblePipes=$it") }
            s.rankNumbers.forEachIndexed { idx, rankNum ->
                sb.appendLine("Rank${(idx + 1).toString().padStart(3, '0')}=$rankNum")
            }
            s.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "NumberOfRanks", "StopControlMIDIKeyNumber",
                        "FirstAccessiblePipeLogicalKeyNumber", "NumberOfAccessiblePipes") &&
                    !k.startsWith("Rank", ignoreCase = true)) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        for (r in organ.ranks) {
            sb.appendLine("[Rank${r.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${r.name}")
            sb.appendLine("FirstMIDINoteNumber=${r.firstMidiNoteNumber}")
            sb.appendLine("NumberOfLogicalPipes=${r.numberOfLogicalPipes}")
            r.windchestGroup?.let { sb.appendLine("WindchestGroup=$it") }
            r.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "FirstMIDINoteNumber", "FirstMidiNoteNumber",
                        "NumberOfLogicalPipes", "WindchestGroup")) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        for (c in organ.couplers) {
            sb.appendLine("[Coupler${c.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${c.name}")
            c.sourceManual?.let { sb.appendLine("SourceManual=$it") }
            c.destinationManual?.let { sb.appendLine("DestinationManual=$it") }
            if (c.couplerType.isNotBlank()) sb.appendLine("CouplerType=${c.couplerType}")
            if (c.unisonOff) sb.appendLine("UnisonOff=Y")
            if (c.destinationKeyShift != 0) sb.appendLine("DestinationKeyShift=${c.destinationKeyShift}")
            c.stopControlMIDIKeyNumber?.let { sb.appendLine("StopControlMIDIKeyNumber=$it") }
            c.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "SourceManual", "DestinationManual", "CouplerType", "Type",
                        "UnisonOff", "DestinationKeyShift", "StopControlMIDIKeyNumber", "MIDIKeyNumber")) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        for (t in organ.tremulants) {
            sb.appendLine("[Tremulant${t.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${t.name}")
            if (t.tremulantType.isNotBlank()) sb.appendLine("TremulantType=${t.tremulantType}")
            t.period?.let { sb.appendLine("Period=$it") }
            t.startRate?.let { sb.appendLine("StartRate=$it") }
            t.stopRate?.let { sb.appendLine("StopRate=$it") }
            t.ampModDepth?.let { sb.appendLine("AmpModDepth=$it") }
            t.stopControlMIDIKeyNumber?.let { sb.appendLine("StopControlMIDIKeyNumber=$it") }
            t.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "TremulantType", "Type", "Period", "StartRate",
                        "StopRate", "AmpModDepth", "StopControlMIDIKeyNumber", "MIDIKeyNumber")) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        for ((section, props) in organ.otherSections) {
            sb.appendLine("[$section]")
            props.forEach { (k, v) -> sb.appendLine("$k=$v") }
            sb.appendLine()
        }

        return sb.toString()
    }
}
