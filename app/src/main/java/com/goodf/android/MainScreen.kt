package com.goodf.android

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val TABS = listOf("Organ", "Stops", "Ranks", "Import")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: OrganViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("GoOdf Android") }) },
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { },
                        label = { Text(t) }
                    )
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            val msg = vm.message
            if (msg != null) {
                Card(Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, Modifier.weight(1f))
                        TextButton(onClick = { vm.message = null }) { Text("OK") }
                    }
                }
            }
            when (tab) {
                0 -> OrganTab(vm)
                1 -> StopsTab(vm)
                2 -> RanksTab(vm)
                else -> ImportTab(vm)
            }
        }
    }
}

// ============================================================ ORGAN TAB

@Composable
fun OrganTab(vm: OrganViewModel) {
    val o = vm.organ
    var confirmNew by remember { mutableStateOf(false) }
    var confirmOpen by remember { mutableStateOf(false) }

    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) vm.export(uri) }

    val opener = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.loadAll(uri) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = o.churchName,
            onValueChange = { vm.setChurchName(it) },
            label = { Text("Pangalan ng organ (ChurchName)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = o.builder,
            onValueChange = { vm.setBuilder(it) },
            label = { Text("Builder (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Mga manual", style = MaterialTheme.typography.titleMedium)
        for (m in o.manuals) {
            OutlinedTextField(
                value = m.name,
                onValueChange = { vm.renameManual(m.id, it) },
                label = { Text(if (m.isPedal) "Pedal (32 keys)" else "Manual (61 keys)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.addManual() }) { Text("+ Manual") }
            OutlinedButton(onClick = { vm.removeLastManual() }) { Text("- Huling manual") }
        }

        HorizontalDivider()
        Text("Ranks: " + o.ranks.size + "   Stops: " + o.stops.size)
        for (w in vm.warnings()) {
            Text("Babala: " + w, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                val base = o.churchName.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "organ" }
                saver.launch("$base.organ")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("I-save bilang .organ") }

        OutlinedButton(onClick = { confirmOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Buksan ang .organ para i-edit")
        }
        OutlinedButton(onClick = { confirmNew = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Bagong organ")
        }
        Text(
            "Awtomatikong nase-save ang ginagawa mo sa loob ng app.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text("Bagong organ?") },
            text = { Text("Mabubura ang kasalukuyang ginagawa.") },
            confirmButton = { TextButton(onClick = { vm.newOrgan(); confirmNew = false }) { Text("Oo") } },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Huwag") } }
        )
    }
    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            title = { Text("Buksan ang .organ?") },
            text = { Text("Papalitan nito ang kasalukuyang ginagawa.") },
            confirmButton = {
                TextButton(onClick = { confirmOpen = false; opener.launch(arrayOf("*/*")) }) { Text("Oo") }
            },
            dismissButton = { TextButton(onClick = { confirmOpen = false }) { Text("Huwag") } }
        )
    }
}

// ============================================================ STOPS TAB

private class StopDialogState(val manualId: Long, val stop: StopItem?)

@Composable
fun StopsTab(vm: OrganViewModel) {
    val o = vm.organ
    var dialog by remember { mutableStateOf<StopDialogState?>(null) }
    val stopMap = o.stops.associateBy { it.id }
    val rankMap = o.ranks.associateBy { it.id }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        for (m in o.manuals) {
            item(key = "h" + m.id) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { dialog = StopDialogState(m.id, null) }) { Text("+ Stop") }
                }
            }
            itemsIndexed(m.stopIds, key = { _, id -> "s$id" }) { _, sid ->
                val s = stopMap[sid]
                if (s != null) {
                    val rn = s.links.mapNotNull { rankMap[it.rankId]?.name }.joinToString(", ")
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name)
                            Text(rn, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { vm.moveStop(m.id, s.id, -1) }) { Text("↑") }
                        TextButton(onClick = { vm.moveStop(m.id, s.id, 1) }) { Text("↓") }
                        TextButton(onClick = { dialog = StopDialogState(m.id, s) }) { Text("Edit") }
                        TextButton(onClick = { vm.deleteStop(s.id) }) { Text("X") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    val d = dialog
    if (d != null) {
        StopDialog(
            state = d,
            ranks = o.ranks,
            onDismiss = { dialog = null },
            onOk = { name, ids ->
                if (d.stop == null) vm.addStop(d.manualId, name, ids) else vm.updateStop(d.stop.id, name, ids)
                dialog = null
            }
        )
    }
}

@Composable
private fun StopDialog(
    state: StopDialogState,
    ranks: List<RankItem>,
    onDismiss: () -> Unit,
    onOk: (String, List<Long>) -> Unit
) {
    var name by remember { mutableStateOf(state.stop?.name ?: "") }
    var sel by remember {
        mutableStateOf<Set<Long>>(state.stop?.links?.map { it.rankId }?.toSet() ?: emptySet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.stop == null) "Bagong stop" else "I-edit ang stop") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Pangalan ng stop") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.padding(4.dp))
                Text("Piliin ang rank(s):")
                if (ranks.isEmpty()) {
                    Text("Wala pang rank. Gumawa sa Ranks tab o mag-import muna.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(ranks, key = { it.id }) { r ->
                            CheckRow(r.name, r.id in sel) {
                                sel = if (r.id in sel) sel - r.id else sel + r.id
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && sel.isNotEmpty(),
                onClick = { onOk(name, ranks.filter { it.id in sel }.map { it.id }) }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kanselahin") } }
    )
}

@Composable
fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

// ============================================================ RANKS TAB

@Composable
fun RanksTab(vm: OrganViewModel) {
    val o = vm.organ
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<RankItem?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = { adding = true }) { Text("+ Bagong rank") }
        LazyColumn(Modifier.weight(1f)) {
            items(o.ranks, key = { it.id }) { r ->
                val used = o.stops.count { s -> s.links.any { it.rankId == r.id } }
                val pipes = r.props["NumberOfLogicalPipes"] ?: "?"
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name)
                        Text("$pipes pipes · gamit ng $used stop(s)", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { renaming = r }) { Text("Edit") }
                    TextButton(onClick = { vm.deleteRank(r.id) }) { Text("X") }
                }
                HorizontalDivider()
            }
        }
    }

    if (adding) {
        RankDialog(onDismiss = { adding = false }) { name, first, count, template ->
            vm.addRank(name, first, count, template)
            adding = false
        }
    }
    val rr = renaming
    if (rr != null) {
        var text by remember(rr.id) { mutableStateOf(rr.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Palitan ang pangalan") },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = { vm.renameRank(rr.id, text.trim()); renaming = null }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Kanselahin") } }
        )
    }
}

@Composable
private fun RankDialog(onDismiss: () -> Unit, onOk: (String, Int, Int, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("36") }
    var count by remember { mutableStateOf("61") }
    var template by remember { mutableStateOf("{n}.wav") }
    val f = first.toIntOrNull()
    val c = count.toIntOrNull()
    val valid = name.isNotBlank() && f != null && f in 0..127 && c != null && c in 1..127 && template.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bagong rank") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Pangalan ng rank") }, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = first, onValueChange = { first = it },
                        label = { Text("Unang MIDI note") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = count, onValueChange = { count = it },
                        label = { Text("Bilang ng pipes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = template, onValueChange = { template = it },
                    label = { Text("Path ng pipe (gamitin ang {n} = MIDI note, 3 digit)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Hal: Principal8\\{n}.wav", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onOk(name, f ?: 36, c ?: 61, template) }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kanselahin") } }
    )
}

// ============================================================ IMPORT TAB

@Composable
fun ImportTab(vm: OrganViewModel) {
    val src = vm.source
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.loadSource(uri) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Pumili ng lumang .organ") }

        if (src == null) {
            Spacer(Modifier.padding(8.dp))
            Text("Pumili ng .organ file, tapos lagyan ng check ang mga stop o rank na gusto mong dalhin sa bagong organ.")
        } else {
            var selStops by remember(src) { mutableStateOf<Set<Long>>(emptySet()) }
            var selRanks by remember(src) { mutableStateOf<Set<Long>>(emptySet()) }
            var target by remember(src) { mutableStateOf<Long?>(null) }
            var menu by remember { mutableStateOf(false) }
            val rankName = remember(src) { src.ranks.associate { it.id to it.name } }
            val allStopIds = remember(src) { src.manuals.flatMap { m -> m.stops.map { it.id } }.toSet() }
            val targetLabel = vm.organ.manuals.firstOrNull { it.id == target }?.name ?: "Auto"

            Spacer(Modifier.padding(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ilagay sa: ")
                Box {
                    OutlinedButton(onClick = { menu = true }) { Text(targetLabel) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Auto (pedal→pedal, manual→manual)") },
                            onClick = { target = null; menu = false })
                        for (m in vm.organ.manuals) {
                            DropdownMenuItem(text = { Text(m.name) },
                                onClick = { target = m.id; menu = false })
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selStops = allStopIds }) { Text("Piliin lahat ng stops") }
                TextButton(onClick = { selStops = emptySet(); selRanks = emptySet() }) { Text("Burahin ang pili") }
            }
            Button(
                enabled = selStops.isNotEmpty() || selRanks.isNotEmpty(),
                onClick = {
                    vm.importSelected(selStops, selRanks, target)
                    selStops = emptySet()
                    selRanks = emptySet()
                }
            ) { Text("I-import (" + selStops.size + " stops, " + selRanks.size + " ranks)") }

            LazyColumn(Modifier.weight(1f)) {
                for (m in src.manuals) {
                    item(key = "m" + m.number + m.name) {
                        Text(m.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(m.stops, key = { it.id }) { s ->
                        val rn = s.links.mapNotNull { rankName[it.rankId] }.joinToString(", ")
                        CheckRow(s.name + "  [" + rn + "]", s.id in selStops) {
                            selStops = if (s.id in selStops) selStops - s.id else selStops + s.id
                        }
                    }
                }
                item(key = "rh") {
                    Text(
                        "Mga rank (kung ranks lang ang kailangan)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                items(src.ranks, key = { it.id }) { r ->
                    CheckRow(r.name, r.id in selRanks) {
                        selRanks = if (r.id in selRanks) selRanks - r.id else selRanks + r.id
                    }
                }
            }
        }
    }
}
