package pt.materialgasto.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "material_items",
    foreignKeys = [
        ForeignKey(
            entity = WorkRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["workRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workRecordId"])]
)
data class MaterialItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workRecordId: Long,
    val referencia: String,
    val designacao: String,
    val quantity: Double,
    val barcode: String,
    val photoPath: String? = null,
    val sortOrder: Int = 0
)
