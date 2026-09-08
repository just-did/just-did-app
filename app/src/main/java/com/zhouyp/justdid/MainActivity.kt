package com.zhouyp.justdid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.zhouyp.justdid.domain.repository.DailyReportRepository
import com.zhouyp.justdid.ui.navigation.AppNavHost
import com.zhouyp.justdid.ui.privacy.PrivacyConsentGate
import com.zhouyp.justdid.ui.theme.JustDidTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dailyReportRepository: DailyReportRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JustDidTheme {
                var cacheUsageRefreshed by remember { mutableStateOf(false) }
                PrivacyConsentGate {
                    LaunchedEffect(Unit) {
                        if (!cacheUsageRefreshed) {
                            cacheUsageRefreshed = true
                            dailyReportRepository.refreshCacheUsage()
                        }
                    }
                    AppNavHost()
                }
            }
        }
    }
}
