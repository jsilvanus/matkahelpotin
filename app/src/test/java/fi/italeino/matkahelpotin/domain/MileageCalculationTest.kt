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
    fun noLimitMeansNoRemainingLimit() {
        assertEquals(null, remainingMileageMeters(123, null))
    }
}
