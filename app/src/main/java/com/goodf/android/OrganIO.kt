package com.goodf.android

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

class IniFile(val sections: LinkedHashMap<String, LinkedHashMap<String, String>>)

object Ini {
    /** UTF-8 kung valid, kung hindi ISO-8859-1 (karaniwan sa lumang .organ files). */
    fun decode(bytes: ByteArray): String {
        var b = bytes
        if (b.size >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte()) {
            b = b.copyOfRange(3, b.size)
        }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(b)).toString()
        } catch (e: CharacterCodingException) {
            String(b, Charsets.ISO_8859_1)
        }
    }

    /** ISO-8859-1 kung kasya lahat ng karakter, kung hindi UTF-8. */
    fun encode(text: String): ByteArray {
        val latin = Charsets.ISO_8859_1.newEncoder()
        return if (latin.canEncode(text)) text.toByteArray(Charsets.ISO_8859_1)
        else text.toByteArray(Charsets.UTF_8)
    }

    fun parse(text: String): IniFile {
        val result = LinkedHashMap<String, LinkedHashMap<String, String>>()
        var cur: LinkedHashMap<String, String>? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                val name = line.substring(1, line.length - 1).trim()
                cur = result.getOrPut(name) { LinkedHashMap() }
            } else {
                val i = line.indexOf('=')
                val target = cur
                if (i > 0 && target != null) {
                    target[line.substring(0, i).trim()] = line.substring(i + 1).trim()
                }
            }
        }
        return IniFile(result)
    }
}

class SrcManual(
    val number: Int,
    val name: String,
    val isPedal: Boolean,
    val stops: List<StopItem>,
    val unassigned: Boolean = false
)

class SourceOrgan(
    val churchName: String,
    val builder: String,
    val buildDate: String,
    val manuals: List<SrcManual>,
    val ranks: List<RankItem>
)

object OrganReader {
    private val rankSec = Regex("^Rank(\\d+)$")
    private val manualSec = Regex("^Manual(\\d+)$")
    private val stopSec = Regex("^Stop(\\d+)$")
    private val rankKey = Regex("^Rank\\d+.*$")

    private fun buildStop(props: Map<String, String>, rankByNo: Map<Int, RankItem>): StopItem {
        val name = props["Name"] ?: "Stop"
        val nRanks = props["NumberOfRanks"]?.trim()?.toIntOrNull() ?: 0
        val links = ArrayList<RankLink>()
        for (j in 1..nRanks) {
            val key = "Rank" + p3(j)
            val rn = props[key]?.trim()?.toIntOrNull() ?: continue
            val r = rankByNo[rn] ?: continue
            val opts = LinkedHashMap<String, String>()
            for ((k, v) in props) {
                if (k.startsWith(key) && k.length > key.length) {
                    opts[k.substring(key.length)] = v
                }
            }
            links.add(RankLink(r.id, opts))
        }
        val extras = LinkedHashMap<String, String>()
        for ((k, v) in props) {
            if (k == "Name" || k == "NumberOfRanks" || rankKey.matches(k)) continue
            extras[k] = v
        }
        return StopItem(Ids.next(), name, links, extras)
    }

    fun read(text: String): SourceOrgan? {
        val ini = Ini.parse(text)
        val org: Map<String, String> = ini.sections["Organ"] ?: emptyMap()
        val hasPedals = (org["HasPedals"] ?: "N").trim().uppercase().let {
            it == "Y" || it == "YES" || it == "TRUE" || it == "1"
        }

        // Ranks
        val rankByNo = HashMap<Int, RankItem>()
        val rankList = ArrayList<Pair<Int, RankItem>>()
        for ((secName, props) in ini.sections) {
            val m = rankSec.matchEntire(secName) ?: continue
            val no = m.groupValues[1].toInt()
            val r = RankItem(Ids.next(), props["Name"] ?: "Rank $no", props.filterKeys { it != "Name" })
            rankByNo[no] = r
            rankList.add(Pair(no, r))
        }
        rankList.sortBy { it.first }

        // Manuals at stops
        val manualEntries = ArrayList<Pair<Int, Map<String, String>>>()
        for ((secName, props) in ini.sections) {
            val m = manualSec.matchEntire(secName) ?: continue
            manualEntries.add(Pair(m.groupValues[1].toInt(), props))
        }
        manualEntries.sortBy { it.first }

        val usedStops = HashSet<Int>()
        val manuals = ArrayList<SrcManual>()
        for ((num, props) in manualEntries) {
            val n = props["NumberOfStops"]?.trim()?.toIntOrNull() ?: 0
            val stops = ArrayList<StopItem>()
            for (j in 1..n) {
                val sn = props["Stop" + p3(j)]?.trim()?.toIntOrNull() ?: continue
                val sp = ini.sections["Stop" + p3(sn)] ?: continue
                stops.add(buildStop(sp, rankByNo))
                usedStops.add(sn)
            }
            manuals.add(SrcManual(num, props["Name"] ?: "Manual $num", hasPedals && num == 0, stops))
        }

        // Mga stop na hindi naka-assign sa manual
        val orphans = ArrayList<StopItem>()
        for ((secName, props) in ini.sections) {
            val m = stopSec.matchEntire(secName) ?: continue
            val no = m.groupValues[1].toInt()
            if (no !in usedStops) orphans.add(buildStop(props, rankByNo))
        }
        if (orphans.isNotEmpty()) {
            manuals.add(SrcManual(1, "(walang manual)", false, orphans, unassigned = true))
        }

        val ranks = rankList.map { it.second }
        if (ranks.isEmpty() && manuals.all { it.stops.isEmpty() }) return null

        return SourceOrgan(
            churchName = org["ChurchName"] ?: "",
            builder = org["OrganBuilder"] ?: "",
            buildDate = org["OrganBuildDate"] ?: "",
            manuals = manuals,
            ranks = ranks
        )
    }
}

object OrganWriter {
    fun write(o: OrganState): String {
        val rankNo = HashMap<Long, Int>()
        o.ranks.forEachIndexed { i, r -> rankNo[r.id] = i + 1 }
        val stopById = o.stops.associateBy { it.id }

        var counter = 0
        val orderedStops = ArrayList<StopItem>()
        val perManual = ArrayList<List<Int>>()
        for (m in o.manuals) {
            val nums = ArrayList<Int>()
            for (sid in m.stopIds) {
                val s = stopById[sid] ?: continue
                counter++
                orderedStops.add(s)
                nums.add(counter)
            }
            perManual.add(nums)
        }

        fun clean(s: String) = s.replace("\r", " ").replace("\n", " ").trim()

        val sb = StringBuilder()
        fun sec(name: String) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append("[").append(name).append("]\n")
        }
        fun kv(k: String, v: Any) {
            sb.append(k).append("=").append(v).append("\n")
        }

        val manualCount = o.manuals.count { !it.isPedal }

        sec("Organ")
        kv("ChurchName", clean(o.churchName))
        if (o.builder.isNotBlank()) kv("OrganBuilder", clean(o.builder))
        if (o.buildDate.isNotBlank()) kv("OrganBuildDate", clean(o.buildDate))
        kv("NumberOfManuals", manualCount)
        kv("HasPedals", "Y")
        kv("NumberOfEnclosures", 0)
        kv("NumberOfTremulants", 0)
        kv("NumberOfWindchestGroups", 1)
        kv("NumberOfReversiblePistons", 0)
        kv("NumberOfGenerals", 0)
        kv("NumberOfDivisionalCouplers", 0)
        kv("NumberOfPanels", 0)
        kv("NumberOfRanks", o.ranks.size)

        sec("WindchestGroup001")
        kv("Name", "Main")
        kv("NumberOfEnclosures", 0)
        kv("NumberOfTremulants", 0)

        o.manuals.forEachIndexed { idx, m ->
            val keys = if (m.isPedal) 32 else 61
            sec("Manual" + p3(idx))
            kv("Name", clean(m.name))
            kv("NumberOfLogicalKeys", keys)
            kv("FirstAccessibleKeyLogicalKeyNumber", 1)
            kv("FirstAccessibleKeyMIDINoteNumber", 36)
            kv("NumberOfAccessibleKeys", keys)
            kv("Displayed", "Y")
            val nums = perManual[idx]
            kv("NumberOfStops", nums.size)
            kv("NumberOfCouplers", 0)
            kv("NumberOfDivisionals", 0)
            kv("NumberOfTremulants", 0)
            nums.forEachIndexed { j, n -> kv("Stop" + p3(j + 1), n) }
        }

        orderedStops.forEachIndexed { i, s ->
            val links = s.links.filter { rankNo.containsKey(it.rankId) }
            sec("Stop" + p3(i + 1))
            kv("Name", clean(s.name))
            kv("NumberOfRanks", links.size)
            links.forEachIndexed { j, l ->
                val key = "Rank" + p3(j + 1)
                kv(key, rankNo[l.rankId] ?: 1)
                for ((k, v) in l.opts) kv(key + k, v)
            }
            for ((k, v) in s.extras) kv(k, v)
        }

        o.ranks.forEachIndexed { i, r ->
            sec("Rank" + p3(i + 1))
            kv("Name", clean(r.name))
            kv("WindchestGroup", 1)
            for ((k, v) in r.props) {
                if (k == "WindchestGroup") continue
                kv(k, v)
            }
        }

        return sb.toString()
    }
}
