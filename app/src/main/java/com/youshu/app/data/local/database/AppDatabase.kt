package com.youshu.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.youshu.app.data.local.dao.CategoryDao
import com.youshu.app.data.local.dao.ItemDao
import com.youshu.app.data.local.dao.LocationDao
import com.youshu.app.data.local.dao.AiModelConfigDao
import com.youshu.app.data.local.entity.AiModelConfig
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.Location
import com.youshu.app.util.LegacyTextNormalizer

@Database(
    entities = [Item::class, Category::class, Location::class, AiModelConfig::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun locationDao(): LocationDao
    abstract fun aiModelConfigDao(): AiModelConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN rating INTEGER")
                db.execSQL("ALTER TABLE items ADD COLUMN ratedAt INTEGER")
                SeedHelper.normalizeLegacyData(db)
                SeedHelper.ensureSeedData(db)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN deletedAt INTEGER")
                SeedHelper.normalizeLegacyData(db)
                SeedHelper.ensureSeedData(db)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN imagePaths TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE items
                    SET imagePaths = CASE
                        WHEN TRIM(COALESCE(imagePath, '')) = '' THEN ''
                        ELSE imagePath
                    END
                    """.trimIndent()
                )
                SeedHelper.normalizeLegacyData(db)
                SeedHelper.ensureSeedData(db)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SeedHelper.createAiModelConfigTable(db)
                SeedHelper.ensureAiModelSeedData(db)
                SeedHelper.normalizeLegacyData(db)
                SeedHelper.ensureSeedData(db)
            }
        }

        fun buildDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "youshu.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(PrepopulateCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class PrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            SeedHelper.ensureAiModelSeedData(db)
            SeedHelper.ensureSeedData(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            SeedHelper.normalizeLegacyData(db)
            SeedHelper.ensureAiModelSeedData(db)
            SeedHelper.ensureSeedData(db)
        }
    }
}

private object SeedHelper {
    private val defaultCategories = listOf("食品", "药品", "日用品", "数码", "衣物", "文具", "工具", "其他")

    private val roomHierarchy = linkedMapOf(
        "厨房" to listOf("冰箱", "橱柜", "台面"),
        "客厅" to listOf("电视柜", "茶几", "书架"),
        "卧室" to listOf("衣柜", "床头柜", "梳妆台"),
        "卫生间" to listOf("洗手台", "淋浴间"),
        "阳台" to listOf("储物柜"),
        "书房" to listOf("书桌", "书柜"),
        "储物间" to listOf("收纳架")
    )

    fun ensureSeedData(db: SupportSQLiteDatabase) {
        defaultCategories.forEach { ensureCategory(db, it) }

        val homeId = ensureHomeRoot(db)
        roomHierarchy.forEach { (room, children) ->
            val roomId = ensureLocation(db, room, homeId)
            children.forEach { child ->
                ensureLocation(db, child, roomId)
            }
        }
    }

    fun createAiModelConfigTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ai_model_configs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                alias TEXT NOT NULL,
                provider TEXT NOT NULL,
                endpoint TEXT NOT NULL,
                modelName TEXT NOT NULL,
                apiKey TEXT NOT NULL,
                supportsVision INTEGER NOT NULL,
                isDefault INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_model_configs_isDefault ON ai_model_configs(isDefault)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_model_configs_provider ON ai_model_configs(provider)")
    }

    fun ensureAiModelSeedData(db: SupportSQLiteDatabase) {
        createAiModelConfigTable(db)
        val count = db.query("SELECT COUNT(*) FROM ai_model_configs").useCursor { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        if (count > 0L) return

        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO ai_model_configs
                (alias, provider, endpoint, modelName, apiKey, supportsVision, isDefault, createdAt, updatedAt)
            VALUES
                ('OpenAI Compatible', 'openai-compatible', 'https://api.openai.com/v1', 'gpt-4o-mini', '', 1, 1, $now, $now)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO ai_model_configs
                (alias, provider, endpoint, modelName, apiKey, supportsVision, isDefault, createdAt, updatedAt)
            VALUES
                ('Ollama Local', 'ollama', 'http://127.0.0.1:11434/v1', 'llava', '', 1, 0, $now, $now)
            """.trimIndent()
        )
    }

    fun normalizeLegacyData(db: SupportSQLiteDatabase) {
        LegacyTextNormalizer.categoryReplacements.forEach { (legacy, correct) ->
            db.execSQL(
                "UPDATE categories SET name = '${correct.sql()}' WHERE name = '${legacy.sql()}'"
            )
        }
        LegacyTextNormalizer.locationReplacements.forEach { (legacy, correct) ->
            db.execSQL(
                "UPDATE locations SET name = '${correct.sql()}' WHERE name = '${legacy.sql()}'"
            )
        }
        val legacyUnits = LegacyTextNormalizer.legacyUnitNames.joinToString { "'${it.sql()}'" }
        db.execSQL("UPDATE items SET unit = '件' WHERE unit IN ($legacyUnits)")
    }

    private fun ensureCategory(db: SupportSQLiteDatabase, name: String) {
        if (findCategoryId(db, name) == null) {
            db.execSQL("INSERT INTO categories (name, icon) VALUES ('${name.sql()}', '')")
        }
    }

    private fun ensureHomeRoot(db: SupportSQLiteDatabase): Long {
        val canonicalName = LegacyTextNormalizer.HOME_ROOT_NAME
        val legacyNames = LegacyTextNormalizer.aliases(canonicalName)
            .joinToString { "'${it.sql()}'" }

        db.execSQL(
            "UPDATE locations SET name = '${canonicalName.sql()}' " +
                "WHERE parentId IS NULL AND name IN ($legacyNames)"
        )

        val rootIds = mutableListOf<Long>()
        db.query(
            "SELECT id FROM locations " +
                "WHERE parentId IS NULL AND name = '${canonicalName.sql()}' ORDER BY id ASC"
        ).useCursor { cursor ->
            while (cursor.moveToNext()) {
                rootIds += cursor.getLong(0)
            }
        }

        if (rootIds.isEmpty()) {
            db.execSQL("INSERT INTO locations (name, parentId) VALUES ('${canonicalName.sql()}', NULL)")
            return requireNotNull(findLocationId(db, canonicalName, null))
        }

        val keeperId = rootIds.first()
        rootIds.drop(1).forEach { duplicateId ->
            db.execSQL("UPDATE locations SET parentId = $keeperId WHERE parentId = $duplicateId")
            db.execSQL("UPDATE items SET locationId = $keeperId WHERE locationId = $duplicateId")
            db.execSQL("DELETE FROM locations WHERE id = $duplicateId")
        }

        return keeperId
    }

    private fun ensureLocation(db: SupportSQLiteDatabase, name: String, parentId: Long?): Long {
        findLocationId(db, name, parentId)?.let { return it }

        val parentSql = parentId?.toString() ?: "NULL"
        db.execSQL(
            "INSERT INTO locations (name, parentId) VALUES ('${name.sql()}', $parentSql)"
        )
        return requireNotNull(findLocationId(db, name, parentId))
    }

    private fun findCategoryId(db: SupportSQLiteDatabase, name: String): Long? {
        return db.query(
            "SELECT id FROM categories WHERE name = '${name.sql()}' LIMIT 1"
        ).useCursor { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    private fun findLocationId(
        db: SupportSQLiteDatabase,
        name: String,
        parentId: Long?
    ): Long? {
        val parentClause = parentId?.let { "parentId = $it" } ?: "parentId IS NULL"
        return db.query(
            "SELECT id FROM locations WHERE name = '${name.sql()}' AND $parentClause LIMIT 1"
        ).useCursor { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    private fun String.sql(): String = replace("'", "''")
}

private inline fun <T> android.database.Cursor.useCursor(block: (android.database.Cursor) -> T): T {
    return try {
        block(this)
    } finally {
        close()
    }
}
