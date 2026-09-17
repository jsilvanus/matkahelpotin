package fi.italeino.matkahelpotin.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReimbursementPolicyTest {
    @Test
    fun selectsRateValidOnTripDate() {
        val old = ReimbursementRate(type = "KM", amountCents = 40, validFrom = LocalDate.of(2025,1,1), validUntil = LocalDate.of(2026,1,1))
        val current = ReimbursementRate(type = "KM", amountCents = 50, validFrom = LocalDate.of(2026,1,1))
        assertEquals(50, selectReimbursementRate(listOf(old, current), LocalDate.of(2026,9,18))!!.amountCents)
    }

    @Test
    fun snapshotsRateAndMileagePolicy() {
        val trip = BusinessTrip(date = LocalDate.of(2026,9,18), employmentId = java.util.UUID.randomUUID())
        val rate = ReimbursementRate(type = "KM", amountCents = 53, validFrom = LocalDate.of(2026,1,1))
        val policy = MileagePolicy(year = 2026, mileageRateCentsPerKm = 53, mileageLimitMeters = 500_000)
        val snapshot = trip.withPolicySnapshot(rate, policy)
        assertEquals(53, snapshot.reimbursementRateCentsPerKmSnapshot)
        assertEquals(53, snapshot.mileageRateCentsPerKmSnapshot)
        assertEquals(500_000L, snapshot.mileageLimitMetersSnapshot)
        assertEquals(rate.id, snapshot.reimbursementRateIdSnapshot)
        assertEquals(policy.id, snapshot.mileagePolicyIdSnapshot)
    }

    @Test
    fun calculatesReimbursementFromEffectiveDistance() {
        val trip = BusinessTrip(
            date = LocalDate.of(2026,9,18),
            employmentId = java.util.UUID.randomUUID(),
            reimbursementRateCentsPerKmSnapshot = 53,
        )
        val legs = listOf(
            BusinessTripLeg(
                businessTripId = trip.id,
                sequence = 0,
                fromAddress = "A",
                toAddress = "B",
                distanceMeters = 12_500,
                distanceSource = DistanceSource.MANUAL_OVERRIDE,
                manualDistanceOverrideMeters = 12_500,
                transportMode = TransportMode.PRIVATE_CAR,
            )
        )
        assertEquals(662L, calculateBusinessTripReimbursementCents(trip, legs))
    }
}
