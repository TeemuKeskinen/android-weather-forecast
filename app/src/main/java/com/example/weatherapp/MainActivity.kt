package com.example.weatherapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.weatherapp.data.DataStoreManager
import com.example.weatherapp.ui.WeatherAppWithDrawer
import com.example.weatherapp.ui.theme.WeatherAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            var darkTheme by remember { mutableStateOf(false) }

            // Load persisted theme once
            LaunchedEffect(Unit) {
                val saved = DataStoreManager.loadDarkTheme(this@MainActivity)
                darkTheme = saved
            }

            // Remember a callback that saves the preference when toggled (receives desired state)
            val onToggleThemeCallback: (Boolean) -> Unit = { desired ->
                scope.launch {
                    darkTheme = desired
                    DataStoreManager.saveDarkTheme(this@MainActivity, desired)
                }
            }

            WeatherAppTheme(darkTheme = darkTheme) {
                WeatherAppWithDrawer(
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleThemeCallback
                )
            }
        }
    }
}
