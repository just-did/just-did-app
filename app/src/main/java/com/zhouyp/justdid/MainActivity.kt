package com.zhouyp.justdid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.zhouyp.justdid.domain.repository.DailyReportRepository
import com.zhouyp.justdid.ui.navigation.AppNavHost
import com.zhouyp.justdid.ui.theme.JustDidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dailyReportRepository: DailyReportRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) {
            dailyReportRepository.refreshCacheUsage()
        }
        setContent {
            JustDidTheme {
                AppNavHost()
            }
        }
    }
}
