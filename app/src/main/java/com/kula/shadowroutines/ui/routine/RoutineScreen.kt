package com.kula.shadowroutines.ui.routine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kula.shadowroutines.core.routine.SoftAspectCategory
import com.kula.shadowroutines.data.db.entity.ChecklistItemEntity
import com.kula.shadowroutines.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    viewModel: AppViewModel,
    routineId: Long,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
    val routine by viewModel.routine(routineId).collectAsState(initial = null)
    val current = routine

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.routine?.title ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (current != null) {
                Button(
                    onClick = {
                        viewModel.completeRoutine(routineId)
                        onCompleted()
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("Complete & reward me")
                }
            }
        },
    ) { padding ->
        if (current == null) return@Scaffold

        val grouped = current.items.groupBy { it.category }.toSortedMap(compareBy { it })

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                val total = current.totalCount.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { current.checkedCount.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            grouped.forEach { (categoryName, items) ->
                item { CategoryHeader(categoryName) }
                items.sortedBy { it.order }.forEach { checklistItem ->
                    item(key = checklistItem.id) {
                        ChecklistRow(
                            item = checklistItem,
                            onToggle = { checked -> viewModel.setItemChecked(checklistItem.id, checked) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(categoryName: String) {
    val display = runCatching { SoftAspectCategory.valueOf(categoryName).displayName }
        .getOrDefault(categoryName)
    Text(
        text = display,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun ChecklistRow(item: ChecklistItemEntity, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle(!item.isChecked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = onToggle)
        Text(item.label, style = MaterialTheme.typography.bodyLarge)
    }
}
