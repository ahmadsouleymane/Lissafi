package com.lissafi.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.entity.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Insert
    suspend fun insertSaleItem(item: SaleItem)

    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    suspend fun getSalesBetween(startDate: Long, endDate: Long): List<Sale>

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Query("""
        SELECT si.name, SUM(si.quantity) as total_qty, COUNT(DISTINCT si.sale_id) as count
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        WHERE s.date >= :startDate AND s.date < :endDate
        GROUP BY si.name
        ORDER BY total_qty DESC
    """)
    suspend fun getTopProducts(startDate: Long, endDate: Long): List<TopProduct>

    @Query("SELECT COUNT(*) FROM sales WHERE date >= :startDate AND date < :endDate")
    suspend fun countSalesBetween(startDate: Long, endDate: Long): Int

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date >= :startDate AND date < :endDate")
    suspend fun sumTotalBetween(startDate: Long, endDate: Long): Int

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date >= :startDate AND date < :endDate AND is_credit = 1")
    suspend fun sumCreditBetween(startDate: Long, endDate: Long): Int
}

data class TopProduct(
    val name: String,
    val total_qty: Double,
    val count: Int
)
