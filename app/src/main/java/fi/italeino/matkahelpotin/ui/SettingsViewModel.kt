package fi.italeino.matkahelpotin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fi.italeino.matkahelpotin.MatkahelpotinApplication
import fi.italeino.matkahelpotin.domain.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(
    private val employmentRepository: EmploymentRepository,
    private val placeRepository: PlaceRepository,
    private val vehicleRepository: VehicleRepository,
    private val commuteProfileRepository: CommuteProfileRepository,
    private val reimbursementRateRepository: ReimbursementRateRepository,
    private val mileagePolicyRepository: MileagePolicyRepository,
) : ViewModel() {
    val employments = employmentRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val places = placeRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val vehicles = vehicleRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val commuteProfiles = commuteProfileRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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

    fun addCommuteProfile(
        employmentId: EntityId,
        homePlaceId: EntityId,
        workplaceId: EntityId,
        transportMode: TransportMode,
        distanceKm: String,
        ticketPriceEur: String,
        tripsPerDay: String,
    ) {
        val distance = distanceKm.replace(',', '.').toDoubleOrNull()?.let { (it * 1000).toLong() }
        val ticket = ticketPriceEur.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() }
        val trips = tripsPerDay.toIntOrNull()
        if (trips == null || trips <= 0) return
        val profile = CommuteProfile(
            employmentId = employmentId,
            homePlaceId = homePlaceId,
            workplaceId = workplaceId,
            transportMode = transportMode,
            distanceMeters = distance,
            ticketPriceCents = ticket,
            tripsPerDay = trips,
        )
        runCatching { validateCommuteProfile(profile) }.onSuccess {
            viewModelScope.launch { commuteProfileRepository.upsert(profile) }
        }
    }

    fun addReimbursementRate(amountCentsPerKm: Long, validFrom: LocalDate) {
        if (amountCentsPerKm < 0) return
        viewModelScope.launch { reimbursementRateRepository.upsert(ReimbursementRate(type = "KM", amountCents = amountCentsPerKm, validFrom = validFrom)) }
    }

    fun addMileagePolicy(
        year: Int,
        rateCentsPerKm: Long,
        limitMeters: Long?,
        scope: MileagePolicyScope = MileagePolicyScope.BUSINESS_TRIP,
        validFrom: LocalDate = LocalDate.of(year, 1, 1),
        validUntil: LocalDate? = null,
    ) {
        if (year < 2000 || rateCentsPerKm < 0 || limitMeters != null && limitMeters < 0) return
        viewModelScope.launch {
            mileagePolicyRepository.upsert(
                MileagePolicy(
                    year = year,
                    mileageRateCentsPerKm = rateCentsPerKm,
                    mileageLimitMeters = limitMeters,
                    scope = scope,
                    validFrom = validFrom,
                    validUntil = validUntil,
                )
            )
        }
    }

    companion object {
        fun factory(application: MatkahelpotinApplication) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(
                application.employmentRepository,
                application.placeRepository,
                application.vehicleRepository,
                application.commuteProfileRepository,
                application.reimbursementRateRepository,
                application.mileagePolicyRepository,
            ) as T
        }
    }
}
