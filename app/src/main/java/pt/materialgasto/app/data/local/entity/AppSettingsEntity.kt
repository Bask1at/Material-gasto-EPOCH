package pt.materialgasto.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val destinationEmail: String = DEFAULT_DESTINATION_EMAIL,
    val catalogLastImportedMillis: Long? = null,
    val catalogFileName: String? = null,
    val catalogItemCount: Int = 0
) {
    companion object {
        const val DEFAULT_DESTINATION_EMAIL = "pedro.lopes@homeserve.pt"
    }
}
