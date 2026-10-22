package com.goodf.android

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class OrganViewModel(app: Application) : AndroidViewModel(app) {

    var organ by mutableStateOf(OrganState.default())
        private set

    var source by mutableStateOf<SourceOrgan?>(null)
        private set

    var message by mutableStateOf<String?>(null)

    private var saveJob: Job? = null
    private var touched = false

    private val autosaveFile: File
        get() = File(getApplication<Application>().filesDir, "autosave.organ")

    init {
        // Ibalik ang huling ginagawa kung meron
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                try {
                    val f = autosaveFile
                    if (f.exists()) {
                        val src = OrganReader.read(Ini.decode(f.readBytes()))
                        if (src != null) OrganState.fromSource(src) else null
                    } else null
                } catch (e: Throwable) {
                    null
                }
            }
            if (restored != null && !touched) organ = restored
        }
    }

    private fun change(block: (OrganState) -> OrganState) {
        touched = true
        organ = block(organ)
        scheduleSave()
    }

    private fun scheduleSave() {
        val snapshot = organ
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(800)
            try {
                autosaveFile.writeBytes(Ini.encode(OrganWriter.write(snapshot)))
            } catch (e: Throwable) {
                // balewalain
            }
        }
    }

    // ---------- Organ info ----------
    fun setChurchName(v: String) = change { it.copy(churchName = v) }
    fun setBuilder(v: String) = change { it.copy(builder = v) }

    fun newOrgan() {
        touched = true
        organ = OrganState.default()
        source = null
        scheduleSave()
    }

    // ---------- Manuals ----------
    fun addManual() = change { st ->
        val n = st.manuals.count { !it.isPedal }
        if (n >= 5) st
        else st.copy(manuals = st.manuals + ManualItem(Ids.next(), "Manual " + (n + 1), false, emptyList()))
    }

    fun removeLastManual() = change { st ->
        val nonPedal = st.manuals.filter { !it.isPedal }
        if (nonPedal.size <= 1) {
            st
        } else {
            val last = nonPedal.last()
            val gone = last.stopIds.toSet()
            st.copy(
                manuals = st.manuals.filter { it.id != last.id },
                stops = st.stops.filter { it.id !in gone }
            )
        }
    }

    fun renameManual(id: Long, name: String) = change { st ->
        st.copy(manuals = st.manuals.map { if (it.id == id) it.copy(name = name) else it })
    }

    // ---------- Ranks ----------
    fun addRank(name: String, firstNote: Int, count: Int, template: String) = change { st ->
        val props = LinkedHashMap<String, String>()
        props["FirstMidiNoteNumber"] = firstNote.toString()
        props["NumberOfLogicalPipes"] = count.toString()
        for (i in 0 until count) {
            props["Pipe" + p3(i + 1)] = template.replace("{n}", p3(firstNote + i))
        }
        st.copy(ranks = st.ranks + RankItem(Ids.next(), name.trim(), props))
    }

    fun renameRank(id: Long, name: String) = change { st ->
        st.copy(ranks = st.ranks.map { if (it.id == id) it.copy(name = name) else it })
    }

    fun deleteRank(id: Long) = change { st ->
        val stops = st.stops.map { s -> s.copy(links = s.links.filter { it.rankId != id }) }
        val dead = stops.filter { it.links.isEmpty() }.map { it.id }.toSet()
        st.copy(
            ranks = st.ranks.filter { it.id != id },
            stops = stops.filter { it.id !in dead },
            manuals = st.manuals.map { m -> m.copy(stopIds = m.stopIds.filter { it !in dead }) }
        )
    }

    // ---------- Stops ----------
    fun addStop(manualId: Long, name: String, rankIds: List<Long>) = change { st ->
        val s = StopItem(Ids.next(), name.trim(), rankIds.map { RankLink(it) })
        st.copy(
            stops = st.stops + s,
            manuals = st.manuals.map { if (it.id == manualId) it.copy(stopIds = it.stopIds + s.id) else it }
        )
    }

    fun updateStop(id: Long, name: String, rankIds: List<Long>) = change { st ->
        st.copy(stops = st.stops.map { s ->
            if (s.id != id) s
            else s.copy(
                name = name.trim(),
                links = rankIds.map { rid -> s.links.firstOrNull { it.rankId == rid } ?: RankLink(rid) }
            )
        })
    }

    fun deleteStop(id: Long) = change { st ->
        st.copy(
            stops = st.stops.filter { it.id != id },
            manuals = st.manuals.map { m -> m.copy(stopIds = m.stopIds.filter { it != id }) }
        )
    }

    fun moveStop(manualId: Long, stopId: Long, delta: Int) = change { st ->
        st.copy(manuals = st.manuals.map { m ->
            if (m.id != manualId) {
                m
            } else {
                val l = m.stopIds.toMutableList()
                val i = l.indexOf(stopId)
                val j = i + delta
                if (i < 0 || j < 0 || j >= l.size) {
                    m
                } else {
                    l.removeAt(i)
                    l.add(j, stopId)
                    m.copy(stopIds = l)
                }
            }
        })
    }

    // ---------- Import ----------
    private fun readSource(uri: Uri): SourceOrgan? {
        try {
            val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            return OrganReader.read(Ini.decode(bytes))
        } catch (e: Throwable) {
            return null
        }
    }

    /** Buksan ang lumang .organ para pumili ng stops/ranks na i-import. */
    fun loadSource(uri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) { readSource(uri) }
            if (res == null) {
                message = "Walang nakitang Stop/Rank sa file. Siguraduhing valid na GrandOrgue .organ ito."
            } else {
                source = res
                message = "Nabasa: " + res.ranks.size + " ranks, " + res.manuals.sumOf { it.stops.size } + " stops."
            }
        }
    }

    /** Buksan ang .organ at palitan ang kasalukuyang ginagawa (para i-edit). */
    fun loadAll(uri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) { readSource(uri) }
            if (res == null) {
                message = "Hindi mabasa ang file bilang .organ."
            } else {
                touched = true
                organ = OrganState.fromSource(res)
                scheduleSave()
                message = "Nabuksan: " + organ.ranks.size + " ranks, " + organ.stops.size + " stops."
            }
        }
    }

    private fun autoTarget(st: OrganState, m: SrcManual): Long {
        if (m.isPedal) return st.manuals.first { it.isPedal }.id
        val nonPedal = st.manuals.filter { !it.isPedal }
        val idx = (m.number - 1).coerceIn(0, nonPedal.size - 1)
        return nonPedal[idx].id
    }

    /**
     * Mag-import ng napiling stops (kasama ang ranks nila) at ng mga napiling ranks lang.
     * targetManualId = null ay "Auto" (pedal -> pedal, manual k -> manual k).
     */
    fun importSelected(stopIds: Set<Long>, rankIds: Set<Long>, targetManualId: Long?) {
        val src = source ?: return
        val st = organ
        val rankById = src.ranks.associateBy { it.id }
        val rankMap = HashMap<Long, Long>()
        val newRanks = ArrayList<RankItem>()
        fun mapRank(r: RankItem): Long = rankMap.getOrPut(r.id) {
            val nid = Ids.next()
            newRanks.add(r.copy(id = nid))
            nid
        }

        val newStops = ArrayList<StopItem>()
        val adds = HashMap<Long, MutableList<Long>>()
        for (m in src.manuals) {
            for (s in m.stops) {
                if (s.id !in stopIds) continue
                val links = s.links.mapNotNull { l ->
                    val r = rankById[l.rankId]
                    if (r == null) null else RankLink(mapRank(r), l.opts)
                }
                if (links.isEmpty()) continue
                val ns = s.copy(id = Ids.next(), links = links)
                newStops.add(ns)
                val target = targetManualId ?: autoTarget(st, m)
                adds.getOrPut(target) { ArrayList() }.add(ns.id)
            }
        }
        for (rid in rankIds) {
            val r = rankById[rid]
            if (r != null) mapRank(r)
        }

        change { cur ->
            cur.copy(
                ranks = cur.ranks + newRanks,
                stops = cur.stops + newStops,
                manuals = cur.manuals.map { m ->
                    val extra = adds[m.id]
                    if (extra == null) m else m.copy(stopIds = m.stopIds + extra)
                }
            )
        }
        message = "Na-import: " + newStops.size + " stops, " + newRanks.size + " ranks."
    }

    // ---------- Export ----------
    fun warnings(): List<String> {
        val w = ArrayList<String>()
        if (organ.stops.isEmpty()) w.add("Wala pang stops.")
        for (m in organ.manuals) {
            if (m.stopIds.isEmpty()) w.add("Walang stop ang \"" + m.name + "\".")
        }
        return w
    }

    fun export(uri: Uri) {
        val snapshot = organ
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val bytes = Ini.encode(OrganWriter.write(snapshot))
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                        ?.use { it.write(bytes) } != null
                } catch (e: Throwable) {
                    false
                }
            }
            message = if (ok) {
                "Na-save ang .organ. Ilagay ito sa parehong folder ng samples para tama ang pipe paths."
            } else {
                "Hindi na-save ang file."
            }
        }
    }
}
