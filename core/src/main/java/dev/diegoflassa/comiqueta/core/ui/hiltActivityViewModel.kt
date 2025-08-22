package dev.diegoflassa.comiqueta.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel

@Composable
inline fun <reified T : ViewModel> hiltActivityViewModel(): T {
    val context = LocalContext.current
    return hiltViewModel(context as ComponentActivity)
}
