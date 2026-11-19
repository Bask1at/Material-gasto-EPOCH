package pt.materialgasto.app.ui.newrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.materialgasto.app.data.local.entity.CatalogItemEntity
import pt.materialgasto.app.data.local.entity.TechnicianEntity
import pt.materialgasto.app.data.local.entity.WorkRecordWithMaterials
import pt.materialgasto.app.data.repository.DraftMaterialInput
import pt.materialgasto.app.data.repository.MaterialGastoRepository
import pt.materialgasto.app.domain.CatalogMatcher
import pt.materialgasto.app.domain.DesignacaoLookupResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DraftMaterialRow(
    val localId: String = UUID.randomUUID().toString(),
    val referencia: String,
    val designacao: String,
    val quantity: Double,
    val barcode: String,
    val photoPath: String?
)

data class NewRecordUiState(
    val workOrder: String = "",
    val technicianName: String = "",
    val dateText: String = todayDateText(),
    val draftReferencia: String = "",
    val draftDesignacao: String = "",
    val draftQuantity: String = "",
    val draftBarcode: String = "",
    val draftPhotoPath: String? = null,
    val editingMaterialLocalId: String? = null,
    val referenciaSuggestions: List<CatalogItemEntity> = emptyList(),
    val designacaoSuggestions: List<CatalogItemEntity> = emptyList(),
    val ambiguousDesignacaoMatches: List<CatalogItemEntity> = emptyList(),
    val addedMaterials: List<DraftMaterialRow> = emptyList(),
    val materialError: String? = null,
    val formError: String? = null,
    val isSavingRecord: Boolean = false,
    val completedRecord: WorkRecordWithMaterials? = null,
    val destinationEmail: String = "pedro.lopes@homeserve.pt"
) {
    val hasPendingDraftInput: Boolean
        get() = draftReferencia.isNotBlank() ||
            draftDesignacao.isNotBlank() ||
            draftQuantity.isNotBlank() ||
            draftBarcode.isNotBlank() ||
            !draftPhotoPath.isNullOrBlank()
}

private fun todayDateText(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT")).format(Date())
}

class NewRecordViewModel(
    private val repository: MaterialGastoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewRecordUiState())
    val uiState: StateFlow<NewRecordUiState> = _uiState.asStateFlow()

    val technicians: StateFlow<List<TechnicianEntity>> = repository.observeTechnicians()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val catalogFlow: StateFlow<List<CatalogItemEntity>> = repository.observeCatalog()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Override tracking flags to respect manual edits after auto-fill
    private var autoFilledDesignacao = false
    private var autoFilledReferencia = false
    private var userOverrodeDesignacao = false
    private var userOverrodeReferencia = false

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.update { it.copy(destinationEmail = settings.destinationEmail) }
            }
        }
    }

    fun onWorkOrderChanged(value: String) {
        _uiState.update { it.copy(workOrder = value, formError = null) }
    }

    fun onTechnicianNameChanged(value: String) {
        _uiState.update { it.copy(technicianName = value, formError = null) }
    }

    fun onDateTextChanged(value: String) {
        _uiState.update { it.copy(dateText = value, formError = null) }
    }

    fun onReferenciaChanged(newRef: String) {
        val catalog = catalogFlow.value
        val currentState = _uiState.value

        if (autoFilledReferencia && newRef != currentState.draftReferencia) {
            userOverrodeReferencia = true
            autoFilledReferencia = false
        }

        val suggestions = CatalogMatcher.suggestByReferencia(newRef, catalog)
        val exactMatch = CatalogMatcher.findExactByReferencia(newRef, catalog)

        var updatedDesignacao = currentState.draftDesignacao
        if (exactMatch != null) {
            if (updatedDesignacao.isBlank() || !userOverrodeDesignacao) {
                updatedDesignacao = exactMatch.designacao
                autoFilledDesignacao = true
                userOverrodeDesignacao = false
            }
        } else if (newRef.isBlank() && autoFilledDesignacao && !userOverrodeDesignacao) {
            updatedDesignacao = ""
            autoFilledDesignacao = false
        }

        _uiState.update {
            it.copy(
                draftReferencia = newRef,
                draftDesignacao = updatedDesignacao,
                referenciaSuggestions = suggestions,
                designacaoSuggestions = emptyList(),
                ambiguousDesignacaoMatches = emptyList(),
                materialError = null,
                formError = null
            )
        }
    }

    fun onDesignacaoChanged(newDesc: String) {
        val catalog = catalogFlow.value
        val currentState = _uiState.value

        if (autoFilledDesignacao && newDesc != currentState.draftDesignacao) {
            userOverrodeDesignacao = true
            autoFilledDesignacao = false
        }

        val suggestions = CatalogMatcher.suggestByDesignacao(newDesc, catalog)
        var updatedReferencia = currentState.draftReferencia
        var ambiguousMatches: List<CatalogItemEntity> = emptyList()

        when (val lookup = CatalogMatcher.findExactByDesignacao(newDesc, catalog)) {
            is DesignacaoLookupResult.ExactUniqueMatch -> {
                if (updatedReferencia.isBlank() || !userOverrodeReferencia) {
                    updatedReferencia = lookup.item.referencia
                    autoFilledReferencia = true
                    userOverrodeReferencia = false
                }
            }
            is DesignacaoLookupResult.MultipleMatches -> {
                ambiguousMatches = lookup.items
            }
            DesignacaoLookupResult.NoExactMatch -> {
                if (newDesc.isBlank() && autoFilledReferencia && !userOverrodeReferencia) {
                    updatedReferencia = ""
                    autoFilledReferencia = false
                }
            }
        }

        _uiState.update {
            it.copy(
                draftReferencia = updatedReferencia,
                draftDesignacao = newDesc,
                designacaoSuggestions = suggestions,
                referenciaSuggestions = emptyList(),
                ambiguousDesignacaoMatches = ambiguousMatches,
                materialError = null,
                formError = null
            )
        }
    }

    /**
     * Fills ONLY Referência and Designação when user selects a suggestion or resolves
     * a duplicate designation. Never touches Quantidade, Código de barras, or Fotografia.
     */
    fun onSelectCatalogItem(item: CatalogItemEntity) {
        autoFilledDesignacao = true
        autoFilledReferencia = true
        userOverrodeDesignacao = false
        userOverrodeReferencia = false

        _uiState.update {
            it.copy(
                draftReferencia = item.referencia,
                draftDesignacao = item.designacao,
                referenciaSuggestions = emptyList(),
                designacaoSuggestions = emptyList(),
                ambiguousDesignacaoMatches = emptyList(),
                materialError = null
            )
        }
    }

    fun dismissSuggestions() {
        _uiState.update {
            it.copy(
                referenciaSuggestions = emptyList(),
                designacaoSuggestions = emptyList()
            )
        }
    }

    fun dismissAmbiguousDialog() {
        _uiState.update { it.copy(ambiguousDesignacaoMatches = emptyList()) }
    }

    fun onQuantityChanged(value: String) {
        // Allow digits, one decimal separator (comma or dot)
        val filtered = value.filter { it.isDigit() || it == '.' || it == ',' }
        _uiState.update { it.copy(draftQuantity = filtered, materialError = null, formError = null) }
    }

    fun onBarcodeChanged(value: String) {
        _uiState.update { it.copy(draftBarcode = value, materialError = null) }
    }

    /**
     * Strictly populates ONLY draftBarcode.
     * NEVER modifies Referência, Designação, Quantidade, or Fotografia.
     */
    fun onBarcodeScanned(scannedCode: String) {
        _uiState.update { it.copy(draftBarcode = scannedCode.trim(), materialError = null) }
    }

    /**
     * Strictly populates ONLY draftPhotoPath.
     * NEVER modifies Referência, Designação, Quantidade, or Código de barras.
     */
    fun onPhotoCaptured(newPhotoPath: String) {
        val oldPath = _uiState.value.draftPhotoPath
        val isOldPhotoUsedInAddedList = _uiState.value.addedMaterials.any { it.photoPath == oldPath }
        if (!oldPath.isNullOrBlank() && oldPath != newPhotoPath && !isOldPhotoUsedInAddedList) {
            repository.fileStorage.deleteFileIfExists(oldPath)
        }
        _uiState.update { it.copy(draftPhotoPath = newPhotoPath) }
    }

    fun onRemoveDraftPhoto() {
        val currentPath = _uiState.value.draftPhotoPath
        val isUsedInAddedList = _uiState.value.addedMaterials.any { it.photoPath == currentPath }
        if (!currentPath.isNullOrBlank() && !isUsedInAddedList) {
            repository.fileStorage.deleteFileIfExists(currentPath)
        }
        _uiState.update { it.copy(draftPhotoPath = null) }
    }

    fun saveDraftMaterial() {
        val state = _uiState.value
        val ref = state.draftReferencia.trim()
        val desc = state.draftDesignacao.trim()
        val qtyNormalized = state.draftQuantity.trim().replace(',', '.')
        val qty = qtyNormalized.toDoubleOrNull()

        if (ref.isBlank() && desc.isBlank()) {
            _uiState.update {
                it.copy(materialError = "Introduza a Referência ou a Designação do material.")
            }
            return
        }
        if (qty == null || qty <= 0.0) {
            _uiState.update {
                it.copy(materialError = "Introduza uma quantidade numérica válida maior que zero.")
            }
            return
        }

        val editingId = state.editingMaterialLocalId
        val updatedList = if (editingId != null) {
            val previousItem = state.addedMaterials.firstOrNull { it.localId == editingId }
            if (previousItem?.photoPath != null && previousItem.photoPath != state.draftPhotoPath) {
                repository.fileStorage.deleteFileIfExists(previousItem.photoPath)
            }
            state.addedMaterials.map { item ->
                if (item.localId == editingId) {
                    item.copy(
                        referencia = ref,
                        designacao = desc,
                        quantity = qty,
                        barcode = state.draftBarcode.trim(),
                        photoPath = state.draftPhotoPath
                    )
                } else {
                    item
                }
            }
        } else {
            state.addedMaterials + DraftMaterialRow(
                referencia = ref,
                designacao = desc,
                quantity = qty,
                barcode = state.draftBarcode.trim(),
                photoPath = state.draftPhotoPath
            )
        }

        resetDraftFlags()
        _uiState.update {
            it.copy(
                draftReferencia = "",
                draftDesignacao = "",
                draftQuantity = "",
                draftBarcode = "",
                draftPhotoPath = null,
                editingMaterialLocalId = null,
                referenciaSuggestions = emptyList(),
                designacaoSuggestions = emptyList(),
                ambiguousDesignacaoMatches = emptyList(),
                addedMaterials = updatedList,
                materialError = null,
                formError = null
            )
        }
    }

    fun cancelEditMaterial() {
        val state = _uiState.value
        val currentDraftPhoto = state.draftPhotoPath
        val editingOriginalPhoto = state.addedMaterials
            .firstOrNull { it.localId == state.editingMaterialLocalId }
            ?.photoPath
        if (!currentDraftPhoto.isNullOrBlank() && currentDraftPhoto != editingOriginalPhoto) {
            repository.fileStorage.deleteFileIfExists(currentDraftPhoto)
        }
        resetDraftFlags()
        _uiState.update {
            it.copy(
                draftReferencia = "",
                draftDesignacao = "",
                draftQuantity = "",
                draftBarcode = "",
                draftPhotoPath = null,
                editingMaterialLocalId = null,
                referenciaSuggestions = emptyList(),
                designacaoSuggestions = emptyList(),
                ambiguousDesignacaoMatches = emptyList(),
                materialError = null
            )
        }
    }

    fun editMaterial(item: DraftMaterialRow) {
        resetDraftFlags()
        val formattedQty = if (item.quantity % 1.0 == 0.0) {
            item.quantity.toLong().toString()
        } else {
            item.quantity.toString().replace('.', ',')
        }
        _uiState.update {
            it.copy(
                draftReferencia = item.referencia,
                draftDesignacao = item.designacao,
                draftQuantity = formattedQty,
                draftBarcode = item.barcode,
                draftPhotoPath = item.photoPath,
                editingMaterialLocalId = item.localId,
                referenciaSuggestions = emptyList(),
                designacaoSuggestions = emptyList(),
                ambiguousDesignacaoMatches = emptyList(),
                materialError = null,
                formError = null
            )
        }
    }

    fun deleteMaterial(item: DraftMaterialRow) {
        if (_uiState.value.editingMaterialLocalId == item.localId) {
            cancelEditMaterial()
        }
        repository.fileStorage.deleteFileIfExists(item.photoPath)
        _uiState.update {
            it.copy(
                addedMaterials = it.addedMaterials.filterNot { row -> row.localId == item.localId }
            )
        }
    }

    fun finalizeAndGeneratePdf() {
        val state = _uiState.value
        if (state.isSavingRecord) return

        if (state.workOrder.isBlank()) {
            _uiState.update { it.copy(formError = "Introduza o Número/Nome da Obra.") }
            return
        }
        if (state.technicianName.isBlank()) {
            _uiState.update { it.copy(formError = "Selecione ou introduza o Nome do Técnico.") }
            return
        }
        if (state.dateText.isBlank()) {
            _uiState.update { it.copy(formError = "Introduza a Data do registo.") }
            return
        }
        if (state.hasPendingDraftInput) {
            _uiState.update {
                it.copy(
                    formError = "Tem um material em preenchimento. Carregue em \"Guardar Material\" antes de gravar o registo."
                )
            }
            return
        }
        if (state.addedMaterials.isEmpty()) {
            _uiState.update {
                it.copy(formError = "Adicione pelo menos um material antes de gravar e gerar o PDF.")
            }
            return
        }

        _uiState.update { it.copy(isSavingRecord = true, formError = null) }

        viewModelScope.launch {
            runCatching {
                repository.saveWorkRecordAndGeneratePdf(
                    workOrder = state.workOrder,
                    technicianName = state.technicianName,
                    dateText = state.dateText,
                    materials = state.addedMaterials.map { row ->
                        DraftMaterialInput(
                            referencia = row.referencia,
                            designacao = row.designacao,
                            quantity = row.quantity,
                            barcode = row.barcode,
                            photoPath = row.photoPath
                        )
                    }
                )
            }.onSuccess { savedRecord ->
                _uiState.update {
                    it.copy(
                        isSavingRecord = false,
                        completedRecord = savedRecord
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSavingRecord = false,
                        formError = "Erro ao gravar registo ou gerar PDF: ${err.localizedMessage ?: "Erro desconhecido"}"
                    )
                }
            }
        }
    }

    private fun resetDraftFlags() {
        autoFilledDesignacao = false
        autoFilledReferencia = false
        userOverrodeDesignacao = false
        userOverrodeReferencia = false
    }

    class Factory(private val repository: MaterialGastoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewRecordViewModel(repository) as T
        }
    }
}
