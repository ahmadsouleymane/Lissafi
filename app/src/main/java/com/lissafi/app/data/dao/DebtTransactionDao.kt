package com.lissafi.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lissafi.app.data.entity.DebtTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtTransactionDao {
    @Insert
    suspend fun insert(transaction: DebtTransaction)

    @Query("SELECT * FROM debt_transactions WHERE client_id = :clientId ORDER BY date DESC")
    fun getForClient(clientId: String): Flow<List<DebtTransaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debt_transactions WHERE client_id = :clientId")
    suspend fun getTotalDebt(clientId: String): Int

    @Query("SELECT MAX(date) FROM debt_transactions WHERE client_id = :clientId")
    suspend fun getLastTransactionDate(clientId: String): Long?
}
