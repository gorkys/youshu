package com.youshu.app.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.Location
import com.youshu.app.data.repository.CategoryRepository
import com.youshu.app.data.repository.ItemRepository
import com.youshu.app.data.repository.LocationRepository
import com.youshu.app.util.ImageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun exportBackup(targetUri: Uri) {
        val backupFile = createBackupZip()
        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            backupFile.inputStream().use { input -> input.copyTo(output) }
        } ?: error("无法打开导出目标")
        backupFile.delete()
    }

    suspend fun importBackup(sourceUri: Uri) {
        val workingDir = File(context.cacheDir, "backup-import-${System.currentTimeMillis()}").apply {
            mkdirs()
        }
        val backupZip = File(workingDir, "backup.zip")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            backupZip.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取备份文件")

        unzip(backupZip, workingDir)
        val manifestFile = File(workingDir, BACKUP_MANIFEST_NAME)
        val payload = manifestFile.readText()
        val backup = json.decodeFromString<AppBackupPayload>(payload)

        val importedItems = backup.items.map { snapshot ->
            val resolvedImages = snapshot.imagePaths.mapNotNull { relativePath ->
                val sourceFile = File(workingDir, relativePath)
                if (!sourceFile.exists()) {
                    null
                } else {
                    ImageUtil.copyImageInto(
                        context = context,
                        sourcePath = sourceFile.absolutePath,
                        targetName = "import_${UUID.randomUUID()}.jpg"
                    )
                }
            }
            snapshot.toItem(resolvedImages)
        }

        categoryRepository.replaceAll(backup.categories.map { it.toEntity() })
        locationRepository.replaceAll(backup.locations.map { it.toEntity() })
        itemRepository.replaceAll(importedItems)

        workingDir.deleteRecursively()
    }

    suspend fun createBackupZip(): File {
        val workingFile = File(context.cacheDir, "youshu-backup-${System.currentTimeMillis()}.zip")
        val imageEntries = mutableListOf<ImageEntry>()
        val backup = AppBackupPayload(
            exportedAt = System.currentTimeMillis(),
            categories = categoryRepository.getAllCategoriesSnapshot().map { CategorySnapshot.fromEntity(it) },
            locations = locationRepository.getAllLocationsSnapshot().map { LocationSnapshot.fromEntity(it) },
            items = itemRepository.getAllItemsSnapshot().map { item ->
                val images = item.imagePathList().mapIndexedNotNull { index, path ->
                    val file = File(path)
                    if (!file.exists()) {
                        null
                    } else {
                        val relativePath = "images/${item.id}_${index}_${file.name}"
                        imageEntries += ImageEntry(relativePath, file)
                        relativePath
                    }
                }
                ItemSnapshot.fromEntity(item, images)
            }
        )

        ZipOutputStream(FileOutputStream(workingFile)).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_MANIFEST_NAME))
            zip.write(json.encodeToString(backup).toByteArray())
            zip.closeEntry()

            imageEntries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.relativePath))
                entry.file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }

        return workingFile
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }
    }

    private data class ImageEntry(
        val relativePath: String,
        val file: File
    )

    companion object {
        private const val BACKUP_MANIFEST_NAME = "backup.json"
    }
}

@Serializable
data class AppBackupPayload(
    val exportedAt: Long,
    val categories: List<CategorySnapshot>,
    val locations: List<LocationSnapshot>,
    val items: List<ItemSnapshot>
)

@Serializable
data class CategorySnapshot(
    val id: Long,
    val name: String,
    val icon: String
) {
    fun toEntity(): Category = Category(id = id, name = name, icon = icon)

    companion object {
        fun fromEntity(category: Category): CategorySnapshot {
            return CategorySnapshot(
                id = category.id,
                name = category.name,
                icon = category.icon
            )
        }
    }
}

@Serializable
data class LocationSnapshot(
    val id: Long,
    val name: String,
    val parentId: Long?
) {
    fun toEntity(): Location = Location(id = id, name = name, parentId = parentId)

    companion object {
        fun fromEntity(location: Location): LocationSnapshot {
            return LocationSnapshot(
                id = location.id,
                name = location.name,
                parentId = location.parentId
            )
        }
    }
}

@Serializable
data class ItemSnapshot(
    val id: Long,
    val name: String,
    val categoryId: Long?,
    val locationId: Long?,
    val quantity: Int,
    val unit: String,
    val price: Double?,
    val expireTime: Long?,
    val status: Int,
    val rating: Int?,
    val ratedAt: Long?,
    val deletedAt: Long?,
    val note: String,
    val imagePaths: List<String>,
    val createdAt: Long
) {
    fun toItem(resolvedImages: List<String>): Item {
        return Item(
            id = id,
            name = name,
            categoryId = categoryId,
            locationId = locationId,
            quantity = quantity,
            unit = unit,
            price = price,
            expireTime = expireTime,
            status = status,
            rating = rating,
            ratedAt = ratedAt,
            deletedAt = deletedAt,
            note = note,
            imagePath = resolvedImages.firstOrNull().orEmpty(),
            imagePaths = Item.encodeImagePaths(resolvedImages),
            createdAt = createdAt
        )
    }

    companion object {
        fun fromEntity(item: Item, imagePaths: List<String>): ItemSnapshot {
            return ItemSnapshot(
                id = item.id,
                name = item.name,
                categoryId = item.categoryId,
                locationId = item.locationId,
                quantity = item.quantity,
                unit = item.unit,
                price = item.price,
                expireTime = item.expireTime,
                status = item.status,
                rating = item.rating,
                ratedAt = item.ratedAt,
                deletedAt = item.deletedAt,
                note = item.note,
                imagePaths = imagePaths,
                createdAt = item.createdAt
            )
        }
    }
}
