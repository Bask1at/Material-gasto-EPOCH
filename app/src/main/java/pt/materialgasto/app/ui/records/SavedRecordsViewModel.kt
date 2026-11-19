package pt.materialgasto.app.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.materialgasto.app.data.local.entity.WorkRecordWithMaterials
import pt.materialgasto.app.data.repository.MaterialGastoRepository
import java.io.File

class SavedRecordsViewModel(
    private val repository: MaterialGastoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val records: StateFlow<List<WorkRecordWithMaterials>> = combine(
        repository.observeWorkRecords(),
        _searchQuery
    ) { allRecords, query ->
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) {
            allRecords
        } else {
            allRecords.filter { item ->
                item.record.workOrder.lowercase().contains(clean) ||
                    item.record.technicianName.lowercase().contains(clean) ||
                    item.record.dateText.lowercase().contains(clean)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val destinationEmail: StateFlow<String> = combine(
        repository.observeSettings(),
        MutableStateFlow(Unit)
    ) { settings, _ ->
        settings.destinationEmail
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "pedro.lopes@homeserve.pt")

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun observeRecord(recordId: Long): Flow<WorkRecordWithMaterials?> {
        return repository.observeWorkRecord(recordId)
    }

    suspend fun getOrRegeneratePdf(recordWithMaterials: WorkRecordWithMaterials): File {
        return repository.ensurePdfExists(recordWithMaterials)
    }

    fun deleteRecord(recordWithMaterials: WorkRecordWithMaterials, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteWorkRecord(recordWithMaterials)
            onDeleted()
        }
    }

    class Factory(private val repository: MaterialGastoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SavedRecordsViewModel(repository) as T
        }
    }
}
