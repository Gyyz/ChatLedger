package com.chatledger.data.dao

import androidx.room.*
import com.chatledger.data.entity.Expense
import com.chatledger.data.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: ExpenseCategory,
    val total: Double,
    val count: Int
)

data class DailyTotal(
    val date: String,   // yyyy-MM-dd
    val total: Double
)

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getExpensesBetween(start: Long, end: Long): Flow<List<Expense>>

    @Query("""
        SELECT category, SUM(amount) as total, COUNT(*) as count
        FROM expenses
        WHERE timestamp BETWEEN :start AND :end AND isIncome = 0
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT SUM(amount) FROM expenses
        WHERE timestamp BETWEEN :start AND :end AND isIncome = 0
    """)
    fun getTotalExpense(start: Long, end: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM expenses
        WHERE timestamp BETWEEN :start AND :end AND isIncome = 1
    """)
    fun getTotalIncome(start: Long, end: Long): Flow<Double?>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int = 10): Flow<List<Expense>>
}
