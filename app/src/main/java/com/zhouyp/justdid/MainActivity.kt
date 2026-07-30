package com.zhouyp.justdid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhouyp.justdid.ui.navigation.AppNavHost
import com.zhouyp.justdid.ui.theme.JustDidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JustDidTheme {
                AppNavHost()
            }
        }
    }
}
