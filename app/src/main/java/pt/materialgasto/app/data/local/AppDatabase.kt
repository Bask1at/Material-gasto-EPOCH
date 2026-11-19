package pt.materialgasto.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pt.materialgasto.app.data.local.dao.CatalogDao
import pt.materialgasto.app.data.local.dao.SettingsDao
import pt.materialgasto.app.data.local.dao.TechnicianDao
import pt.materialgasto.app.data.local.dao.WorkRecordDao
import pt.materialgasto.app.data.local.entity.AppSettingsEntity
import pt.materialgasto.app.data.local.entity.CatalogItemEntity
import pt.materialgasto.app.data.local.entity.MaterialItemEntity
import pt.materialgasto.app.data.local.entity.TechnicianEntity
import pt.materialgasto.app.data.local.entity.WorkRecordEntity

@Database(
    entities = [
        TechnicianEntity::class,
        CatalogItemEntity::class,
        WorkRecordEntity::class,
        MaterialItemEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun technicianDao(): TechnicianDao
    abstract fun catalogDao(): CatalogDao
    abstract fun workRecordDao(): WorkRecordDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "material_gasto.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch {
                            INSTANCE?.let { database ->
                                database.settingsDao().upsert(
                                    AppSettingsEntity(
                                        id = 1,
                                        destinationEmail = AppSettingsEntity.DEFAULT_DESTINATION_EMAIL
                                    )
                                )
                                database.technicianDao().insertAll(
                                    listOf(
                                        TechnicianEntity(name = "João Silva"),
                                        TechnicianEntity(name = "Manuel Ferreira"),
                                        TechnicianEntity(name = "Carlos Santos"),
                                        TechnicianEntity(name = "Tiago Almeida")
                                    )
                                )
                            }
                        }
                    }
                })
                .build()
        }
    }
}
