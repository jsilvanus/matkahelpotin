package fi.italeino.matkahelpotin.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class MileageCalculationTest {
    @Test
    fun calculatesAnnualMileageAndRemainingLimit() {
        val employment = UUID.randomUUID()
        val trip = BusinessTrip(date = LocalDate.of(2026, 9, 18), employmentId = employment)
        val legs = listOf(
            BusinessTripLeg(
                businessTripId = trip.id,
                sequence = 0,
                fromAddress = "A",
                toAddress = "B",
                distanceMeters = 12500,
                distanceSource = DistanceSource.MANUAL_OVERRIDE,
                manualDistanceOverrideMeters = 12500,
                transportMode = TransportMode.PRIVATE_CAR,
            )
        )
        val meters = calculateAnnualMileageMeters(listOf(trip), legs, 2026, employment)
        assertEquals(12500, meters)
        assertEquals(487500, remainingMileageMeters(meters, 500000))
    }

    @Test
    fun excludesPublicTransportFromAnnualMileage() {
        val employment = UUID.randomUUID()
        val trip = BusinessTrip(
            date = LocalDate.of(2026, 9, 18),
            employmentId = employment,
            transportMode = TransportMode.PUBLIC_TRANSPORT,
        )
        val leg = BusinessTripLeg(
            businessTripId = trip.id,
            sequence = 0,
            fromAddress = "A",
            toAddress = "B",
            distanceMeters = 100_000,
            distanceSource = DistanceSource.MANUAL_OVERRIDE,
            manualDistanceOverrideMeters = 100_000,
            transportMode = TransportMode.PUBLIC_TRANSPORT,
        )
        assertEquals(0L, calculateAnnualMileageMeters(listOf(trip), listOf(leg), 2026, employment))
    }

    @Test
    fun annualLimitUsesPolicyEffectiveForReferenceDate() {
        val old = MileagePolicy(
            year = 2026,
            mileageRateCentsPerKm = 50,
            mileageLimitMeters = 500_000,
            validFrom = LocalDate.of(2026, 1, 1),
        )
        val current = MileagePolicy(
            year = 2026,
            mileageRateCentsPerKm = 53,
            mileageLimitMeters = 600_000,
            validFrom = LocalDate.of(2026, 7, 1),
        )
        assertEquals(current.id, selectAnnualMileagePolicy(listOf(old, current), 2026, LocalDate.of(2026, 9, 18))?.id)
        assertEquals(old.id, selectAnnualMileagePolicy(listOf(old, current), 2026, LocalDate.of(2026, 6, 30))?.id)
    }

    @Test
    fun commuteMileageCountsOnlyPrivateCarRecordsAndTripDistance() {
        val profile = CommuteProfile(
            employmentId = UUID.randomUUID(), homePlaceId = UUID.randomUUID(), workplaceId = UUID.randomUUID(),
            transportMode = TransportMode.PRIVATE_CAR, distanceMeters = 25_000, tripsPerDay = 2,
        )
        val record = CommuteRecord(date = LocalDate.of(2026, 9, 18), commuteProfileId = profile.id, tripCount = 2, distanceMetersSnapshot = 25_000)
        assertEquals(50_000L, calculateAnnualCommuteMileageMeters(listOf(record), listOf(profile), 2026))
    }

    @Test
    fun commutePolicyDoesNotGetSelectedForBusinessMileage() {
        val commute = MileagePolicy(year = 2026, mileageRateCentsPerKm = 0, mileageLimitMeters = 100_000, scope = MileagePolicyScope.COMMUTE)
        assertEquals(null, selectAnnualMileagePolicy(listOf(commute), 2026, LocalDate.of(2026, 9, 18)))
    }

    @Test
    fun noLimitMeansNoRemainingLimit() {
        assertEquals(null, remainingMileageMeters(123, null))
    }
}
