package com.example.goodfandroid.parser

import com.example.goodfandroid.model.*

object OrganParser {

    private fun Map<String, String>.getCI(key: String): String? =
        entries.find { it.key.equals(key, ignoreCase = true) }?.value

    private fun extractNumber(name: String, prefix: String): Int? {
        if (!name.startsWith(prefix, ignoreCase = true)) return null
        val rest = name.substring(prefix.length)
        return rest.trimStart('0').ifEmpty { "0" }.toIntOrNull() ?: rest.toIntOrNull()
    }

    fun parse(content: String): Organ {
        val organ = Organ()
        val cleaned = content.removePrefix("\uFEFF").replace("\r\n", "\n").replace("\r", "\n")
        val sections = mutableMapOf<String, MutableMap<String, String>>()
        var current: String? = null

        for (raw in cleaned.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue
            if (line.startsWith("[") && line.contains("]")) {
                current = line.substring(1, line.indexOf(']')).trim()
                sections.putIfAbsent(current, mutableMapOf())
                continue
            }
            val eq = line.indexOf('=')
            if (eq > 0 && current != null) {
                sections[current]!![line.substring(0, eq).trim()] = line.substring(eq + 1).trim()
            }
        }

        sections.entries.find { it.key.equals("Organ", true) }?.value?.let { p ->
            organ.churchName = p.getCI("ChurchName") ?: ""
            organ.organName = p.getCI("OrganName") ?: p.getCI("Name") ?: "Organ"
            organ.numberOfManuals = p.getCI("NumberOfManuals")?.toIntOrNull() ?: 0
            organ.hasPedals = (p.getCI("HasPedals") ?: "N").uppercase() in listOf("Y", "YES", "TRUE", "1")
            organ.amplitudeLevel = p.getCI("AmplitudeLevel")?.toDoubleOrNull() ?: 100.0
        }

        for ((sec, props) in sections) {
            when {
                sec.startsWith("Manual", true) && sec.length > 6 -> {
                    val n = extractNumber(sec, "Manual") ?: continue
                    organ.manuals.add(Manual(
                        number = n,
                        name = props.getCI("Name") ?: if (n == 0) "Pedal" else "Manual $n",
                        numberOfLogicalKeys = props.getCI("NumberOfLogicalKeys")?.toIntOrNull() ?: if (n == 0) 32 else 61,
                        firstAccessibleKeyMIDINoteNumber = props.getCI("FirstAccessibleKeyMIDINoteNumber")?.toIntOrNull() ?: 36,
                        numberOfAccessibleKeys = props.getCI("NumberOfAccessibleKeys")?.toIntOrNull() ?: if (n == 0) 32 else 61,
                        midiInputNumber = props.getCI("MIDIInputNumber")?.toIntOrNull() ?: (n + 1),
                        numberOfStops = props.getCI("NumberOfStops")?.toIntOrNull() ?: 0,
                        numberOfCouplers = props.getCI("NumberOfCouplers")?.toIntOrNull() ?: 0,
                        numberOfTremulants = props.getCI("NumberOfTremulants")?.toIntOrNull() ?: 0,
                        rawProperties = props.toMutableMap()
                    ))
                }
                sec.startsWith("WindchestGroup", true) || sec.startsWith("Windchest", true) -> {
                    val n = extractNumber(sec, "WindchestGroup")
                        ?: extractNumber(sec, "Windchest") ?: continue
                    val w = Windchest(
                        number = n,
                        name = props.getCI("Name") ?: "Windchest $n",
                        numberOfEnclosures = props.getCI("NumberOfEnclosures")?.toIntOrNull() ?: 0,
                        numberOfTremulants = props.getCI("NumberOfTremulants")?.toIntOrNull() ?: 0,
                        rawProperties = props.toMutableMap()
                    )
                    organ.windchests.add(w)
                }
                sec.startsWith("Tremulant", true) && sec.length > 9 -> {
                    val n = extractNumber(sec, "Tremulant") ?: continue
                    organ.tremulants.add(Tremulant(
                        number = n,
                        name = props.getCI("Name") ?: "Tremulant $n",
                        tremulantType = props.getCI("TremulantType") ?: props.getCI("Type") ?: "Synth",
                        period = props.getCI("Period")?.toIntOrNull() ?: 200,
                        startRate = props.getCI("StartRate")?.toIntOrNull() ?: 30,
                        stopRate = props.getCI("StopRate")?.toIntOrNull() ?: 30,
                        ampModDepth = props.getCI("AmpModDepth")?.toIntOrNull() ?: 20,
                        stopControlMIDIKeyNumber = props.getCI("StopControlMIDIKeyNumber")?.toIntOrNull(),
                        rawProperties = props.toMutableMap()
                    ))
                }
                sec.startsWith("Rank", true) && sec.length > 4 -> {
                    val n = extractNumber(sec, "Rank") ?: continue
                    organ.ranks.add(Rank(
                        number = n,
                        name = props.getCI("Name") ?: "Rank $n",
                        firstMidiNoteNumber = props.getCI("FirstMIDINoteNumber")?.toIntOrNull()
                            ?: props.getCI("FirstMidiNoteNumber")?.toIntOrNull() ?: 36,
                        numberOfLogicalPipes = props.getCI("NumberOfLogicalPipes")?.toIntOrNull() ?: 61,
                        windchestGroup = props.getCI("WindchestGroup")?.toIntOrNull() ?: 1,
                        samplePath = props.getCI("Pipe001")?.substringBeforeLast("/")?.plus("/") ?: "",
                        rawProperties = props.toMutableMap()
                    ))
                }
                else -> {
                    if (!sec.equals("Organ", true)) {
                        organ.otherSections[sec] = props.toMutableMap()
                    }
                }
            }
        }

        organ.manuals.sortBy { it.number }
        organ.windchests.sortBy { it.number }
        organ.tremulants.sortBy { it.number }
        organ.ranks.sortBy { it.number }
        organ.hasPedals = organ.manuals.any { it.number == 0 }
        organ.numberOfManuals = organ.manuals.count { it.number > 0 }
        return organ
    }

    /** Import only Ranks from another .organ content into target organ */
    fun importRanksFrom(content: String, target: Organ) {
        val source = parse(content)
        var nextNum = (target.ranks.maxOfOrNull { it.number } ?: 0) + 1
        for (r in source.ranks) {
            target.ranks.add(r.copy(number = nextNum))
            nextNum++
        }
        target.ranks.sortBy { it.number }
    }

    fun write(organ: Organ): String {
        val sb = StringBuilder()
        sb.appendLine("[Organ]")
        if (organ.churchName.isNotBlank()) sb.appendLine("ChurchName=${organ.churchName}")
        sb.appendLine("OrganName=${organ.organName}")
        sb.appendLine("NumberOfManuals=${organ.manuals.count { it.number > 0 }}")
        sb.appendLine("HasPedals=${if (organ.manuals.any { it.number == 0 }) "Y" else "N"}")
        sb.appendLine("AmplitudeLevel=${organ.amplitudeLevel}")
        sb.appendLine()

        for (m in organ.manuals.sortedBy { it.number }) {
            sb.appendLine("[Manual${m.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${m.name}")
            sb.appendLine("NumberOfLogicalKeys=${m.numberOfLogicalKeys}")
            sb.appendLine("FirstAccessibleKeyMIDINoteNumber=${m.firstAccessibleKeyMIDINoteNumber}")
            sb.appendLine("NumberOfAccessibleKeys=${m.numberOfAccessibleKeys}")
            sb.appendLine("MIDIInputNumber=${m.midiInputNumber}")
            sb.appendLine("NumberOfStops=${m.numberOfStops}")
            sb.appendLine("NumberOfCouplers=${m.numberOfCouplers}")
            sb.appendLine("NumberOfTremulants=${m.numberOfTremulants}")
            m.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "NumberOfLogicalKeys", "FirstAccessibleKeyMIDINoteNumber",
                        "NumberOfAccessibleKeys", "MIDIInputNumber", "NumberOfStops",
                        "NumberOfCouplers", "NumberOfTremulants")) sb.appendLine("$k=$v")
            }
            sb.appendLine()
        }

        for (w in organ.windchests.sortedBy { it.number }) {
            sb.appendLine("[WindchestGroup${w.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${w.name}")
            sb.appendLine("NumberOfEnclosures=${w.numberOfEnclosures}")
            sb.appendLine("NumberOfTremulants=${w.numberOfTremulants}")
            w.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "NumberOfEnclosures", "NumberOfTremulants")) sb.appendLine("$k=$v")
            }
            sb.appendLine()
        }

        for (t in organ.tremulants.sortedBy { it.number }) {
            sb.appendLine("[Tremulant${t.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${t.name}")
            sb.appendLine("TremulantType=${t.tremulantType}")
            sb.appendLine("Period=${t.period}")
            sb.appendLine("StartRate=${t.startRate}")
            sb.appendLine("StopRate=${t.stopRate}")
            sb.appendLine("AmpModDepth=${t.ampModDepth}")
            t.stopControlMIDIKeyNumber?.let { sb.appendLine("StopControlMIDIKeyNumber=$it") }
            t.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "TremulantType", "Type", "Period", "StartRate",
                        "StopRate", "AmpModDepth", "StopControlMIDIKeyNumber")) sb.appendLine("$k=$v")
            }
            sb.appendLine()
        }

        for (r in organ.ranks.sortedBy { it.number }) {
            sb.appendLine("[Rank${r.number.toString().padStart(3, '0')}]")
            sb.appendLine("Name=${r.name}")
            sb.appendLine("FirstMIDINoteNumber=${r.firstMidiNoteNumber}")
            sb.appendLine("NumberOfLogicalPipes=${r.numberOfLogicalPipes}")
            sb.appendLine("WindchestGroup=${r.windchestGroup}")
            // Preserve existing pipe paths if any
            r.rawProperties.forEach { (k, v) ->
                if (k !in listOf("Name", "FirstMIDINoteNumber", "FirstMidiNoteNumber",
                        "NumberOfLogicalPipes", "WindchestGroup")) sb.appendLine("$k=$v")
            }
            sb.appendLine()
        }

        for ((sec, props) in organ.otherSections) {
            sb.appendLine("[$sec]")
            props.forEach { (k, v) -> sb.appendLine("$k=$v") }
            sb.appendLine()
        }
        return sb.toString()
    }
}
