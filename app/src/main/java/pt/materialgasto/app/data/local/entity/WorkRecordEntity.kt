package pt.materialgasto.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_records")
data class WorkRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workOrder: String,
    val technicianName: String,
    val dateText: String,
    val pdfFilePath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
