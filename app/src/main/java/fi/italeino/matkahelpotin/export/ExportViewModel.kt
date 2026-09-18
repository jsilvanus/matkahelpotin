package fi.italeino.matkahelpotin.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.italeino.matkahelpotin.MatkahelpotinApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExportState(val busy: Boolean = false, val data: ByteArray? = null, val error: String? = null)

class ExportViewModel(private val service: ExportService) : ViewModel() {
    private val _state = MutableStateFlow(ExportState())
    val state: StateFlow<ExportState> = _state

    fun export(from: LocalDate, until: LocalDate, commute: Boolean, business: Boolean, applicationVersion: String) {
        if (from.isAfter(until) || (!commute && !business)) {
            _state.value = ExportState(error = "Select a valid date range and at least one dataset")
            return
        }
        viewModelScope.launch {
            _state.value = ExportState(busy = true)
            runCatching {
                service.buildZip(ExportSelection(from, until, buildSet {
                    if (commute) add(ExportDataset.COMMUTE)
                    if (business) add(ExportDataset.BUSINESS_TRIPS)
                }), applicationVersion)
            }.onSuccess { _state.value = ExportState(data = it) }
             .onFailure { _state.value = ExportState(error = it.message ?: "Export failed") }
        }
    }

    fun clear() { _state.value = ExportState() }

    companion object {
        fun factory(app: MatkahelpotinApplication) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ExportViewModel(
                ExportService(app.employmentRepository, app.placeRepository, app.commuteProfileRepository, app.commuteRecordRepository,
                    app.businessLocationRepository, app.routeRepository, app.businessTripRepository, app.businessTripLegRepository)
            ) as T
        }
    }
}
