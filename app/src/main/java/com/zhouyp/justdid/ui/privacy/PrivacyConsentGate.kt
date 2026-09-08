package com.zhouyp.justdid.ui.privacy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PrivacyConsentGate(
    viewModel: PrivacyConsentViewModel = hiltViewModel(),
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
            onDecline = viewModel::decline,
            onAccept = viewModel::accept
        )
        PrivacyConsentUiState.Declined -> DeclinedScreen(
            onViewPolicy = openPolicy,
            onChooseAgain = viewModel::chooseAgain
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
            Column {
                Text("感谢您使用 JustDid。我们重视并保护您的个人信息和隐私安全。")
                Text(
                    text = "在使用前，请阅读并充分理解《隐私政策》，了解我们如何处理您记录的工作内容，以及通过局域网与电脑端同步数据。",
                    modifier = Modifier.padding(top = 12.dp)
                )
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
                Text("不同意")
            }
        }
    )
}

@Composable
internal fun DeclinedScreen(
    onViewPolicy: () -> Unit,
    onChooseAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "您尚未同意隐私政策，因此暂时无法使用 JustDid。您可以阅读隐私政策后重新选择。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        TextButton(
            onClick = onViewPolicy,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("查看隐私政策")
        }
        Button(onClick = onChooseAgain) {
            Text("重新选择")
        }
    }
}
