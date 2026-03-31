package com.chatledger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatledger.data.dao.CategoryTotal
import com.chatledger.data.database.AppDatabase
import com.chatledger.data.entity.Expense
import com.chatledger.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

enum class StatsTab(val title: String) {
    WEEK("本周"),
    MONTH("本月")
}

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val expenseRepo = ExpenseRepository(db.expenseDao())

    private val _selectedTab = MutableStateFlow(StatsTab.WEEK)
    val selectedTab: StateFlow<StatsTab> = _selectedTab

    // 本周数据
    val weekExpenses: StateFlow<List<Expense>> = expenseRepo.getThisWeekExpenses()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weekCategoryTotals: StateFlow<List<CategoryTotal>> = expenseRepo.getThisWeekCategoryTotals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weekTotal: StateFlow<Double> = expenseRepo.getThisWeekTotal()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 本月数据
    val monthExpenses: StateFlow<List<Expense>> = expenseRepo.getThisMonthExpenses()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val monthCategoryTotals: StateFlow<List<CategoryTotal>> = expenseRepo.getThisMonthCategoryTotals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val monthTotal: StateFlow<Double> = expenseRepo.getThisMonthTotal()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 每日趋势
    val dailyTrend: StateFlow<Map<Int, Double>> = run {
        val (start, end) = if (_selectedTab.value == StatsTab.WEEK) {
            ExpenseRepository.getWeekRange()
        } else {
            ExpenseRepository.getMonthRange()
        }
        expenseRepo.getExpensesBetween(start, end).map { expenses ->
            expenses.filter { !it.isIncome }
                .groupBy { expense ->
                    Calendar.getInstance().apply { timeInMillis = expense.timestamp }
                        .get(Calendar.DAY_OF_MONTH)
                }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    }

    fun selectTab(tab: StatsTab) {
        _selectedTab.value = tab
    }
}
