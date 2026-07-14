package com.kula.stylusnotes.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kula.stylusnotes.data.repository.NoteRepository
import com.kula.stylusnotes.ui.editor.EditorScreen
import com.kula.stylusnotes.ui.editor.EditorViewModel
import com.kula.stylusnotes.ui.notelist.NoteListScreen
import com.kula.stylusnotes.ui.notelist.NoteListViewModel

private const val ROUTE_NOTE_LIST = "notes"
private const val ROUTE_EDITOR = "editor/{noteId}"

@Composable
fun StylusNotesNavHost(
    repository: NoteRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = ROUTE_NOTE_LIST) {
        composable(ROUTE_NOTE_LIST) {
            val viewModel: NoteListViewModel = viewModel(
                factory = viewModelFactory { initializer { NoteListViewModel(repository) } }
            )
            NoteListScreen(
                viewModel = viewModel,
                onOpenNote = { noteId -> navController.navigate("editor/$noteId") }
            )
        }
        composable(ROUTE_EDITOR) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
            val viewModel: EditorViewModel = viewModel(
                factory = viewModelFactory { initializer { EditorViewModel(repository) } }
            )
            EditorScreen(
                viewModel = viewModel,
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
