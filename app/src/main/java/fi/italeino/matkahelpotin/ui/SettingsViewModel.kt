package fi.italeino.matkahelpotin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.italeino.matkahelpotin.domain.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val employmentRepository: EmploymentRepository,
    private val placeRepository: PlaceRepository,
    private val vehicleRepository: VehicleRepository,
    private val reimbursementRateRepository: ReimbursementRateRepository,
    private val mileagePolicyRepository: MileagePolicyRepository,
) : ViewModel() {
    val employments = employmentRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val places = placeRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val vehicles = vehicleRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reimbursementRates = reimbursementRateRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val mileagePolicies = mileagePolicyRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addEmployment(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { employmentRepository.upsert(Employment(employerName = name.trim())) }
    }

    fun addPlace(name: String, type: PlaceType) {
        if (name.isBlank()) return
        viewModelScope.launch { placeRepository.upsert(Place(name = name.trim(), type = type)) }
    }

    fun addVehicle(name: String, registration: String?) {
        if (name.isBlank()) return
        viewModelScope.launch { vehicleRepository.upsert(Vehicle(name = name.trim(), registration = registration?.trim()?.ifBlank { null })) }
    }

    fun addReimbursementRate(amountCentsPerKm: Long, validFrom: java.time.LocalDate) {
        if (amountCentsPerKm < 0) return
        viewModelScope.launch { reimbursementRateRepository.upsert(ReimbursementRate(type = "KM", amountCents = amountCentsPerKm, validFrom = validFrom)) }
    }

    fun addMileagePolicy(year: Int, rateCentsPerKm: Long, limitMeters: Long?) {
        if (year < 2000 || rateCentsPerKm < 0 || limitMeters != null && limitMeters < 0) return
        viewModelScope.launch { mileagePolicyRepository.upsert(MileagePolicy(year = year, mileageRateCentsPerKm = rateCentsPerKm, mileageLimitMeters = limitMeters)) }
    }

    companion object {
        fun factory(application: MatkahelpotinApplication) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(
                application.employmentRepository, application.placeRepository, application.vehicleRepository,
                application.reimbursementRateRepository, application.mileagePolicyRepository
            ) as T
        }
    }
}
