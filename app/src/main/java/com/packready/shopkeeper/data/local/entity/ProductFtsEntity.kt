package com.packready.shopkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Full-Text Search virtual table to power sub-10ms Kirana catalog search queries
 */
@Entity(tableName = "products_fts")
@Fts4(contentEntity = ProductEntity::class)
data class ProductFtsEntity(
    @PrimaryKey val rowid: Int,
    val barcode: String,
    val name: String,
    val brand: String,
    val category: String,
    val subCategory: String
)
