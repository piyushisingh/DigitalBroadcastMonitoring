package com.example.digitalbroadcastmonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.digitalbroadcastmonitoring.ui.screens.MainScreen
import com.example.digitalbroadcastmonitoring.ui.theme.DigitalBroadcastMonitoringTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalBroadcastMonitoringTheme {
                MainScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    DigitalBroadcastMonitoringTheme {
        MainScreen()
    }
}