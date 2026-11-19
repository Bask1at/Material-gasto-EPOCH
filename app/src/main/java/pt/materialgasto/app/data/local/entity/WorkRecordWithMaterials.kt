package pt.materialgasto.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WorkRecordWithMaterials(
    @Embedded
    val record: WorkRecordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workRecordId"
    )
    val materials: List<MaterialItemEntity>
)
