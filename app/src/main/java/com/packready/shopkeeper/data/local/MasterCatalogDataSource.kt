package com.packready.shopkeeper.data.local

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.packready.shopkeeper.domain.model.DynamicAttributes
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterCatalogDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mutex = Mutex()
    private var database: SQLiteDatabase? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val TAG = "MasterCatalogDataSource"
        private const val ASSET_NAME = "master_catalog.db"
        private const val DB_FILE_NAME = "master_catalog.db"
    }

    private suspend fun getReadableDatabase(): SQLiteDatabase? = mutex.withLock {
        if (database?.isOpen == true) {
            return database
        }

        withContext(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath(DB_FILE_NAME)
                ensureDatabaseExtracted(dbFile)

                if (dbFile.exists() && dbFile.length() > 0) {
                    database = SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    )
                    Log.i(TAG, "Master Catalog SQLite DB opened successfully (${dbFile.length()} bytes)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open Master Catalog Database", e)
            }
        }
        return database
    }

    private fun ensureDatabaseExtracted(targetFile: File) {
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        var assetSize = -1L
        try {
            context.assets.openFd(ASSET_NAME).use { fd ->
                assetSize = fd.length
            }
        } catch (_: Exception) {
            // Assets compressed in APK might not have openFd length
        }

        val needsExtraction = !targetFile.exists() || targetFile.length() == 0L ||
                (assetSize > 0 && targetFile.length() != assetSize)

        if (needsExtraction) {
            Log.i(TAG, "Extracting $ASSET_NAME to ${targetFile.absolutePath}...")
            context.assets.open(ASSET_NAME).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            Log.i(TAG, "Master catalog asset extracted: ${targetFile.length()} bytes")
        }
    }

    suspend fun lookupByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        val db = getReadableDatabase() ?: return@withContext null
        val trimmed = barcode.trim()

        val query = """
            SELECT id, barcode, name, brand, category, subCategory,
                   mrp, costPrice, sellingPrice, currentStock, minStockAlert,
                   unit, gstPercent, salesCount, attributesJson, lastRestockedAt, createdAt
            FROM master_products
            WHERE barcode = ? OR id = ?
            LIMIT 1
        """.trimIndent()

        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(query, arrayOf(trimmed, trimmed))
            if (cursor.moveToFirst()) {
                return@withContext parseProduct(cursor)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up barcode $trimmed in master catalog", e)
        } finally {
            cursor?.close()
        }
        return@withContext null
    }

    suspend fun searchMasterCatalog(query: String, limit: Int = 25): List<Product> = withContext(Dispatchers.IO) {
        val db = getReadableDatabase() ?: return@withContext emptyList()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val wildcard = "%$trimmed%"
        val sql = """
            SELECT id, barcode, name, brand, category, subCategory,
                   mrp, costPrice, sellingPrice, currentStock, minStockAlert,
                   unit, gstPercent, salesCount, attributesJson, lastRestockedAt, createdAt
            FROM master_products
            WHERE name LIKE ? OR brand LIKE ? OR barcode LIKE ? OR subCategory LIKE ?
            ORDER BY salesCount DESC
            LIMIT ?
        """.trimIndent()

        val results = mutableListOf<Product>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(sql, arrayOf(wildcard, wildcard, wildcard, wildcard, limit.toString()))
            while (cursor.moveToNext()) {
                results.add(parseProduct(cursor))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching master catalog for query: $trimmed", e)
        } finally {
            cursor?.close()
        }
        return@withContext results
    }

    suspend fun getMasterCatalogCount(): Int = withContext(Dispatchers.IO) {
        val db = getReadableDatabase() ?: return@withContext 0
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM master_products", null)
            if (cursor.moveToFirst()) {
                return@withContext cursor.getInt(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting master catalog count", e)
        } finally {
            cursor?.close()
        }
        return@withContext 0
    }

    private fun parseProduct(cursor: Cursor): Product {
        val id = cursor.getString(0)
        val barcode = cursor.getString(1)
        val name = cursor.getString(2)
        val brand = cursor.getString(3)
        val categoryStr = cursor.getString(4)
        val subCategory = cursor.getString(5)
        val mrp = cursor.getDouble(6)
        val costPrice = cursor.getDouble(7)
        val sellingPrice = cursor.getDouble(8)
        val currentStock = cursor.getDouble(9)
        val minStockAlert = cursor.getDouble(10)
        val unit = cursor.getString(11)
        val gstPercent = cursor.getDouble(12)
        val salesCount = cursor.getInt(13)
        val attributesJson = cursor.getString(14)
        val lastRestockedAt = cursor.getLong(15)
        val createdAt = cursor.getLong(16)

        val category = try {
            ProductCategory.valueOf(categoryStr)
        } catch (_: Exception) {
            ProductCategory.OTHER
        }

        val dynamicAttributes = attributesJson?.let {
            try {
                json.decodeFromString<DynamicAttributes>(it)
            } catch (_: Exception) {
                null
            }
        }

        return Product(
            id = id,
            barcode = barcode,
            name = name,
            brand = brand,
            category = category,
            subCategory = subCategory,
            mrp = mrp,
            costPrice = costPrice,
            sellingPrice = sellingPrice,
            currentStock = currentStock,
            minStockAlert = minStockAlert,
            unit = unit,
            gstPercent = gstPercent,
            salesCount = salesCount,
            attributes = dynamicAttributes,
            lastRestockedAt = if (lastRestockedAt > 0) lastRestockedAt else System.currentTimeMillis(),
            createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis()
        )
    }
}
