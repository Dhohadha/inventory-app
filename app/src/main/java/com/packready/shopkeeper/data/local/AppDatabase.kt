package com.packready.shopkeeper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.packready.shopkeeper.data.local.dao.InvoiceDao
import com.packready.shopkeeper.data.local.dao.KhataDao
import com.packready.shopkeeper.data.local.dao.OutboxDao
import com.packready.shopkeeper.data.local.dao.ProductDao
import com.packready.shopkeeper.data.local.dao.SaleDao
import com.packready.shopkeeper.data.local.entity.InvoiceDocumentEntity
import com.packready.shopkeeper.data.local.entity.InvoiceLineItemEntity
import com.packready.shopkeeper.data.local.entity.KhataCustomerEntity
import com.packready.shopkeeper.data.local.entity.KhataTransactionEntity
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
        InvoiceLineItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
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
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with Indian Kirana Store Catalog
                        scope.launch(Dispatchers.IO) {
                            val database = INSTANCE ?: return@launch
                            val entities = InitialCatalogData.sampleProducts.map {
                                ProductEntity.fromDomain(it)
                            }
                            database.productDao().insertAll(entities)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
