package com.youshu.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.youshu.app.data.local.dao.CategoryDao
import com.youshu.app.data.local.dao.ItemDao
import com.youshu.app.data.local.dao.LocationDao
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.Location

@Database(
    entities = [Item::class, Category::class, Location::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun buildDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "youshu.db"
                )
                    .addCallback(PrepopulateCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class PrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Insert default categories
            val categories = listOf("食品", "药品", "日用品", "电子产品", "衣物", "文具", "工具", "其他")
            categories.forEach { name ->
                db.execSQL("INSERT INTO categories (name, icon) VALUES ('$name', '')")
            }
            // Insert default locations (parent first, then children)
            val parentLocations = mapOf(
                "厨房" to listOf("冰箱", "橱柜", "台面"),
                "客厅" to listOf("电视柜", "茶几", "书架"),
                "卧室" to listOf("衣柜", "床头柜", "梳妆台"),
                "卫生间" to listOf("洗手台", "淋浴间"),
                "阳台" to listOf("储物柜"),
                "书房" to listOf("书桌", "书柜"),
                "玄关" to listOf("鞋柜")
            )
            parentLocations.forEach { (parent, children) ->
                db.execSQL("INSERT INTO locations (name, parentId) VALUES ('$parent', NULL)")
                // Get the parent ID (last inserted row)
                val cursor = db.query("SELECT last_insert_rowid()")
                cursor.moveToFirst()
                val parentId = cursor.getLong(0)
                cursor.close()
                children.forEach { child ->
                    db.execSQL("INSERT INTO locations (name, parentId) VALUES ('$child', $parentId)")
                }
            }
        }
    }
}
