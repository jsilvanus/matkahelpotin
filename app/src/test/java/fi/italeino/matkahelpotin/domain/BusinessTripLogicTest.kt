package fi.italeino.matkahelpotin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class BusinessTripLogicTest {
    private val employment = UUID.randomUUID()
    private val home = UUID.randomUUID()
    private val office = UUID.randomUUID()
    private val church = UUID.randomUUID()

    private fun trip(date: LocalDate, id: EntityId = UUID.randomUUID()) =
        BusinessTrip(id = id, date = date, employmentId = employment)

    private fun leg(tripId: EntityId, sequence: Int, from: EntityId, to: EntityId, meters: Long) =
        BusinessTripLeg(
            businessTripId = tripId,
            sequence = sequence,
            fromPlaceId = from,
            toPlaceId = to,
            distanceMeters = meters,
            distanceSource = DistanceSource.EMPLOYER_DEFINED,
            transportMode = TransportMode.PRIVATE_CAR,
        )

    @Test
    fun summaryCountsOnlySelectedEmploymentAndDateRange() {
        val selected = trip(LocalDate.of(2026, 9, 10))
        val otherEmploymentTrip = BusinessTrip(date = selected.date, employmentId = UUID.randomUUID())
        val outside = trip(LocalDate.of(2026, 9, 20))
        val legs = listOf(
            leg(selected.id, 0, home, office, 12000),
            leg(selected.id, 1, office, home, 12000),
            leg(otherEmploymentTrip.id, 0, home, office, 50000),
            leg(outside.id, 0, home, church, 7000),
        )

        val summary = summarizeBusinessTrips(
            listOf(selected, otherEmploymentTrip, outside),
            legs,
            employment,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 15),
        )

        assertEquals(1, summary.tripCount)
        assertEquals(2, summary.legCount)
        assertEquals(24000, summary.distanceMeters)
    }

    @Test
    fun routeMatchingUsesFirstOrderedLeg() {
        val t = trip(LocalDate.of(2026, 9, 10))
        val route = Route(fromPlaceId = home, toPlaceId = office, distanceMeters = 12000, source = RouteSource.EMPLOYER_DEFINED)
        val legs = listOf(leg(t.id, 0, home, office, 12000), leg(t.id, 1, office, church, 8000))

        assertTrue(routeMatchesFirstLeg(t, legs, route))
        assertFalse(isReturnTrip(t, legs))
    }

    @Test
    fun returnTripRequiresReverseSecondLeg() {
        val t = trip(LocalDate.of(2026, 9, 10))
        val legs = listOf(leg(t.id, 0, home, office, 12000), leg(t.id, 1, office, home, 12000))

        assertTrue(isReturnTrip(t, legs))
    }

    @Test
    fun multiLegDistanceIsSummedInSequenceIndependentOfInputOrder() {
        val t = trip(LocalDate.of(2026, 9, 10))
        val legs = listOf(
            leg(t.id, 2, church, home, 5000),
            leg(t.id, 0, home, office, 12000),
            leg(t.id, 1, office, church, 8000),
        )

        val summary = summarizeBusinessTrips(listOf(t), legs, employment)

        assertEquals(1, summary.tripCount)
        assertEquals(3, summary.legCount)
        assertEquals(25000, summary.distanceMeters)
    }

    @Test
    fun oneWayTripIsNotReturnTrip() {
        val t = trip(LocalDate.of(2026, 9, 10))
        val legs = listOf(leg(t.id, 0, home, office, 12000))
        val route = Route(fromPlaceId = office, toPlaceId = home, distanceMeters = 12000, source = RouteSource.EMPLOYER_DEFINED)

        assertFalse(isReturnTrip(t, legs))
        assertFalse(routeMatchesFirstLeg(t, legs, route))
    }
}


class RoutingProvenanceTest {
    private val tripId = UUID.randomUUID()

    private val origin = RoutingEndpoint("Pori Church, Pori")
    private val destination = RoutingEndpoint("Pori Cemetery, Pori")

    @Test
    fun routingLegRetainsCalculatedDistanceAndProvider() {
        val leg = businessTripLegFromRouting(
            businessTripId = tripId,
            sequence = 0,
            origin = origin,
            destination = destination,
            transportMode = TransportMode.PRIVATE_CAR,
            result = RoutingResult(12500, 900, "test-router"),
        )

        assertEquals(DistanceSource.ROUTING_PROVIDER, leg.distanceSource)
        assertEquals(12500, leg.distanceMeters)
        assertEquals(12500, leg.calculatedDistanceMeters)
        assertEquals("test-router", leg.calculatedDistanceProvider)
        assertEquals(origin.address, leg.fromAddress)
        assertEquals(destination.address, leg.toAddress)
    }

    @Test
    fun manualOverrideChangesEffectiveDistanceButRetainsCalculation() {
        val leg = businessTripLegFromRouting(
            businessTripId = tripId,
            sequence = 0,
            origin = origin,
            destination = destination,
            transportMode = TransportMode.PRIVATE_CAR,
            result = RoutingResult(12500, provider = "test-router"),
        ).withManualDistanceOverride(11000)

        assertEquals(DistanceSource.MANUAL_OVERRIDE, leg.distanceSource)
        assertEquals(11000, leg.distanceMeters)
        assertEquals(11000, leg.manualDistanceOverrideMeters)
        assertEquals(12500, leg.calculatedDistanceMeters)
        assertEquals("test-router", leg.calculatedDistanceProvider)
    }

    @Test(expected = IllegalArgumentException::class)
    fun employerDefinedLegCannotCarryRoutingMetadata() {
        validateBusinessTripLeg(
            BusinessTripLeg(
                businessTripId = tripId,
                sequence = 0,
                fromAddress = origin.address,
                toAddress = destination.address,
                distanceMeters = 12500,
                distanceSource = DistanceSource.EMPLOYER_DEFINED,
                calculatedDistanceMeters = 12500,
                transportMode = TransportMode.PRIVATE_CAR,
            )
        )
    }
}
