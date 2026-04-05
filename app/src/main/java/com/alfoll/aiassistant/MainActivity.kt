package com.alfoll.aiassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.alfoll.aiassistant.data.local.ThemePreferenceStorage
import com.alfoll.aiassistant.ui.navigation.AppNavGraph
import com.alfoll.aiassistant.ui.theme.Ai_assistant_avito_techTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeStorage = remember { ThemePreferenceStorage(applicationContext) }
            val scope = rememberCoroutineScope()

            val isDarkTheme by themeStorage.isDarkThemeFlow.collectAsState(initial = null)

            if (isDarkTheme == null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                Ai_assistant_avito_techTheme(
                    darkTheme = isDarkTheme == true
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavGraph(
                            isDarkTheme = isDarkTheme == true,
                            onThemeChange = { newValue ->
                                scope.launch {
                                    themeStorage.setDarkTheme(newValue)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}