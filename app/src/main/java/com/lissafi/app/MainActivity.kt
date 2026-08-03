package com.lissafi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.lissafi.app.ui.navigation.LissafiNavHost
import com.lissafi.app.ui.theme.LissafiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LissafiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LissafiNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
