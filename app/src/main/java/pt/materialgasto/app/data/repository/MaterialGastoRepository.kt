package pt.materialgasto.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pt.materialgasto.app.data.local.AppDatabase
import pt.materialgasto.app.data.local.entity.AppSettingsEntity
import pt.materialgasto.app.data.local.entity.CatalogItemEntity
import pt.materialgasto.app.data.local.entity.MaterialItemEntity
import pt.materialgasto.app.data.local.entity.TechnicianEntity
import pt.materialgasto.app.data.local.entity.WorkRecordEntity
import pt.materialgasto.app.data.local.entity.WorkRecordWithMaterials
import pt.materialgasto.app.data.storage.LocalFileStorage
import pt.materialgasto.app.domain.CatalogMatcher
import pt.materialgasto.app.pdf.PdfGenerator
import java.io.File

data class DraftMaterialInput(
    val referencia: String,
    val designacao: String,
    val quantity: Double,
    val barcode: String,
    val photoPath: String?
)

class MaterialGastoRepository(
    private val database: AppDatabase,
    val fileStorage: LocalFileStorage,
    private val pdfGenerator: PdfGenerator
) {
    private val technicianDao = database.technicianDao()
    private val catalogDao = database.catalogDao()
    private val workRecordDao = database.workRecordDao()
    private val settingsDao = database.settingsDao()

    fun observeTechnicians(): Flow<List<TechnicianEntity>> = technicianDao.observeAll()

    fun observeCatalog(): Flow<List<CatalogItemEntity>> = catalogDao.observeAll()

    fun observeCatalogCount(): Flow<Int> = catalogDao.observeCount()

    fun observeWorkRecords(): Flow<List<WorkRecordWithMaterials>> =
        workRecordDao.observeAllWithMaterials()

    fun observeWorkRecord(recordId: Long): Flow<WorkRecordWithMaterials?> =
        workRecordDao.observeByIdWithMaterials(recordId)

    fun observeSettings(): Flow<AppSettingsEntity> =
        settingsDao.observeSettings().map { it ?: AppSettingsEntity() }

    suspend fun getDestinationEmail(): String {
        return settingsDao.getSettings()?.destinationEmail
            ?.takeIf { it.isNotBlank() }
            ?: AppSettingsEntity.DEFAULT_DESTINATION_EMAIL
    }

    suspend fun updateDestinationEmail(email: String) {
        val current = settingsDao.getSettings() ?: AppSettingsEntity()
        settingsDao.upsert(current.copy(destinationEmail = email.trim()))
    }

    suspend fun addTechnician(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) {
            technicianDao.insert(TechnicianEntity(name = clean))
        }
    }

    suspend fun updateTechnician(technician: TechnicianEntity, newName: String) {
        val clean = newName.trim()
        if (clean.isNotEmpty()) {
            technicianDao.update(technician.copy(name = clean))
        }
    }

    suspend fun deleteTechnician(technician: TechnicianEntity) {
        technicianDao.delete(technician)
    }

    suspend fun importCatalog(
        items: List<Pair<String, String>>,
        fileName: String?
    ) = withContext(Dispatchers.IO) {
        val entities = items.map { (ref, desc) ->
            CatalogItemEntity(
                referencia = ref.trim(),
                designacao = desc.trim(),
                referenciaNormalized = CatalogMatcher.normalize(ref),
                designacaoNormalized = CatalogMatcher.normalize(desc)
            )
        }
        catalogDao.replaceCatalog(entities)

        val current = settingsDao.getSettings() ?: AppSettingsEntity()
        settingsDao.upsert(
            current.copy(
                catalogLastImportedMillis = System.currentTimeMillis(),
                catalogFileName = fileName,
                catalogItemCount = entities.size
            )
        )
    }

    suspend fun saveWorkRecordAndGeneratePdf(
        workOrder: String,
        technicianName: String,
        dateText: String,
        materials: List<DraftMaterialInput>
    ): WorkRecordWithMaterials = withContext(Dispatchers.IO) {
        val record = WorkRecordEntity(
            workOrder = workOrder.trim(),
            technicianName = technicianName.trim(),
            dateText = dateText.trim(),
            createdAtMillis = System.currentTimeMillis()
        )
        val materialEntities = materials.mapIndexed { index, draft ->
            MaterialItemEntity(
                workRecordId = 0,
                referencia = draft.referencia.trim(),
                designacao = draft.designacao.trim(),
                quantity = draft.quantity,
                barcode = draft.barcode.trim(),
                photoPath = draft.photoPath,
                sortOrder = index
            )
        }
        val recordId = workRecordDao.insertRecordWithMaterials(record, materialEntities)
        val saved = workRecordDao.getByIdWithMaterials(recordId)
            ?: error("Erro ao recuperar registo guardado")

        val pdfFile = fileStorage.buildUniquePdfFile(
            workOrder = saved.record.workOrder,
            technicianName = saved.record.technicianName,
            dateText = saved.record.dateText
        )
        pdfGenerator.generate(saved, pdfFile)
        workRecordDao.updatePdfPath(recordId, pdfFile.absolutePath)

        workRecordDao.getByIdWithMaterials(recordId) ?: saved
    }

    suspend fun ensurePdfExists(recordWithMaterials: WorkRecordWithMaterials): File =
        withContext(Dispatchers.IO) {
            val existingPath = recordWithMaterials.record.pdfFilePath
            if (!existingPath.isNullOrBlank()) {
                val existingFile = File(existingPath)
                if (existingFile.exists() && existingFile.length() > 0L) {
                    return@withContext existingFile
                }
            }
            val newPdfFile = fileStorage.buildUniquePdfFile(
                workOrder = recordWithMaterials.record.workOrder,
                technicianName = recordWithMaterials.record.technicianName,
                dateText = recordWithMaterials.record.dateText
            )
            pdfGenerator.generate(recordWithMaterials, newPdfFile)
            workRecordDao.updatePdfPath(recordWithMaterials.record.id, newPdfFile.absolutePath)
            newPdfFile
        }

    suspend fun deleteWorkRecord(recordWithMaterials: WorkRecordWithMaterials) =
        withContext(Dispatchers.IO) {
            fileStorage.deleteFileIfExists(recordWithMaterials.record.pdfFilePath)
            recordWithMaterials.materials.forEach { item ->
                fileStorage.deleteFileIfExists(item.photoPath)
            }
            workRecordDao.deleteWorkRecordById(recordWithMaterials.record.id)
        }
}
