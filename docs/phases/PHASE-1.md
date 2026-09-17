# Phase 1 — Foundation and Domain Model

## Goal

Create a clean, local-first Kotlin/Android foundation and establish the domain model before building feature UI.

## Deliverables

- Android project using Kotlin and Jetpack Compose.
- Application architecture following the existing `tilastohelpotin` pattern: UI, domain, and data concerns separated.
- Room/SQLite database and migration strategy.
- Domain entities and value objects for employment, places, commute profiles, business locations, routes, trips, vehicles, and policies.
- Repository interfaces with Room implementations.
- Initial settings screens for employment and places.
- Unit tests for core domain rules.

## Domain entities

Start with:

```text
Employment
Place
EmploymentWorkplace
CommuteProfile
CommuteRecord
BusinessLocation
Route
BusinessTrip
BusinessTripLeg
Vehicle
ReimbursementRate
MileagePolicy
```

Keep domain models independent of Room and Android where practical.

## Important rules

- IDs are stable UUIDs.
- Dates use `LocalDate`; timestamps are separate.
- Historical calculations must be reproducible.
- Employer-defined route distances retain their source.
- No network dependency exists in the recording path.

## Exit criteria

A fresh installation can configure an employment, workplaces, home, a vehicle, and initial commute/business-trip locations. Database migrations and domain tests pass.
