package com.example.goodfandroid.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.goodfandroid.model.Organ
import com.example.goodfandroid.parser.OrganParser

class OrganViewModel : ViewModel() {

    var organ by mutableStateOf<Organ?>(null)
        private set

    var fileName by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun loadFromText(content: String, name: String = "untitled.organ") {
        isLoading = true
        errorMessage = null
        try {
            organ = OrganParser.parse(content)
            fileName = name
        } catch (e: Exception) {
            errorMessage = "Failed to parse: ${e.message}"
            organ = null
        } finally {
            isLoading = false
        }
    }

    fun getOrganText(): String? {
        return organ?.let { OrganParser.write(it) }
    }

    // --- Stop helpers ---
    fun updateStopMidiKey(stopNumber: Int, midiKey: Int?) {
        organ = organ?.copy(
            stops = organ!!.stops.map { stop ->
                if (stop.number == stopNumber) stop.copy(stopControlMIDIKeyNumber = midiKey)
                else stop
            }.toMutableList()
        )
    }

    fun updateStopName(stopNumber: Int, newName: String) {
        organ = organ?.copy(
            stops = organ!!.stops.map { stop ->
                if (stop.number == stopNumber) stop.copy(name = newName)
                else stop
            }.toMutableList()
        )
    }

    // --- Coupler helpers ---
    fun updateCouplerMidiKey(couplerNumber: Int, midiKey: Int?) {
        organ = organ?.copy(
            couplers = organ!!.couplers.map { c ->
                if (c.number == couplerNumber) c.copy(stopControlMIDIKeyNumber = midiKey)
                else c
            }.toMutableList()
        )
    }

    fun updateCouplerName(couplerNumber: Int, newName: String) {
        organ = organ?.copy(
            couplers = organ!!.couplers.map { c ->
                if (c.number == couplerNumber) c.copy(name = newName)
                else c
            }.toMutableList()
        )
    }

    // --- Tremulant helpers ---
    fun updateTremulantMidiKey(tremNumber: Int, midiKey: Int?) {
        organ = organ?.copy(
            tremulants = organ!!.tremulants.map { t ->
                if (t.number == tremNumber) t.copy(stopControlMIDIKeyNumber = midiKey)
                else t
            }.toMutableList()
        )
    }

    fun updateTremulantName(tremNumber: Int, newName: String) {
        organ = organ?.copy(
            tremulants = organ!!.tremulants.map { t ->
                if (t.number == tremNumber) t.copy(name = newName)
                else t
            }.toMutableList()
        )
    }

    fun clear() {
        organ = null
        fileName = null
        errorMessage = null
    }
}
