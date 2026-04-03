package com.alfoll.aiassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alfoll.aiassistant.ui.navigation.AppNavGraph
import com.alfoll.aiassistant.ui.theme.Ai_assistant_avito_techTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ai_assistant_avito_techTheme {
                AppNavGraph()
            }
        }
    }
}
