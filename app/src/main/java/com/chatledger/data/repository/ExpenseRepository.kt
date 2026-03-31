package com.chatledger.data.repository

import com.chatledger.data.dao.CategoryTotal
import com.chatledger.data.dao.ExpenseDao
import com.chatledger.data.entity.Expense
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(private val dao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()

    suspend fun insert(expense: Expense): Long = dao.insert(expense)

    suspend fun update(expense: Expense) = dao.update(expense)

    suspend fun delete(expense: Expense) = dao.delete(expense)

    suspend fun getById(id: Long): Expense? = dao.getById(id)

    fun getRecentExpenses(limit: Int = 10): Flow<List<Expense>> = dao.getRecentExpenses(limit)

    fun getExpensesBetween(start: Long, end: Long): Flow<List<Expense>> =
        dao.getExpensesBetween(start, end)

    fun getCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>> =
        dao.getCategoryTotals(start, end)

    fun getTotalExpense(start: Long, end: Long): Flow<Double?> =
        dao.getTotalExpense(start, end)

    fun getTotalIncome(start: Long, end: Long): Flow<Double?> =
        dao.getTotalIncome(start, end)

    // --- 便捷方法 ---

    fun getThisWeekExpenses(): Flow<List<Expense>> {
        val (start, end) = getWeekRange()
        return getExpensesBetween(start, end)
    }

    fun getThisMonthExpenses(): Flow<List<Expense>> {
        val (start, end) = getMonthRange()
        return getExpensesBetween(start, end)
    }

    fun getThisWeekCategoryTotals(): Flow<List<CategoryTotal>> {
        val (start, end) = getWeekRange()
        return getCategoryTotals(start, end)
    }

    fun getThisMonthCategoryTotals(): Flow<List<CategoryTotal>> {
        val (start, end) = getMonthRange()
        return getCategoryTotals(start, end)
    }

    fun getThisWeekTotal(): Flow<Double?> {
        val (start, end) = getWeekRange()
        return getTotalExpense(start, end)
    }

    fun getThisMonthTotal(): Flow<Double?> {
        val (start, end) = getMonthRange()
        return getTotalExpense(start, end)
    }

    companion object {
        fun getWeekRange(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            val end = cal.timeInMillis
            return start to end
        }

        fun getMonthRange(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            return start to end
        }
    }
}
