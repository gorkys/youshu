package com.youshu.app.data.local.entity

import androidx.room.Embedded

data class ItemDetail(
    @Embedded val item: Item,
    val categoryName: String? = null,
    val locationName: String? = null
)
