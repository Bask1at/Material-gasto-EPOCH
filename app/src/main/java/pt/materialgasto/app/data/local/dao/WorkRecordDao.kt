package pt.materialgasto.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pt.materialgasto.app.data.local.entity.MaterialItemEntity
import pt.materialgasto.app.data.local.entity.WorkRecordEntity
import pt.materialgasto.app.data.local.entity.WorkRecordWithMaterials

@Dao
interface WorkRecordDao {
    @Transaction
    @Query("SELECT * FROM work_records ORDER BY createdAtMillis DESC")
    fun observeAllWithMaterials(): Flow<List<WorkRecordWithMaterials>>

    @Transaction
    @Query("SELECT * FROM work_records WHERE id = :recordId")
    fun observeByIdWithMaterials(recordId: Long): Flow<WorkRecordWithMaterials?>

    @Transaction
    @Query("SELECT * FROM work_records WHERE id = :recordId")
    suspend fun getByIdWithMaterials(recordId: Long): WorkRecordWithMaterials?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkRecord(record: WorkRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<MaterialItemEntity>)

    @Query("UPDATE work_records SET pdfFilePath = :pdfPath WHERE id = :recordId")
    suspend fun updatePdfPath(recordId: Long, pdfPath: String)

    @Query("DELETE FROM work_records WHERE id = :recordId")
    suspend fun deleteWorkRecordById(recordId: Long)

    @Transaction
    suspend fun insertRecordWithMaterials(
        record: WorkRecordEntity,
        materials: List<MaterialItemEntity>
    ): Long {
        val recordId = insertWorkRecord(record)
        val linkedMaterials = materials.mapIndexed { index, item ->
            item.copy(workRecordId = recordId, sortOrder = index)
        }
        insertMaterials(linkedMaterials)
        return recordId
    }
}
