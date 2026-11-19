package pt.materialgasto.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pt.materialgasto.app.data.local.entity.TechnicianEntity

@Dao
interface TechnicianDao {
    @Query("SELECT * FROM technicians ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TechnicianEntity>>

    @Query("SELECT * FROM technicians ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<TechnicianEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(technician: TechnicianEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(technicians: List<TechnicianEntity>)

    @Update
    suspend fun update(technician: TechnicianEntity)

    @Delete
    suspend fun delete(technician: TechnicianEntity)
}
