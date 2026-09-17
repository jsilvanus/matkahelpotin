package fi.italeino.matkahelpotin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.italeino.matkahelpotin.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class CommuteViewModel(
    private val profileRepository: CommuteProfileRepository,
    private val recordRepository: CommuteRecordRepository,
) : ViewModel() {
    val profiles: Flow<List<CommuteProfile>> = profileRepository.observeAll()
    val records: Flow<List<CommuteRecord>> = recordRepository.observeAll()

    fun cycleDay(date: LocalDate) {
        viewModelScope.launch {
            val profilesNow = profiles.first().filter { it.enabled }.sortedBy { it.id.toString() }
            val current = records.first().firstOrNull { it.date == date }
            val next = when {
                profilesNow.isEmpty() -> null
                current == null -> profilesNow.first()
                else -> profilesNow.dropWhile { it.id != current.commuteProfileId }.drop(1).firstOrNull()
            }
            if (next == null) {
                current?.let { recordRepository.delete(it.id) }
            } else {
                recordRepository.upsert(
                    CommuteRecord(
                        id = current?.id ?: UUID.randomUUID(),
                        date = date,
                        commuteProfileId = next.id,
                        tripCount = next.tripsPerDay,
                        distanceMetersSnapshot = next.distanceMeters,
                        costCentsSnapshot = next.ticketPriceCents,
                    )
                )
            }
        }
    }
}
