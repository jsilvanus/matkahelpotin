# Matkahelpotin Implementation Plan

## Product

Matkahelpotin is a local-first Android application for recording commuting and work-related business trips. The Android application is the system of record. There is no backend, web application, Node.js service, or PostgreSQL database.

The application exports selected records as a versioned structured ZIP package. A separate Chromium extension reads that package and assists the user in entering the data into target systems such as tax services or Visma Intu. Browser interaction remains user-visible and user-verifiable.

## Technology

### Android

- Kotlin
- Jetpack Compose
- AndroidX
- Room / SQLite
- Kotlin Serialization
- Coroutines / Flow
- ViewModel
- Navigation Compose
- Android Storage Access Framework for export/share

The architecture should follow the existing `tilastohelpotin` style: domain concepts are separated from Android/data implementations, external providers are behind interfaces, and UI observes application state rather than containing business logic.

### Chromium extension

- TypeScript
- Manifest V3
- Extension service worker
- Content scripts
- JSON Schema validation
- ZIP import
- Target-specific browser workflow adapters

The extension is a separate future component. Its contract with Android is the export package, not an API.

## Core domain

The initial domain consists of:

- Employment
- Place
- EmploymentWorkplace
- CommuteProfile
- CommuteRecord
- BusinessLocation
- Route
- BusinessTrip
- BusinessTripLeg
- Vehicle
- ReimbursementRate
- MileagePolicy

### Commute

A commute profile represents a recurring home/workplace relationship and its transport configuration. A daily commute record records whether and how many normal trips occurred on a particular date.

Supported initial transport modes:

- Private car
- Public transport

A private-car profile stores distance. A public-transport profile stores the relevant ticket cost. Multiple employment/workplace relationships are supported.

### Business trips

Business trips are actual work-related travel events. The simple view uses predefined locations and employer-defined routes. The complex view supports arbitrary addresses and routing. A business trip consists of one or more ordered legs.

Routes retain their source. An employer-defined distance must not silently be replaced by a map provider's calculated distance.

## Navigation model

The main application scope is selected first:

- Commute
- Business trips

Within each scope:

- Simple
- Complex
- History

Export is a separate operation rather than a history mode.

## Views

### Commute / Simple

Monthly calendar. Clicking a day cycles through configured commute profiles and back to no commute. Profile colours are presentation only; records reference the profile ID.

### Commute / Complex

Detailed day/week editing where required for multiple trips or unusual days.

### Business trips / Simple

Seven-day view using predefined locations and routes. Plus/minus controls allow rapid entry of trip counts.

### Business trips / Complex

Arbitrary origin/destination addresses, route calculation, map presentation, multi-leg trips, and manual distance override.

### History

Tabular audit-oriented view. It is optimized for reviewing records and manually copying data into external systems. It supports date filtering, type filtering, totals, and detailed records.

## Reimbursement calculations

Rates and annual mileage policies are versioned by effective date/year. Historical records must not silently change when current rates change.

A mileage limit should flag relevant records/totals rather than prevent the user from recording what actually happened.

## Export contract

The export is the integration API.

```text
travel-export.zip
├── manifest.json
├── commute.json             # optional
├── business-trips.json      # optional
└── checksums.json
```

The user chooses:

- date range
- commute, business trips, or both

Only selected datasets are included.

The manifest contains at minimum:

- format identifier
- format version
- schema version
- application/version
- creation timestamp
- exported date range
- included datasets

The JSON files contain domain data, not UI-specific state.

JSON schemas live in the repository and are versioned with the application:

```text
docs/
  PLAN.md
  phases/
    PHASE-1.md ... PHASE-6.md
schemas/
  manifest.schema.json
  commute.schema.json
  business-trips.schema.json
```

## Six implementation phases

1. **Foundation and domain model** — establish the Android project, architecture, Room database, domain model, migrations, and initial settings.
2. **Commute recording** — implement employment/workplace configuration, commute profiles, monthly simple view, calculations, and commute history.
3. **Business-trip recording** — implement predefined locations/routes, simple seven-day view, detailed business-trip model, and history.
4. **Complex routing and calculations** — add arbitrary addresses, routing abstraction/provider, multi-leg trips, manual overrides, reimbursement policies, and annual mileage handling.
5. **Export and interchange contract** — implement schemas, versioned ZIP export, validation, checksums, date-range/dataset selection, and robust import-ready output.
6. **Chromium integration and production hardening** — build the Manifest V3 extension, generic human-in-the-loop workflow engine, target adapters, end-to-end validation, security, migration tests, and release packaging.

Each phase has a dedicated document under `docs/phases/`.

## Definition of done

The first complete product should allow a user to:

1. Configure multiple employment/workplace relationships.
2. Define recurring commute profiles and transport details.
3. Record commuting rapidly from a monthly calendar.
4. Record simple workplace-to-workplace business trips rapidly from a weekly view.
5. Record arbitrary business-trip legs using addresses and routing.
6. Review all data in a clear history table.
7. Calculate mileage/reimbursement information using date-appropriate rules.
8. Export a selected date range and either or both datasets into a versioned ZIP.
9. Import that ZIP into the Chromium extension.
10. Have the extension guide and perform visible browser actions while the user verifies the resulting entries.
