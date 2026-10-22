package com.example.goodfandroid.parser

import com.example.goodfandroid.model.*

/**
 * Simple parser for GrandOrgue .organ files (INI-style format).
 * Supports: Organ, Manual, Stop, Rank, Coupler, Tremulant.
 * Other sections are preserved as raw key-value maps.
 */
object OrganParser {

    fun parse(content: String): Organ {
        val organ = Organ()
        val lines = content.lines()

        var currentSection: String? = null
        val sections = mutableMapOf<String, MutableMap<String, String>>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue

            // Section header: [Something]
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length - 1).trim()
                if (!sections.containsKey(currentSection)) {
                    sections[currentSection] = mutableMapOf()
                }
                continue
            }

            // Key=Value
            val eqIndex = line.indexOf('=')
            if (eqIndex > 0 && currentSection != null) {
                val key = line.substring(0, eqIndex).trim()
                val value = line.substring(eqIndex + 1).trim()
                sections[currentSection]!![key] = value
            }
        }

        // Parse [Organ]
        sections["Organ"]?.let { props ->
            organ.churchName = props["ChurchName"] ?: ""
            organ.organName = props["OrganName"] ?: props["Name"] ?: ""
            organ.numberOfManuals = props["NumberOfManuals"]?.toIntOrNull() ?: 0
            organ.hasPedals = (props["HasPedals"] ?: "N").uppercase() in listOf("Y", "YES", "TRUE", "1")
        }

        // Parse all known sections
        for ((sectionName, props) in sections) {
            when {
                sectionName.matches(Regex("Manual\\d{3}", RegexOption.IGNORE_CASE)) -> {
                    val num = sectionName.substring(6).toIntOrNull() ?: continue
                    val manual = Manual(
                        number = num,
                        name = props["Name"] ?: "Manual $num",
                        numberOfLogicalKeys = props["NumberOfLogicalKeys"]?.toIntOrNull() ?: 61,
                        firstAccessibleKeyMIDINoteNumber = props["FirstAccessibleKeyMIDINoteNumber"]?.toIntOrNull() ?: 36,
                        numberOfAccessibleKeys = props["NumberOfAccessibleKeys"]?.toIntOrNull() ?: 61,
                        midiInputNumber = props["MIDIInputNumber"]?.toIntOrNull() ?: 1,
                        numberOfStops = props["NumberOfStops"]?.toIntOrNull() ?: 0,
                        numberOfCouplers = props["NumberOfCouplers"]?.toIntOrNull() ?: 0,
                        numberOfTremulants = props["NumberOfTremulants"]?.toIntOrNull() ?: 0,
                        rawProperties = props.toMutableMap()
                    )
                    // Collect Stop / Coupler / Tremulant references
                    props.forEach { (k, v) ->
                        when {
                            k.startsWith("Stop") && k.length == 7 ->
                                v.toIntOrNull()?.let { manual.stopNumbers.add(it) }
                            k.startsWith("Coupler") && k.length == 10 ->
                                v.toIntOrNull()?.let { manual.couplerNumbers.add(it) }
                            k.startsWith("Tremulant") && k.length == 12 ->
                                v.toIntOrNull()?.let { manual.tremulantNumbers.add(it) }
                        }
                    }
                    organ.manuals.add(manual)
                }

                sectionName.matches(Regex("Stop\\d{3}", RegexOption.IGNORE_CASE)) -> {
                    val num = sectionName.substring(4).toIntOrNull() ?: continue
                    val stop = Stop(
                        number = num,
                        name = props["Name"] ?: "Stop $num",
                        numberOfRanks = props["NumberOfRanks"]?.toIntOrNull() ?: 0,
                        stopControlMIDIKeyNumber = props["StopControlMIDIKeyNumber"]?.toIntOrNull(),
                        firstAccessiblePipeLogicalKeyNumber = props["FirstAccessiblePipeLogicalKeyNumber"]?.toIntOrNull(),
                        numberOfAccessiblePipes = props["NumberOfAccessiblePipes"]?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    )
                    props.forEach { (k, v) ->
                        if (k.startsWith("Rank") && k.length == 7) {
                            v.toIntOrNull()?.let { stop.rankNumbers.add(it) }
                        }
                    }
                    organ.stops.add(stop)
                }

                sectionName.matches(Regex("Rank\\d{3}", RegexOption.IGNORE_CASE)) -> {
                    val num = sectionName.substring(4).toIntOrNull() ?: continue
                    val rank = Rank(
                        number = num,
                        name = props["Name"] ?: "Rank $num",
                        firstMidiNoteNumber = props["FirstMIDINoteNumber"]?.toIntOrNull()
                            ?: props["FirstMidiNoteNumber"]?.toIntOrNull() ?: 36,
                        numberOfLogicalPipes = props["NumberOfLogicalPipes"]?.toIntOrNull() ?: 61,
                        windchestGroup = props["WindchestGroup"]?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    )
                    organ.ranks.add(rank)
                }

                sectionName.matches(Regex("Coupler\\d{3}", RegexOption.IGNORE_CASE)) -> {
                    val num = sectionName.substring(7).toIntOrNull() ?: continue
                    val coupler = Coupler(
                        number = num,
                        name = props["Name"] ?: "Coupler $num",
                        sourceManual = props["SourceManual"]?.toIntOrNull(),
                        destinationManual = props["DestinationManual"]?.toIntOrNull(),
                        couplerType = props["CouplerType"] ?: props["Type"] ?: "",
                        unisonOff = (props["UnisonOff"] ?: "N").uppercase() in listOf("Y", "YES", "TRUE", "1"),
                        destinationKeyShift = props["DestinationKeyShift"]?.toIntOrNull() ?: 0,
                        stopControlMIDIKeyNumber = props["StopControlMIDIKeyNumber"]?.toIntOrNull()
                            ?: props["MIDIKeyNumber"]?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    )
                    organ.couplers.add(coupler)
                }

                sectionName.matches(Regex("Tremulant\\d{3}", RegexOption.IGNORE_CASE)) -> {
                    val num = sectionName.substring(9).toIntOrNull() ?: continue
                    val trem = Tremulant(
                        number = num,
                        name = props["Name"] ?: "Tremulant $num",
                        tremulantType = props["TremulantType"] ?: props["Type"] ?: "Synth",
                        period = props["Period"]?.toIntOrNull(),
                        startRate = props["StartRate"]?.toIntOrNull(),
                        stopRate = props["StopRate"]?.toIntOrNull(),
                        ampModDepth = props["AmpModDepth"]?.toIntOrNull(),
                        stopControlMIDIKeyNumber = props["StopControlMIDIKeyNumber"]?.toIntOrNull()
                            ?: props["MIDIKeyNumber"]?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    )
                    organ.tremulants.add(trem)
                }

                else -> {
                    if (sectionName != "Organ") {
                        organ.otherSections[sectionName] = props.toMutableMap()
                    }
                }
            }
        }

        // Sort for nicer display
        organ.manuals.sortBy { it.number }
        organ.stops.sortBy { it.number }
        organ.ranks.sortBy { it.number }
        organ.couplers.sortBy { it.number }
        organ.tremulants.sortBy { it.number }

        return organ
    }

    /**
     * Reconstructs a usable .organ file.
     * Preserves unknown sections.
     */
    fun write(organ: Organ): String {
        val sb = StringBuilder()

        // [Organ]
        sb.appendLine("[Organ]")
        if (organ.churchName.isNotBlank()) sb.appendLine("ChurchName=${organ.churchName}")
        if (organ.organName.isNotBlank()) sb.appendLine("OrganName=${organ.organName}")
        sb.appendLine("NumberOfManuals=${organ.numberOfManuals}")
        sb.appendLine("HasPedals=${if (organ.hasPedals) "Y" else "N"}")
        sb.appendLine()

        // Manuals
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
                if (k !in listOf(
                        "Name", "NumberOfLogicalKeys", "FirstAccessibleKeyMIDINoteNumber",
                        "NumberOfAccessibleKeys", "MIDIInputNumber", "NumberOfStops",
                        "NumberOfCouplers", "NumberOfTremulants"
                    )
                ) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        // Stops
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
                if (k !in listOf(
                        "Name", "NumberOfRanks", "StopControlMIDIKeyNumber",
                        "FirstAccessiblePipeLogicalKeyNumber", "NumberOfAccessiblePipes"
                    ) && !k.matches(Regex("Rank\\d{3}"))
                ) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        // Ranks
        for (r in organ.ranks) {
            sb.appendLine("[Rank${r.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${r.name}")
            sb.appendLine("FirstMIDINoteNumber=${r.firstMidiNoteNumber}")
            sb.appendLine("NumberOfLogicalPipes=${r.numberOfLogicalPipes}")
            r.windchestGroup?.let { sb.appendLine("WindchestGroup=$it") }
            r.rawProperties.forEach { (k, v) ->
                if (k !in listOf(
                        "Name", "FirstMIDINoteNumber", "FirstMidiNoteNumber",
                        "NumberOfLogicalPipes", "WindchestGroup"
                    )
                ) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        // Couplers
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
                if (k !in listOf(
                        "Name", "SourceManual", "DestinationManual", "CouplerType", "Type",
                        "UnisonOff", "DestinationKeyShift", "StopControlMIDIKeyNumber", "MIDIKeyNumber"
                    )
                ) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        // Tremulants
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
                if (k !in listOf(
                        "Name", "TremulantType", "Type", "Period", "StartRate",
                        "StopRate", "AmpModDepth", "StopControlMIDIKeyNumber", "MIDIKeyNumber"
                    )
                ) {
                    sb.appendLine("$k=$v")
                }
            }
            sb.appendLine()
        }

        // Other sections (preserved)
        for ((section, props) in organ.otherSections) {
            sb.appendLine("[$section]")
            props.forEach { (k, v) -> sb.appendLine("$k=$v") }
            sb.appendLine()
        }

        return sb.toString()
    }
}
