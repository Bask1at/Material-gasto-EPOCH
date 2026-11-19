package pt.materialgasto.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalog_items",
    indices = [
        Index(value = ["referenciaNormalized"]),
        Index(value = ["designacaoNormalized"])
    ]
)
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val referencia: String,
    val designacao: String,
    val referenciaNormalized: String,
    val designacaoNormalized: String
)
