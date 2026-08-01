package com.zhouyp.justdid.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.zhouyp.justdid.ui.components.ConnectionIndicator
import com.zhouyp.justdid.ui.navigation.Route
import com.zhouyp.justdid.ui.qrcode.CustomScannerActivity

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrEmpty()) {
            viewModel.connectToMaster(contents)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose { lifecycleOwner.lifecycle.removeObserver(viewModel) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 紧凑标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 36.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionIndicator(
                isConnected = uiState.isConnected,
                onDisconnectedClick = {
                    scanLauncher.launch(ScanOptions().apply {
                        setCaptureActivity(CustomScannerActivity::class.java)
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("扫描电脑端二维码连接")
                        setBeepEnabled(false)
                        setOrientationLocked(true)
                    })
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Just Did",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { navController.navigate(Route.Settings.route) },
                modifier = Modifier.padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置"
                )
            }
        }

        // 记录列表区（填充剩余空间）
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f)
        ) {
            items(uiState.todayRecords) { group ->
                Text(
                    text = group.time,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
                group.contents.forEach { content ->
                    Text(
                        text = content,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 输入区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "刚刚做了什么？",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = viewModel::onInputTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                placeholder = { Text("") },
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.record() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = ButtonDefaults.outlinedShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF008CFF)
                )
            ) {
                Text(
                    text = "记录",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
