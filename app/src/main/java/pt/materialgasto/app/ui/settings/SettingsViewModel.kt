package pt.materialgasto.app.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.materialgasto.app.data.local.entity.AppSettingsEntity
import pt.materialgasto.app.data.local.entity.TechnicianEntity
import pt.materialgasto.app.data.repository.MaterialGastoRepository
import pt.materialgasto.app.excel.ExcelCatalogParser
import pt.materialgasto.app.excel.ExcelImportAnalysis
import pt.materialgasto.app.excel.ExcelLoadResult
import pt.materialgasto.app.excel.ParsedSpreadsheet

sealed interface ExcelImportDialogState {
    data object Hidden : ExcelImportDialogState
    data object Loading : ExcelImportDialogState
    data class ColumnMapping(
        val spreadsheet: ParsedSpreadsheet,
        val selectedRefColumn: Int,
        val selectedDescColumn: Int,
        val hasHeaderRow: Boolean
    ) : ExcelImportDialogState

    data class ConflictResolution(
        val analysis: ExcelImportAnalysis,
        val selectedDesignations: Map<String, String>
    ) : ExcelImportDialogState

    data class Confirmation(
        val fileName: String,
        val finalItems: List<Pair<String, String>>,
        val duplicateCount: Int,
        val skippedIncompleteCount: Int,
        val resolvedConflictCount: Int
    ) : ExcelImportDialogState
}

data class SettingsUiState(
    val emailInput: String = AppSettingsEntity.DEFAULT_DESTINATION_EMAIL,
    val emailStatusMessage: String? = null,
    val emailIsError: Boolean = false,
    val catalogLastImportedMillis: Long? = null,
    val catalogFileName: String? = null,
    val catalogItemCount: Int = 0,
    val importDialogState: ExcelImportDialogState = ExcelImportDialogState.Hidden,
    val importFeedbackMessage: String? = null,
    val importFeedbackIsError: Boolean = false
)

class SettingsViewModel(
    private val repository: MaterialGastoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val technicians: StateFlow<List<TechnicianEntity>> = repository.observeTechnicians()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val catalogCount: StateFlow<Int> = repository.observeCatalogCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var hasInitializedEmail = false

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        emailInput = if (!hasInitializedEmail) {
                            hasInitializedEmail = true
                            settings.destinationEmail
                        } else {
                            current.emailInput
                        },
                        catalogLastImportedMillis = settings.catalogLastImportedMillis,
                        catalogFileName = settings.catalogFileName,
                        catalogItemCount = settings.catalogItemCount
                    )
                }
            }
        }
    }

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(emailInput = value, emailStatusMessage = null, emailIsError = false)
        }
    }

    fun saveDestinationEmail() {
        val trimmed = _uiState.value.emailInput.trim()
        if (trimmed.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            _uiState.update {
                it.copy(
                    emailStatusMessage = "Introduza um endereço de email válido.",
                    emailIsError = true
                )
            }
            return
        }
        viewModelScope.launch {
            repository.updateDestinationEmail(trimmed)
            _uiState.update {
                it.copy(
                    emailInput = trimmed,
                    emailStatusMessage = "Email de destino guardado.",
                    emailIsError = false
                )
            }
        }
    }

    fun addTechnician(name: String) {
        viewModelScope.launch {
            repository.addTechnician(name)
        }
    }

    fun updateTechnician(technician: TechnicianEntity, newName: String) {
        viewModelScope.launch {
            repository.updateTechnician(technician, newName)
        }
    }

    fun deleteTechnician(technician: TechnicianEntity) {
        viewModelScope.launch {
            repository.deleteTechnician(technician)
        }
    }

    fun onExcelFilePicked(context: Context, uri: Uri) {
        _uiState.update {
            it.copy(
                importDialogState = ExcelImportDialogState.Loading,
                importFeedbackMessage = null,
                importFeedbackIsError = false
            )
        }

        viewModelScope.launch {
            val fileName = resolveDisplayFileName(context, uri)
            val result = withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    ExcelLoadResult.Error("Não foi possível abrir o ficheiro selecionado.")
                } else {
                    inputStream.use { stream ->
                        ExcelCatalogParser.parseAndAnalyze(stream, fileName)
                    }
                }
            }

            when (result) {
                is ExcelLoadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            importDialogState = ExcelImportDialogState.Hidden,
                            importFeedbackMessage = result.message,
                            importFeedbackIsError = true
                        )
                    }
                }

                is ExcelLoadResult.NeedsColumnMapping -> {
                    val defaultRef = result.suggestedRefColumn ?: 0
                    val defaultDesc = result.suggestedDescColumn
                        ?: if (defaultRef == 0) 1 else 0
                    _uiState.update {
                        it.copy(
                            importDialogState = ExcelImportDialogState.ColumnMapping(
                                spreadsheet = result.spreadsheet,
                                selectedRefColumn = defaultRef,
                                selectedDescColumn = defaultDesc,
                                hasHeaderRow = true
                            )
                        )
                    }
                }

                is ExcelLoadResult.ReadyForReview -> {
                    proceedWithAnalysis(result.analysis)
                }
            }
        }
    }

    fun updateColumnMapping(refColumn: Int, descColumn: Int, hasHeaderRow: Boolean) {
        val current = _uiState.value.importDialogState
        if (current is ExcelImportDialogState.ColumnMapping) {
            _uiState.update {
                it.copy(
                    importDialogState = current.copy(
                        selectedRefColumn = refColumn,
                        selectedDescColumn = descColumn,
                        hasHeaderRow = hasHeaderRow
                    )
                )
            }
        }
    }

    fun confirmColumnMapping() {
        val current = _uiState.value.importDialogState
        if (current !is ExcelImportDialogState.ColumnMapping) return

        if (current.selectedRefColumn == current.selectedDescColumn) {
            _uiState.update {
                it.copy(
                    importDialogState = ExcelImportDialogState.Hidden,
                    importFeedbackMessage = "A coluna de Referência e a coluna de Designação não podem ser a mesma.",
                    importFeedbackIsError = true
                )
            }
            return
        }

        val analysis = ExcelCatalogParser.analyzeColumns(
            spreadsheet = current.spreadsheet,
            refCol = current.selectedRefColumn,
            descCol = current.selectedDescColumn,
            hasHeaderRow = current.hasHeaderRow
        )
        proceedWithAnalysis(analysis)
    }

    private fun proceedWithAnalysis(analysis: ExcelImportAnalysis) {
        if (analysis.uniqueItems.isEmpty() && analysis.conflicts.isEmpty()) {
            _uiState.update {
                it.copy(
                    importDialogState = ExcelImportDialogState.Hidden,
                    importFeedbackMessage = "Não foram encontrados artigos válidos (Referência + Designação) no ficheiro.",
                    importFeedbackIsError = true
                )
            }
            return
        }

        if (analysis.conflicts.isNotEmpty()) {
            val defaults = analysis.conflicts.associate { conflict ->
                conflict.normalizedReferencia to conflict.designations.first()
            }
            _uiState.update {
                it.copy(
                    importDialogState = ExcelImportDialogState.ConflictResolution(
                        analysis = analysis,
                        selectedDesignations = defaults
                    )
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    importDialogState = ExcelImportDialogState.Confirmation(
                        fileName = analysis.fileName,
                        finalItems = analysis.uniqueItems,
                        duplicateCount = analysis.duplicateCount,
                        skippedIncompleteCount = analysis.skippedIncompleteCount,
                        resolvedConflictCount = 0
                    )
                )
            }
        }
    }

    fun selectConflictResolution(normalizedRef: String, designation: String) {
        val current = _uiState.value.importDialogState
        if (current is ExcelImportDialogState.ConflictResolution) {
            _uiState.update {
                it.copy(
                    importDialogState = current.copy(
                        selectedDesignations = current.selectedDesignations + (normalizedRef to designation)
                    )
                )
            }
        }
    }

    fun confirmConflictsResolved() {
        val current = _uiState.value.importDialogState
        if (current !is ExcelImportDialogState.ConflictResolution) return

        val resolvedItems = current.analysis.conflicts.map { conflict ->
            val chosenDesc = current.selectedDesignations[conflict.normalizedReferencia]
                ?: conflict.designations.first()
            conflict.referenciaDisplay to chosenDesc
        }

        val finalItems = current.analysis.uniqueItems + resolvedItems
        _uiState.update {
            it.copy(
                importDialogState = ExcelImportDialogState.Confirmation(
                    fileName = current.analysis.fileName,
                    finalItems = finalItems,
                    duplicateCount = current.analysis.duplicateCount,
                    skippedIncompleteCount = current.analysis.skippedIncompleteCount,
                    resolvedConflictCount = current.analysis.conflicts.size
                )
            )
        }
    }

    fun commitCatalogImport() {
        val current = _uiState.value.importDialogState
        if (current !is ExcelImportDialogState.Confirmation) return

        _uiState.update { it.copy(importDialogState = ExcelImportDialogState.Loading) }

        viewModelScope.launch {
            runCatching {
                repository.importCatalog(
                    items = current.finalItems,
                    fileName = current.fileName
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        importDialogState = ExcelImportDialogState.Hidden,
                        importFeedbackMessage = "Catálogo atualizado com ${current.finalItems.size} artigo(s).",
                        importFeedbackIsError = false
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        importDialogState = ExcelImportDialogState.Hidden,
                        importFeedbackMessage = "Erro ao guardar catálogo: ${err.localizedMessage ?: "Erro"}",
                        importFeedbackIsError = true
                    )
                }
            }
        }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(importDialogState = ExcelImportDialogState.Hidden) }
    }

    private fun resolveDisplayFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIdx)
                if (!name.isNullOrBlank()) return name
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "catalogo.xlsx"
    }

    class Factory(private val repository: MaterialGastoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
