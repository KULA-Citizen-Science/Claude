package com.kula.shadowroutines.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.kula.shadowroutines.core.routine.RoutineTemplate
import com.kula.shadowroutines.data.db.RoutineWithItems
import com.kula.shadowroutines.ui.AppViewModel
import com.kula.shadowroutines.ui.Stats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenRoutine: (Long) -> Unit,
) {
    val active by viewModel.activeRoutines.collectAsState()
    val stats by viewModel.stats.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let { viewModel.importCorpus(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shadow Routines") },
                actions = { OverflowMenu(onRescan = viewModel::rescanCorpus, onImport = { importLauncher.launch(null) }) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { StatsRow(stats) }

            if (active.isNotEmpty()) {
                item { SectionHeader("In progress") }
                items(active, key = { it.routine.id }) { routine ->
                    ActiveRoutineCard(routine, onClick = { onOpenRoutine(routine.routine.id) })
                }
            }

            item { SectionHeader("I'm about to…") }
            items(viewModel.templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onClick = { viewModel.startRoutine(template.id, onOpenRoutine) },
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(onRescan: () -> Unit, onImport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Rescan bundled corpus") },
            onClick = { expanded = false; onRescan() },
        )
        DropdownMenuItem(
            text = { Text("Import quotes from folder…") },
            onClick = { expanded = false; onImport() },
        )
    }
}

@Composable
private fun StatsRow(stats: Stats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatChip("Routines completed", stats.completedRoutines, Modifier.weight(1f))
        StatChip("Unique quotes seen", stats.uniqueQuotesSeen, Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TemplateCard(template: RoutineTemplate, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${template.aspects.size} soft steps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ActiveRoutineCard(routine: RoutineWithItems, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Text(routine.routine.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${routine.checkedCount} / ${routine.totalCount} done",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
