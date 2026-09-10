package com.zhouyp.justdid.ui.privacy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PrivacyConsentGate(
    viewModel: PrivacyConsentViewModel = hiltViewModel(),
    onDecline: () -> Unit,
    acceptedContent: @Composable () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val openPolicy = { showPrivacyPolicy = true }
    BackHandler(enabled = uiState != PrivacyConsentUiState.Accepted) {}

    when (uiState) {
        PrivacyConsentUiState.Loading -> LoadingScreen()
        PrivacyConsentUiState.Required -> PrivacyConsentDialog(
            onViewPolicy = openPolicy,
            onDecline = onDecline,
            onAccept = viewModel::accept
        )
        PrivacyConsentUiState.Accepted -> acceptedContent()
    }
    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun PrivacyConsentDialog(
    onViewPolicy: () -> Unit,
    onDecline: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("欢迎使用 JustDid") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(LOCAL_PRIVACY_SUMMARY)
                TextButton(onClick = onViewPolicy) {
                    Text("《隐私政策》", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("同意并继续")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("不同意并退出")
            }
        }
    )
}
