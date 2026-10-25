package com.packready.shopkeeper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.packready.shopkeeper.data.local.dao.EmbeddingDao
import com.packready.shopkeeper.data.local.dao.InvoiceDao
import com.packready.shopkeeper.data.local.dao.KhataDao
import com.packready.shopkeeper.data.local.dao.OutboxDao
import com.packready.shopkeeper.data.local.dao.ProductDao
import com.packready.shopkeeper.data.local.dao.SaleDao
import com.packready.shopkeeper.data.local.entity.InvoiceDocumentEntity
import com.packready.shopkeeper.data.local.entity.InvoiceLineItemEntity
import com.packready.shopkeeper.data.local.entity.KhataCustomerEntity
import com.packready.shopkeeper.data.local.entity.KhataTransactionEntity
import com.packready.shopkeeper.data.local.entity.ProductEmbeddingEntity
import com.packready.shopkeeper.data.local.entity.ProductEntity
import com.packready.shopkeeper.data.local.entity.ProductFtsEntity
import com.packready.shopkeeper.data.local.entity.SaleItemEntity
import com.packready.shopkeeper.data.local.entity.SaleTransactionEntity
import com.packready.shopkeeper.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        ProductFtsEntity::class,
        SaleTransactionEntity::class,
        SaleItemEntity::class,
        SyncOutboxEntity::class,
        KhataCustomerEntity::class,
        KhataTransactionEntity::class,
        InvoiceDocumentEntity::class,
        InvoiceLineItemEntity::class,
        ProductEmbeddingEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun saleDao(): SaleDao
    abstract fun khataDao(): KhataDao
    abstract fun outboxDao(): OutboxDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        const val DATABASE_NAME = "packready_shopkeeper.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun buildDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
