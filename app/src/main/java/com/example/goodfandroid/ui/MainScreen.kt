package com.example.goodfandroid.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.goodfandroid.model.Manual
import com.example.goodfandroid.viewmodel.OrganViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

enum class EditorTab {
    ORGAN, MANUALS, WINDCHESTS, TREMULANTS, RANKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: OrganViewModel = viewModel()) {
    val context = LocalContext.current
    val organ = viewModel.organ
    var currentTab by remember { mutableStateOf(EditorTab.ORGAN) }

    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val content = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "opened.organ"
                viewModel.loadFromText(content, name)
            }
        } catch (e: Exception) {
            viewModel.setError("Open failed: ${e.message}")
        }
    }

    val importRanksLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val content = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                viewModel.importRanksFromText(content)
            }
        } catch (e: Exception) {
            viewModel.setError("Import ranks failed: ${e.message}")
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(viewModel.getOrganText().toByteArray(Charsets.UTF_8))
            }
            viewModel.setStatus("Saved successfully")
        } catch (e: Exception) {
            viewModel.setError("Save failed: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GoOdf Android") },
                actions = {
                    TextButton(onClick = { viewModel.newOrgan() }) { Text("New") }
                    TextButton(onClick = { openLauncher.launch(arrayOf("*/*")) }) { Text("Open") }
                    TextButton(onClick = {
                        val name = (organ.organFileName.ifBlank { "NewOrgan" }) + ".organ"
                        saveLauncher.launch(name)
                    }) { Text("Save") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            viewModel.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
            viewModel.statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            ScrollableTabRow(selectedTabIndex = currentTab.ordinal) {
                EditorTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            val label = when (tab) {
                                EditorTab.ORGAN -> "Organ"
                                EditorTab.MANUALS -> "Manuals (${organ.manuals.size})"
                                EditorTab.WINDCHESTS -> "Windchest (${organ.windchests.size})"
                                EditorTab.TREMULANTS -> "Tremulant (${organ.tremulants.size})"
                                EditorTab.RANKS -> "Ranks (${organ.ranks.size})"
                            }
                            Text(label)
                        }
                    )
                }
            }

            when (currentTab) {
                EditorTab.ORGAN -> OrganInfoTab(viewModel)
                EditorTab.MANUALS -> ManualsTab(viewModel)
                EditorTab.WINDCHESTS -> WindchestsTab(viewModel)
                EditorTab.TREMULANTS -> TremulantsTab(viewModel)
                EditorTab.RANKS -> RanksTab(viewModel) {
                    importRanksLauncher.launch(arrayOf("*/*"))
                }
            }
        }
    }
}

@Composable
fun OrganInfoTab(vm: OrganViewModel) {
    val o = vm.organ
    var name by remember(o.organName) { mutableStateOf(o.organName) }
    var church by remember(o.churchName) { mutableStateOf(o.churchName) }
    var fileName by remember(o.organFileName) { mutableStateOf(o.organFileName) }

    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Organ properties",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                vm.updateOrganInfo(it, church, fileName)
            },
            label = { Text("Organ name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = church,
            onValueChange = {
                church = it
                vm.updateOrganInfo(name, it, fileName)
            },
            label = { Text("Church name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fileName,
            onValueChange = {
                fileName = it
                vm.updateOrganInfo(name, church, it)
            },
            label = { Text("File name (without .organ)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Manuals: ${o.manuals.size}  |  Windchests: ${o.windchests.size}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Tremulants: ${o.tremulants.size}  |  Ranks: ${o.ranks.size}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ManualsTab(vm: OrganViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.addManual(isPedal = true) }) { Text("+ Pedal") }
            Button(onClick = { vm.addManual(isPedal = false) }) { Text("+ Manual") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.organ.manuals, key = { it.number }) { manual ->
                ManualCard(
                    manual = manual,
                    onUpdate = { vm.updateManual(it) },
                    onDelete = { vm.deleteManual(manual.number) }
                )
            }
        }
    }
}

@Composable
fun ManualCard(
    manual: Manual,
    onUpdate: (Manual) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var name by remember(manual.name) { mutableStateOf(manual.name) }
    var keys by remember(manual.numberOfLogicalKeys) {
        mutableStateOf(manual.numberOfLogicalKeys.toString())
    }
    var midi by remember(manual.midiInputNumber) {
        mutableStateOf(manual.midiInputNumber.toString())
    }
    var firstNote by remember(manual.firstAccessibleKeyMIDINoteNumber) {
        mutableStateOf(manual.firstAccessibleKeyMIDINoteNumber.toString())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Manual${manual.number.toString().padStart(3, '0')}: ${manual.name}",
                    fontWeight = FontWeight.Medium
                )
                Text(if (expanded) "▲" else "▼")
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(manual.copy(name = it))
                    },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = keys,
                    onValueChange = {
                        keys = it
                        it.toIntOrNull()?.let { v ->
                            onUpdate(
                                manual.copy(
                                    numberOfLogicalKeys = v,
                                    numberOfAccessibleKeys = v
                                )
                            )
                        }
                    },
                    label = { Text("Number of keys") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = firstNote,
                    onValueChange = {
                        firstNote = it
                        it.toIntOrNull()?.let { v ->
                            onUpdate(manual.copy(firstAccessibleKeyMIDINoteNumber = v))
                        }
                    },
                    label = { Text("First MIDI note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = midi,
                    onValueChange = {
                        midi = it
                        it.toIntOrNull()?.let { v ->
                            onUpdate(manual.copy(midiInputNumber = v))
                        }
                    },
                    label = { Text("MIDI Input Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
fun WindchestsTab(vm: OrganViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Button(onClick = { vm.addWindchest() }) { Text("+ Windchest") }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.organ.windchests, key = { it.number }) { w ->
                var expanded by remember { mutableStateOf(false) }
                var name by remember(w.name) { mutableStateOf(w.name) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WindchestGroup${w.number.toString().padStart(3, '0')}: ${w.name}",
                            fontWeight = FontWeight.Medium
                        )
                        if (expanded) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    vm.updateWindchest(w.copy(name = it))
                                },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = { vm.deleteWindchest(w.number) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TremulantsTab(vm: OrganViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Button(onClick = { vm.addTremulant() }) { Text("+ Tremulant") }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.organ.tremulants, key = { it.number }) { t ->
                var expanded by remember { mutableStateOf(false) }
                var name by remember(t.name) { mutableStateOf(t.name) }
                var type by remember(t.tremulantType) { mutableStateOf(t.tremulantType) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Tremulant${t.number.toString().padStart(3, '0')}: ${t.name}",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Type: ${t.tremulantType}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (expanded) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    vm.updateTremulant(t.copy(name = it))
                                },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = type,
                                onValueChange = {
                                    type = it
                                    vm.updateTremulant(t.copy(tremulantType = it))
                                },
                                label = { Text("Type (Synth/Wave)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = { vm.deleteTremulant(t.number) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RanksTab(vm: OrganViewModel, onImportRanks: () -> Unit) {
    var newRankName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                vm.addRank(name = newRankName.ifBlank { "New Rank" })
                newRankName = ""
            }) { Text("+ Rank") }
            Button(onClick = onImportRanks) { Text("Import Ranks from .organ") }
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = newRankName,
            onValueChange = { newRankName = it },
            label = { Text("New rank name (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.organ.ranks, key = { it.number }) { r ->
                var expanded by remember { mutableStateOf(false) }
                var name by remember(r.name) { mutableStateOf(r.name) }
                var pipes by remember(r.numberOfLogicalPipes) {
                    mutableStateOf(r.numberOfLogicalPipes.toString())
                }
                var midi by remember(r.firstMidiNoteNumber) {
                    mutableStateOf(r.firstMidiNoteNumber.toString())
                }
                var wc by remember(r.windchestGroup) {
                    mutableStateOf(r.windchestGroup.toString())
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Rank${r.number.toString().padStart(3, '0')}: ${r.name}",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Pipes: ${r.numberOfLogicalPipes}  |  MIDI: ${r.firstMidiNoteNumber}  |  WC: ${r.windchestGroup}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (expanded) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    vm.updateRank(r.copy(name = it))
                                },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pipes,
                                onValueChange = {
                                    pipes = it
                                    it.toIntOrNull()?.let { v ->
                                        vm.updateRank(r.copy(numberOfLogicalPipes = v))
                                    }
                                },
                                label = { Text("Number of pipes") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = midi,
                                onValueChange = {
                                    midi = it
                                    it.toIntOrNull()?.let { v ->
                                        vm.updateRank(r.copy(firstMidiNoteNumber = v))
                                    }
                                },
                                label = { Text("First MIDI note") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = wc,
                                onValueChange = {
                                    wc = it
                                    it.toIntOrNull()?.let { v ->
                                        vm.updateRank(r.copy(windchestGroup = v))
                                    }
                                },
                                label = { Text("Windchest Group") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = { vm.deleteRank(r.number) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
