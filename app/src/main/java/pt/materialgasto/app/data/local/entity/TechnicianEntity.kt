package pt.materialgasto.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "technicians")
data class TechnicianEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
