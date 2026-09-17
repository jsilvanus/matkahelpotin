# Matkahelpotin

Local-first Android application for recording commuting and work-related business trips.

## Phase 1

The repository now contains the Android foundation and initial domain/data layer:

- Kotlin + Jetpack Compose
- Room / SQLite
- domain models independent of Room
- stable UUID identifiers and `LocalDate`/`Instant` handling
- employment, places and vehicle setup
- commute and business-trip domain entities
- repository interfaces with Room implementations
- core domain validation tests
- Room schema export configuration

The application has no backend dependency. External routing and export integrations are introduced in later phases.

See [`docs/PLAN.md`](docs/PLAN.md) and [`docs/phases/PHASE-1.md`](docs/phases/PHASE-1.md).
