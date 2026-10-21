package com.kula.nextquest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kula.nextquest.domain.Activity
import com.kula.nextquest.ui.components.scanlines
import com.kula.nextquest.ui.theme.Retro

/** The three states of the flow. Kept as plain hoisted state — no nav library needed. */
private sealed interface Screen {
    data object Home : Screen
    data object Picker : Screen
    data class Reading(val activity: Activity, val icon: String) : Screen
}

@Composable
fun NextQuestApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = screen !is Screen.Home) {
        screen = when (screen) {
            is Screen.Reading -> Screen.Picker
            else -> Screen.Home
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Retro.Background)
            .scanlines(),
    ) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(onStart = { screen = Screen.Picker })
            Screen.Picker -> PickerScreen(
                onPick = { activity, icon -> screen = Screen.Reading(activity, icon) },
                onBack = { screen = Screen.Home },
            )
            is Screen.Reading -> ReadingScreen(
                activity = s.activity,
                icon = s.icon,
                onNewQuest = { screen = Screen.Picker },
            )
        }
    }
}
