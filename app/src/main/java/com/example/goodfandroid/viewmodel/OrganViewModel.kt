package com.example.goodfandroid.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.goodfandroid.model.*
import com.example.goodfandroid.parser.OrganParser

class OrganViewModel : ViewModel() {

    var organ by mutableStateOf(Organ())
        private set

    var fileName by mutableStateOf<String?>(null)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun setStatus(msg: String) {
        statusMessage = msg
        errorMessage = null
    }

    fun setError(msg: String) {
        errorMessage = msg
        statusMessage = null
    }

    fun newOrgan() {
        organ = Organ(organName = "New Organ", organFileName = "NewOrgan")
        fileName = null
        setStatus("New organ created")
    }

    fun loadFromText(content: String, name: String = "opened.organ") {
        try {
            organ = OrganParser.parse(content)
            fileName = name
            setStatus("Loaded: ${organ.manuals.size} manuals, ${organ.windchests.size} windchests, ${organ.tremulants.size} tremulants, ${organ.ranks.size} ranks")
        } catch (e: Exception) {
            setError("Parse error: ${e.message}")
        }
    }

    fun importRanksFromText(content: String) {
        try {
            val before = organ.ranks.size
            OrganParser.importRanksFrom(content, organ)
            // Trigger recomposition
            organ = organ.copy(ranks = organ.ranks.toMutableList())
            setStatus("Imported ${organ.ranks.size - before} ranks (total now ${organ.ranks.size})")
        } catch (e: Exception) {
            setError("Import ranks failed: ${e.message}")
        }
    }

    fun getOrganText(): String = OrganParser.write(organ)

    // --- Organ info ---
    fun updateOrganInfo(name: String, church: String, fileNameWithoutExt: String) {
        organ = organ.copy(
            organName = name,
            churchName = church,
            organFileName = fileNameWithoutExt
        )
    }

    // --- Manuals ---
    fun addManual(isPedal: Boolean = false) {
        val next = if (isPedal) 0 else {
            val existing = organ.manuals.filter { it.number > 0 }.maxOfOrNull { it.number } ?: 0
            existing + 1
        }
        if (isPedal && organ.manuals.any { it.number == 0 }) {
            setError("Pedal already exists")
            return
        }
        val m = Manual(
            number = next,
            name = if (isPedal) "Pedal" else "Manual $next",
            numberOfLogicalKeys = if (isPedal) 32 else 61,
            numberOfAccessibleKeys = if (isPedal) 32 else 61,
            firstAccessibleKeyMIDINoteNumber = 36,
            midiInputNumber = next + 1
        )
        organ = organ.copy(manuals = (organ.manuals + m).sortedBy { it.number }.toMutableList())
        setStatus("Added ${m.name}")
    }

    fun updateManual(manual: Manual) {
        organ = organ.copy(
            manuals = organ.manuals.map { if (it.number == manual.number) manual else it }.toMutableList()
        )
    }

    fun deleteManual(number: Int) {
        organ = organ.copy(manuals = organ.manuals.filter { it.number != number }.toMutableList())
        setStatus("Manual deleted")
    }

    // --- Windchests ---
    fun addWindchest() {
        val next = (organ.windchests.maxOfOrNull { it.number } ?: 0) + 1
        val w = Windchest(number = next, name = "Windchest $next")
        organ = organ.copy(windchests = (organ.windchests + w).toMutableList())
        setStatus("Added Windchest $next")
    }

    fun updateWindchest(w: Windchest) {
        organ = organ.copy(
            windchests = organ.windchests.map { if (it.number == w.number) w else it }.toMutableList()
        )
    }

    fun deleteWindchest(number: Int) {
        organ = organ.copy(windchests = organ.windchests.filter { it.number != number }.toMutableList())
    }

    // --- Tremulants ---
    fun addTremulant() {
        val next = (organ.tremulants.maxOfOrNull { it.number } ?: 0) + 1
        val t = Tremulant(number = next, name = "Tremulant $next")
        organ = organ.copy(tremulants = (organ.tremulants + t).toMutableList())
        setStatus("Added Tremulant $next")
    }

    fun updateTremulant(t: Tremulant) {
        organ = organ.copy(
            tremulants = organ.tremulants.map { if (it.number == t.number) t else it }.toMutableList()
        )
    }

    fun deleteTremulant(number: Int) {
        organ = organ.copy(tremulants = organ.tremulants.filter { it.number != number }.toMutableList())
    }

    // --- Ranks ---
    fun addRank(name: String = "New Rank", pipes: Int = 61, midiStart: Int = 36, windchest: Int = 1) {
        val next = (organ.ranks.maxOfOrNull { it.number } ?: 0) + 1
        val r = Rank(
            number = next,
            name = name,
            numberOfLogicalPipes = pipes,
            firstMidiNoteNumber = midiStart,
            windchestGroup = windchest
        )
        organ = organ.copy(ranks = (organ.ranks + r).toMutableList())
        setStatus("Added Rank $next: $name")
    }

    fun updateRank(r: Rank) {
        organ = organ.copy(
            ranks = organ.ranks.map { if (it.number == r.number) r else it }.toMutableList()
        )
    }

    fun deleteRank(number: Int) {
        organ = organ.copy(ranks = organ.ranks.filter { it.number != number }.toMutableList())
    }
}
