package com.youshu.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Location::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("locationId"), Index("status")]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantity: Int = 1,
    val unit: String = "个",
    val price: Double? = null,
    val expireTime: Long? = null,
    val status: Int = STATUS_IN_USE,
    val note: String = "",
    val imagePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_IN_USE = 0
        const val STATUS_USED_UP = 1
        const val STATUS_DISCARDED = 2
    }
}
