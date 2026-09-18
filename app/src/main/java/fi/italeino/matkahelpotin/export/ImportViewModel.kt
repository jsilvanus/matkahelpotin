package fi.italeino.matkahelpotin.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.italeino.matkahelpotin.MatkahelpotinApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ImportState(
    val busy: Boolean = false,
    val packageData: ImportService.ImportPackage? = null,
    val result: ImportResult? = null,
    val error: String? = null,
)

class ImportViewModel(private val service: ImportService) : ViewModel() {
    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state

    fun inspect(bytes: ByteArray) {
        viewModelScope.launch {
            _state.value = ImportState(busy = true)
            runCatching { service.inspect(bytes) }
                .onSuccess { _state.value = ImportState(packageData = it) }
                .onFailure { _state.value = ImportState(error = it.message ?: "Import validation failed") }
        }
    }

    fun import(plan: ImportPlan) {
        val packageData = _state.value.packageData ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { service.import(packageData, plan) }
                .onSuccess { _state.value = _state.value.copy(busy = false, result = it) }
                .onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "Import failed") }
        }
    }

    fun clear() { _state.value = ImportState() }

    companion object {
        fun factory(app: MatkahelpotinApplication) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ImportViewModel(ImportService(app.database)) as T
        }
    }
}
