package pt.materialgasto.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pt.materialgasto.app.data.local.entity.CatalogItemEntity

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_items ORDER BY referencia ASC")
    fun observeAll(): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items ORDER BY referencia ASC")
    suspend fun getAll(): List<CatalogItemEntity>

    @Query("SELECT COUNT(*) FROM catalog_items")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogItemEntity>)

    @Query("DELETE FROM catalog_items")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceCatalog(items: List<CatalogItemEntity>) {
        deleteAll()
        if (items.isNotEmpty()) {
            // Chunk inserts so huge Excel catalogs never hit SQLite bind limits
            items.chunked(500).forEach { chunk ->
                insertAll(chunk)
            }
        }
    }
}
