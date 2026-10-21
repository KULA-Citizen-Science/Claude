package com.kula.nextquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kula.nextquest.ui.NextQuestApp
import com.kula.nextquest.ui.theme.NextQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextQuestTheme {
                NextQuestApp()
            }
        }
    }
}
