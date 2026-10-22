package com.example.goodfandroid.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodfandroid.model.Coupler
import com.example.goodfandroid.model.Stop
import com.example.goodfandroid.model.Tremulant
import com.example.goodfandroid.viewmodel.OrganViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: OrganViewModel = viewModel()) {
    val context = LocalContext.current
    val organ = viewModel.organ
    val error = viewModel.errorMessage
    val statusMessage = viewModel.statusMessage

    // Use OpenDocument with */* so .organ files are always selectable
    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.setStatus("No file selected")
            return@rememberLauncherForActivityResult
        }
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "opened.organ"
                if (content.isBlank()) {
                    viewModel.setError("File is empty")
                } else {
                    viewModel.loadFromText(content, name)
                }
            } ?: viewModel.setError("Cannot open file stream")
        } catch (e: Exception) {
            viewModel.setError("Error reading file: ${e.message}")
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            try {
                viewModel.getOrganText()?.let { text ->
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(text.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.setStatus("Saved successfully")
                }
            } catch (e: Exception) {
                viewModel.setError("Save failed: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GoOdf Android") },
                actions = {
                    TextButton(onClick = {
                        // */* allows selecting any file including .organ
                        openLauncher.launch(arrayOf("*/*"))
                    }) {
                        Text("Open")
                    }
                    if (organ != null) {
                        TextButton(onClick = {
                            val suggestedName = (viewModel.fileName ?: "edited").let {
                                if (it.endsWith(".organ")) it else "$it.organ"
                            }
                            saveLauncher.launch(suggestedName)
                        }) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (statusMessage != null && error == null) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (organ == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No organ file loaded",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            openLauncher.launch(arrayOf("*/*"))
                        }) {
                            Text("Open .organ file")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Tip: Select any .organ file from your storage.\nSupports Stops, Ranks, Couplers & Tremulants.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                // Header info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            organ.organName.ifBlank { "Unnamed Organ" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (organ.churchName.isNotBlank()) {
                            Text(organ.churchName, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "File: ${viewModel.fileName ?: "?"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Manuals: ${organ.manuals.size}  |  Stops: ${organ.stops.size}  |  Ranks: ${organ.ranks.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Couplers: ${organ.couplers.size}  |  Tremulants: ${organ.tremulants.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (organ.stops.isEmpty() && organ.ranks.isEmpty() && organ.couplers.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            "Walang nakitang Stop/Rank/Coupler.\n" +
                            "Baka ibang format ang .organ file o walang [Stopxxx]/[Rankxxx] sections.\n" +
                            "Subukan ang sample/demo.organ mula sa repo.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // STOPS
                    item {
                        Text(
                            "Stops (${organ.stops.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (organ.stops.isEmpty()) {
                        item {
                            Text("— walang stop —", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    items(organ.stops) { stop ->
                        StopCard(
                            stop = stop,
                            onMidiKeyChange = { viewModel.updateStopMidiKey(stop.number, it) },
                            onNameChange = { viewModel.updateStopName(stop.number, it) }
                        )
                    }

                    // RANKS (show list too so user can see them)
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ranks (${organ.ranks.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (organ.ranks.isEmpty()) {
                        item {
                            Text("— walang rank —", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    items(organ.ranks) { rank ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Rank${rank.number.toString().padStart(3, '0')}: ${rank.name}",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "MIDI start: ${rank.firstMidiNoteNumber}  |  Pipes: ${rank.numberOfLogicalPipes}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // COUPLERS
                    if (organ.couplers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Couplers (${organ.couplers.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(organ.couplers) { coupler ->
                            CouplerCard(
                                coupler = coupler,
                                onMidiKeyChange = { viewModel.updateCouplerMidiKey(coupler.number, it) },
                                onNameChange = { viewModel.updateCouplerName(coupler.number, it) }
                            )
                        }
                    }

                    // TREMULANTS
                    if (organ.tremulants.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tremulants (${organ.tremulants.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(organ.tremulants) { trem ->
                            TremulantCard(
                                tremulant = trem,
                                onMidiKeyChange = { viewModel.updateTremulantMidiKey(trem.number, it) },
                                onNameChange = { viewModel.updateTremulantName(trem.number, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StopCard(
    stop: Stop,
    onMidiKeyChange: (Int?) -> Unit,
    onNameChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var midiText by remember(stop.stopControlMIDIKeyNumber) {
        mutableStateOf(stop.stopControlMIDIKeyNumber?.toString() ?: "")
    }
    var nameText by remember(stop.name) { mutableStateOf(stop.name) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Stop${stop.number.toString().padStart(3, '0')}: ${stop.name}",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "MIDI: ${stop.stopControlMIDIKeyNumber ?: "—"}  |  Ranks: ${stop.rankNumbers.joinToString()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(if (expanded) "▲" else "▼")
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        onNameChange(it)
                    },
                    label = { Text("Stop Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = midiText,
                    onValueChange = {
                        midiText = it
                        onMidiKeyChange(it.toIntOrNull())
                    },
                    label = { Text("StopControlMIDIKeyNumber") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("MIDI note that controls this stop") }
                )
            }
        }
    }
}

@Composable
fun CouplerCard(
    coupler: Coupler,
    onMidiKeyChange: (Int?) -> Unit,
    onNameChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var midiText by remember(coupler.stopControlMIDIKeyNumber) {
        mutableStateOf(coupler.stopControlMIDIKeyNumber?.toString() ?: "")
    }
    var nameText by remember(coupler.name) { mutableStateOf(coupler.name) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Coupler${coupler.number.toString().padStart(3, '0')}: ${coupler.name}",
                        fontWeight = FontWeight.Medium
                    )
                    val src = coupler.sourceManual?.toString() ?: "?"
                    val dst = coupler.destinationManual?.toString() ?: "?"
                    Text(
                        "MIDI: ${coupler.stopControlMIDIKeyNumber ?: "—"}  |  $src → $dst  |  ${coupler.couplerType}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(if (expanded) "▲" else "▼")
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        onNameChange(it)
                    },
                    label = { Text("Coupler Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = midiText,
                    onValueChange = {
                        midiText = it
                        onMidiKeyChange(it.toIntOrNull())
                    },
                    label = { Text("StopControlMIDIKeyNumber") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun TremulantCard(
    tremulant: Tremulant,
    onMidiKeyChange: (Int?) -> Unit,
    onNameChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var midiText by remember(tremulant.stopControlMIDIKeyNumber) {
        mutableStateOf(tremulant.stopControlMIDIKeyNumber?.toString() ?: "")
    }
    var nameText by remember(tremulant.name) { mutableStateOf(tremulant.name) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Tremulant${tremulant.number.toString().padStart(3, '0')}: ${tremulant.name}",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "MIDI: ${tremulant.stopControlMIDIKeyNumber ?: "—"}  |  Type: ${tremulant.tremulantType}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(if (expanded) "▲" else "▼")
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        onNameChange(it)
                    },
                    label = { Text("Tremulant Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = midiText,
                    onValueChange = {
                        midiText = it
                        onMidiKeyChange(it.toIntOrNull())
                    },
                    label = { Text("StopControlMIDIKeyNumber") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}
