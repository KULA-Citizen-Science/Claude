package com.kula.stylusnotes.ui.editor

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kula.stylusnotes.R
import com.kula.stylusnotes.core.color.InkColor
import com.kula.stylusnotes.core.color.InkPalette
import com.kula.stylusnotes.core.model.CanvasBackground
import com.kula.stylusnotes.export.NoteExporter
import com.kula.stylusnotes.ui.RenameNoteDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    noteId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var eraseMode by remember { mutableStateOf(false) }
    var currentColor by remember { mutableStateOf<InkColor>(InkColor.Adaptive) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val canvasView = remember {
        InkCanvasView(context).apply {
            onStrokesChanged = { viewModel.onStrokesChanged(it) }
            onUndoRedoStateChanged = { undo, redo -> canUndo = undo; canRedo = redo }
        }
    }

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    LaunchedEffect(uiState.isLoaded) {
        if (uiState.isLoaded) {
            canvasView.loadNote(uiState.strokes, uiState.background)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.saveNow() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = uiState.isLoaded) {
                            showRenameDialog = true
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
                actions = {
                    IconButton(onClick = { canvasView.undo() }, enabled = canUndo) {
                        Text("↶")
                    }
                    IconButton(onClick = { canvasView.redo() }, enabled = canRedo) {
                        Text("↷")
                    }
                    // Manual eraser toggle: a passive stylus (Moto G Stylus 2024/2025) has no
                    // barrel button or eraser tip, so this is its only way to erase.
                    IconButton(onClick = {
                        eraseMode = !eraseMode
                        canvasView.manualEraseMode = eraseMode
                    }) {
                        Text(
                            text = "⌫",
                            color = if (eraseMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            },
                            fontWeight = if (eraseMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    IconButton(onClick = {
                        val newBackground = uiState.background.toggled()
                        canvasView.canvasBackground = newBackground
                        viewModel.onBackgroundToggled(newBackground)
                    }) {
                        BackgroundToggleSwatch(background = uiState.background)
                    }
                    Box {
                        IconButton(onClick = { showColorPicker = true }) {
                            InkColorSwatch(color = currentColor)
                        }
                        ColorPickerMenu(
                            expanded = showColorPicker,
                            selected = currentColor,
                            onDismiss = { showColorPicker = false },
                            onColorSelected = { color ->
                                currentColor = color
                                canvasView.currentInkColor = color
                                showColorPicker = false
                            }
                        )
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) { Text("⇪") }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_as_png)) },
                                onClick = {
                                    showExportMenu = false
                                    exportAndShare(context, scope, canvasView, uiState.title, asPdf = false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_as_pdf)) },
                                onClick = {
                                    showExportMenu = false
                                    exportAndShare(context, scope, canvasView, uiState.title, asPdf = true)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            factory = { canvasView },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }

    if (showRenameDialog) {
        RenameNoteDialog(
            currentTitle = uiState.title,
            onConfirm = { newTitle ->
                viewModel.rename(newTitle)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}

@Composable
private fun InkColorSwatch(color: InkColor, size: Dp = 22.dp) {
    val fixedArgb = (color as? InkColor.Fixed)?.argb
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = if (fixedArgb != null) Color(fixedArgb) else Color.Transparent,
                shape = CircleShape
            )
            .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    )
}

@Composable
private fun BackgroundToggleSwatch(background: CanvasBackground) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(Color(background.argb), CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    )
}

@Composable
private fun ColorPickerMenu(
    expanded: Boolean,
    selected: InkColor,
    onDismiss: () -> Unit,
    onColorSelected: (InkColor) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        for (color in InkPalette.all) {
            val label = when (color) {
                InkColor.Adaptive -> stringResource(R.string.ink_color_adaptive)
                is InkColor.Fixed -> color.label
            }
            DropdownMenuItem(
                text = {
                    Row {
                        InkColorSwatch(color = color, size = 18.dp)
                        Text(
                            text = "  $label",
                            fontWeight = if (color == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                onClick = { onColorSelected(color) }
            )
        }
    }
}

private fun exportAndShare(
    context: Context,
    scope: CoroutineScope,
    canvasView: InkCanvasView,
    title: String,
    asPdf: Boolean
) {
    val width = canvasView.width
    val height = canvasView.height
    if (width <= 0 || height <= 0) {
        Toast.makeText(context, "Note isn't ready to export yet", Toast.LENGTH_SHORT).show()
        return
    }
    val strokes = canvasView.getStrokes()
    scope.launch {
        val uri = withContext(Dispatchers.IO) {
            if (asPdf) {
                NoteExporter.exportPdf(context, title, strokes, width, height)
            } else {
                NoteExporter.exportPng(context, title, strokes, width, height)
            }
        }
        val mimeType = if (asPdf) "application/pdf" else "image/png"
        context.startActivity(NoteExporter.buildShareIntent(uri, mimeType))
    }
}
