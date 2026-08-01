package com.zhouyp.justdid.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.zhouyp.justdid.ui.components.ConnectionIndicator
import com.zhouyp.justdid.ui.qrcode.CustomScannerActivity
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrEmpty()) {
            viewModel.connectToMaster(contents)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp) // 底部间距增加2倍
    ) {
        // 紧凑标题栏（顶部间距增加2倍：12dp → 24dp）
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
                text = "管理",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Box {
                IconButton(
                    onClick = { viewModel.toggleDropdownMenu(true) },
                    modifier = Modifier.padding(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多选项"
                    )
                }
                DropdownMenu(
                    expanded = uiState.showDropdownMenu,
                    onDismissRequest = { viewModel.toggleDropdownMenu(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("清除一周以前") },
                        onClick = { viewModel.toggleDropdownMenu(false) }
                    )
                    DropdownMenuItem(
                        text = { Text("清除一月以前") },
                        onClick = { viewModel.toggleDropdownMenu(false) }
                    )
                    DropdownMenuItem(
                        text = { Text("清除一年以前") },
                        onClick = { viewModel.toggleDropdownMenu(false) }
                    )
                }
            }
        }

        // 饼图区域
        Spacer(modifier = Modifier.height(12.dp))
        PieChartView(usedPercent = uiState.storageUsedPercent)

        Spacer(modifier = Modifier.height(18.dp))

        // 日历容器（带边框，模拟原型中的卡片样式），内含模式切换
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            // 日历顶部：模式切换 + 月份导航
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeToggle(
                    currentMode = uiState.mode,
                    onModeChange = { mode -> viewModel.toggleMode(mode) },
                    modifier = Modifier.weight(1f)
                )

                // 全选复选框
                SelectAllCheckbox(
                    currentMonth = uiState.currentMonth,
                    selectedDates = uiState.selectedDates,
                    onToggle = { viewModel.toggleSelectAllInMonth() }
                )

                // 月份导航
                IconButton(
                    onClick = { viewModel.navigateMonth(-1) },
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text("<", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${uiState.currentMonth.year}年${uiState.currentMonth.monthValue}月",
                    fontSize = 14.sp
                )
                IconButton(
                    onClick = { viewModel.navigateMonth(1) },
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(">", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 日期网格
            CalendarGrid(
                currentMonth = uiState.currentMonth,
                selectedDates = uiState.selectedDates,
                onDateClick = { date -> viewModel.selectDate(date) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮
        val buttonText = when (uiState.mode) {
            CalendarMode.CLEAR -> "清空已选"
            CalendarMode.FETCH -> "拉取已选"
        }
        Button(
            onClick = { /* 暂不实现业务逻辑 */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(44.dp),
            shape = ButtonDefaults.outlinedShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF008CFF)
            )
        ) {
            Text(
                text = buttonText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModeToggle(
    currentMode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // 蓝色圆点指示器
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF0065FF))
                .align(Alignment.CenterVertically)
        )

        Text(
            text = "清 除",
            fontSize = 11.sp,
            color = if (currentMode == CalendarMode.CLEAR)
                MaterialTheme.colorScheme.primary
            else
                Color.Gray,
            fontWeight = if (currentMode == CalendarMode.CLEAR)
                FontWeight.Bold
            else
                FontWeight.Normal,
            modifier = Modifier.clickable { onModeChange(CalendarMode.CLEAR) }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "拉 取",
            fontSize = 11.sp,
            color = if (currentMode == CalendarMode.FETCH)
                MaterialTheme.colorScheme.primary
            else
                Color.Gray,
            fontWeight = if (currentMode == CalendarMode.FETCH)
                FontWeight.Bold
            else
                FontWeight.Normal,
            modifier = Modifier.clickable { onModeChange(CalendarMode.FETCH) }
        )
    }
}

@Composable
private fun SelectAllCheckbox(
    currentMonth: YearMonth,
    selectedDates: Set<LocalDate>,
    onToggle: () -> Unit
) {
    val allDatesInMonth = (1..currentMonth.lengthOfMonth())
        .map { currentMonth.atDay(it) }
        .toSet()
    val selectedInMonth = selectedDates intersect allDatesInMonth

    val (icon, description) = when {
        selectedInMonth.isEmpty() ->
            Icons.Outlined.CheckBoxOutlineBlank to "全选"
        selectedInMonth.size == allDatesInMonth.size ->
            Icons.Filled.CheckBox to "取消全选"
        else ->
            Icons.Filled.IndeterminateCheckBox to "取消选择"
    }

    IconButton(
        onClick = onToggle,
        modifier = Modifier.padding(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp)
        )
    }
}
