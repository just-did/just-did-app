package com.zhouyp.justdid.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.zhouyp.justdid.domain.model.DisplayRow
import com.zhouyp.justdid.domain.model.RecordSource
import com.zhouyp.justdid.domain.model.UpdatedIndexEntry
import com.zhouyp.justdid.ui.components.ConnectionIndicator
import com.zhouyp.justdid.ui.navigation.Route
import com.zhouyp.justdid.ui.qrcode.CustomScannerActivity
import java.time.LocalDate
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

private data class DiscardPrompt(val batchId: String, val message: String)

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

    val context = LocalContext.current
    var successEntries by remember { mutableStateOf<List<UpdatedIndexEntry>?>(null) }
    var discardPrompt by remember { mutableStateOf<DiscardPrompt?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.pushUiEvents.collect { event ->
            when (event) {
                is PushUiEvent.Toast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is PushUiEvent.SuccessDialog -> successEntries = event.entries
                is PushUiEvent.DiscardDialog -> discardPrompt = DiscardPrompt(event.batchId, event.message)
            }
        }
    }

    val listState = rememberLazyListState()

    // 记录后（或当前阅读日期消失后）跳回今天（reverseLayout 下索引 0 即底部=今天）
    LaunchedEffect(uiState.scrollToTodayTick) {
        if (uiState.scrollToTodayTick > 0 && uiState.contentDates.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // 滚动锚点上报（窗口缓存中心）；仅在用户滚动时触发，初始锚点由 ViewModel 定为今天；
    // 日期与索引在 snapshotFlow 内原子捕获；按视口行进方向预取前方相邻日期
    LaunchedEffect(listState) {
        var previousIndex: Int? = null
        snapshotFlow {
            val index = listState.firstVisibleItemIndex
            val scrolling = listState.isScrollInProgress
            Triple(index, scrolling, uiState.displayRows.getOrNull(index)?.date)
        }
            .distinctUntilChanged()
            .collect { (index, scrolling, date) ->
                when {
                    scrolling -> {
                        date?.let { viewModel.onAnchorChanged(it) }
                        val prev = previousIndex
                        if (prev != null && index != prev) {
                            // 索引增大=向更早方向行进（预取更早），索引减小=向今天方向行进（预取更新）
                            viewModel.prefetch(if (index > prev) 1 else -1)
                        }
                        previousIndex = index
                    }
                    else -> {
                        // 滚动停止：仅淘汰不可见且超出窗口的日期，避免可见行被重建导致视口跳变
                        val visibleDates = listState.layoutInfo.visibleItemsInfo
                            .mapNotNull { info -> uiState.displayRows.getOrNull(info.index)?.date }
                            .toSet()
                        viewModel.evictWindow(visibleDates)
                    }
                }
            }
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
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = { viewModel.pushStaging() },
                enabled = uiState.isConnected && !uiState.isPushing,
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = ButtonDefaults.outlinedShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF008CFF)
                )
            ) {
                Text(
                    text = if (uiState.isPushing) "推送中" else "推送",
                    fontSize = 13.sp
                )
            }
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

        // 记录列表区（填充剩余空间）；reverseLayout 底部锚定：今天在底部，历史在上方
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f)
        ) {
            items(uiState.displayRows, key = { it.key }) { row ->
                when (row) {
                    is DisplayRow.DayHeader -> DayHeaderItem(row)
                    is DisplayRow.Placeholder ->
                        PlaceholderItem(row, modifier = Modifier.fillParentMaxHeight())
                    is DisplayRow.TimeHeader -> TimeHeaderItem(row)
                    is DisplayRow.ContentLine -> ContentLineItem(row)
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

    successEntries?.let { entries ->
        val dates = entries.map { LocalDate.of(it.year, it.month, it.day) }
        AlertDialog(
            onDismissRequest = { successEntries = null },
            title = { Text("同步成功") },
            text = {
                Column {
                    Text("是否拉取更新后的日报？")
                    dates.forEach { date ->
                        Text(
                            text = date.toString(),
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        successEntries = null
                        viewModel.fetchDailyReports(dates)
                    }
                ) {
                    Text("拉取")
                }
            },
            dismissButton = {
                TextButton(onClick = { successEntries = null }) {
                    Text("不拉取")
                }
            }
        )
    }

    discardPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { discardPrompt = null },
            title = { Text("批数据错误") },
            text = { Text(prompt.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.discardBatch(prompt.batchId)
                        discardPrompt = null
                    }
                ) {
                    Text("丢弃")
                }
            },
            dismissButton = {
                TextButton(onClick = { discardPrompt = null }) {
                    Text("保留")
                }
            }
        )
    }
}

@Composable
private fun DayHeaderItem(row: DisplayRow.DayHeader) {
    Column {
        Text(
            text = dayHeaderText(row.date),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (row.isEmptyToday) {
            Text(
                text = "今天还没有记录",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PlaceholderItem(row: DisplayRow.Placeholder, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = dayHeaderText(row.date),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun TimeHeaderItem(row: DisplayRow.TimeHeader) {
    val isStaging = row.source == RecordSource.STAGING
    val accent = Color(0xFF008CFF)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = row.time,
            fontSize = 14.sp,
            color = if (isStaging) accent else Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (isStaging) {
            Text(
                text = "未同步",
                fontSize = 10.sp,
                color = accent,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun ContentLineItem(row: DisplayRow.ContentLine) {
    val isStaging = row.source == RecordSource.STAGING
    val accent = Color(0xFF008CFF)
    Text(
        text = row.text,
        fontSize = 16.sp,
        color = if (isStaging) accent else Color.Unspecified,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

private fun dayHeaderText(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> if (date.year == today.year) {
            "${date.monthValue}月${date.dayOfMonth}日"
        } else {
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
        }
    }
}
