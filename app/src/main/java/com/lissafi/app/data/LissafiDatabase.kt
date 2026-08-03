package com.lissafi.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lissafi.app.data.dao.AppSettingDao
import com.lissafi.app.data.dao.ClientDao
import com.lissafi.app.data.dao.DebtTransactionDao
import com.lissafi.app.data.dao.ProductDao
import com.lissafi.app.data.dao.SaleDao
import com.lissafi.app.data.entity.AppSetting
import com.lissafi.app.data.entity.Client
import com.lissafi.app.data.entity.DebtTransaction
import com.lissafi.app.data.entity.Product
import com.lissafi.app.data.entity.Sale
import com.lissafi.app.data.entity.SaleItem

@Database(
    entities = [
        Product::class,
        Sale::class,
        SaleItem::class,
        Client::class,
        DebtTransaction::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LissafiDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun clientDao(): ClientDao
    abstract fun debtTransactionDao(): DebtTransactionDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: LissafiDatabase? = null

        fun getInstance(context: Context): LissafiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LissafiDatabase::class.java,
                    "lissafi.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
