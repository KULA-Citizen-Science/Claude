package com.kula.stylusnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kula.stylusnotes.data.db.AppDatabase
import com.kula.stylusnotes.data.repository.NoteRepository
import com.kula.stylusnotes.ui.StylusNotesNavHost
import com.kula.stylusnotes.ui.theme.StylusNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = NoteRepository(AppDatabase.getInstance(applicationContext).noteDao())

        setContent {
            StylusNotesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StylusNotesNavHost(repository = repository)
                }
            }
        }
    }
}
