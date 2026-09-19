package com.kula.anagram.ui

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kula.anagram.R
import com.kula.anagram.core.SavedAnagram

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnagramScreen(viewModel: AnagramViewModel = viewModel()) {
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Input + load
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = viewModel.input,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.input_label)) },
                    placeholder = { Text(stringResource(R.string.input_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.loadLetters()
                        keyboard?.hide()
                    }),
                )
                if (viewModel.input.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearBoard) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.loadLetters()
                    keyboard?.hide()
                },
                enabled = viewModel.input.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_load))
            }

            Spacer(Modifier.height(16.dp))

            if (!viewModel.hasBoard) {
                EmptyBoard()
            } else {
                CurrentWordHeader(
                    word = viewModel.currentWord,
                    status = when {
                        viewModel.isAnagram -> Status.ANAGRAM
                        viewModel.ablage.isNotEmpty() -> Status.INCOMPLETE
                        else -> Status.ORIGINAL
                    },
                )
                Spacer(Modifier.height(12.dp))
                WorkBoard(
                    werkbank = viewModel.werkbank,
                    ablage = viewModel.ablage,
                    onDrop = viewModel::drop,
                    onTap = viewModel::tapCell,
                    onLongPress = viewModel::longPressCell,
                    onAddSpace = viewModel::addSpace,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = viewModel::shuffle, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_shuffle))
                    }
                    OutlinedButton(onClick = viewModel::reset, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_reset))
                    }
                    Button(
                        onClick = viewModel::saveCurrent,
                        enabled = viewModel.canSave,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.saved_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            SavedList(
                items = viewModel.saved,
                onDelete = viewModel::deleteSaved,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

private enum class Status { ORIGINAL, INCOMPLETE, ANAGRAM }

@Composable
private fun CurrentWordHeader(word: String, status: Status) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = word.ifBlank { " " },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    when (status) {
                        Status.ANAGRAM -> R.string.badge_anagram
                        Status.INCOMPLETE -> R.string.badge_incomplete
                        Status.ORIGINAL -> R.string.badge_original
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EmptyBoard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = stringResource(R.string.empty_hint),
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SavedList(
    items: List<SavedAnagram>,
    onDelete: (SavedAnagram) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Text(
            text = stringResource(R.string.saved_empty),
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        items(items = items, key = { it.createdAt.toString() + it.text }) { item ->
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.from_source, item.source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onDelete(item) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }
}
