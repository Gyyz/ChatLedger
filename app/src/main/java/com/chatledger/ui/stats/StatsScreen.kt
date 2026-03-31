package com.chatledger.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatledger.data.dao.CategoryTotal
import com.chatledger.data.entity.Expense
import com.chatledger.data.entity.ExpenseCategory
import com.chatledger.ui.theme.CategoryColors
import com.chatledger.util.DateUtils
import com.chatledger.viewmodel.StatsTab
import com.chatledger.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val weekTotal by viewModel.weekTotal.collectAsState()
    val monthTotal by viewModel.monthTotal.collectAsState()
    val weekCategories by viewModel.weekCategoryTotals.collectAsState()
    val monthCategories by viewModel.monthCategoryTotals.collectAsState()
    val weekExpenses by viewModel.weekExpenses.collectAsState()
    val monthExpenses by viewModel.monthExpenses.collectAsState()

    val total = if (selectedTab == StatsTab.WEEK) weekTotal else monthTotal
    val categories = if (selectedTab == StatsTab.WEEK) weekCategories else monthCategories
    val expenses = if (selectedTab == StatsTab.WEEK) weekExpenses else monthExpenses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("统计报表", style = MaterialTheme.typography.titleLarge) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Tab 切换
        TabRow(
            selectedTabIndex = StatsTab.entries.indexOf(selectedTab)
        ) {
            StatsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = { Text(tab.title) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 总额卡片
            item {
                TotalCard(total = total, tab = selectedTab)
            }

            // 分类占比
            if (categories.isNotEmpty()) {
                item {
                    CategoryBreakdown(categories = categories, total = total)
                }
            }

            // 最近记录
            item {
                Text(
                    "明细记录",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无记录",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(expenses.filter { !it.isIncome }) { expense ->
                    ExpenseItem(expense)
                }
            }
        }
    }
}

@Composable
fun TotalCard(total: Double, tab: StatsTab) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${tab.title}总支出",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "¥${"%.2f".format(total)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CategoryBreakdown(categories: List<CategoryTotal>, total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "分类统计",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 横向比例条
            if (total > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    categories.forEach { cat ->
                        val fraction = (cat.total / total).toFloat().coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(fraction.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(
                                    CategoryColors[cat.category.name] ?: Color.Gray
                                )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 类别列表
            categories.forEach { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(CategoryColors[cat.category.name] ?: Color.Gray)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${cat.category.emoji} ${cat.category.displayName}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${cat.count}笔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "¥${"%.2f".format(cat.total)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (total > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${"%.0f".format(cat.total / total * 100)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类别图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        (CategoryColors[expense.category.name] ?: Color.Gray).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(expense.category.emoji, fontSize = 18.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                Row {
                    Text(
                        expense.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (expense.merchant != null) {
                        Text(
                            " · ${expense.merchant}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (expense.isIncome) "+¥${"%.2f".format(expense.amount)}"
                    else "-¥${"%.2f".format(expense.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (expense.isIncome) Color(0xFF00C853) else Color(0xFFFF5252)
                    )
                )
                Text(
                    DateUtils.formatSmartTime(expense.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
