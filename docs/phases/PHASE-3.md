# Phase 3 — Business-Trip Recording

## Goal

Build the fast workplace-to-workplace business-trip workflow.

## Implemented

- Employer/predefined business locations.
- Employer-defined one-way routes and distances.
- Seven-day simple view.
- Plus/minus controls for daily trip counts.
- One-way and return travel as ordered legs.
- Multiple legs represented by `BusinessTripLeg.sequence`.
- Transport mode stored on trips and legs.
- Trip purpose/notes supported by the domain model.
- Business-trip history table.
- Daily/weekly/monthly totals.
- Pure domain calculation helpers with unit tests.

## Simple view

The user primarily selects existing locations. The UI exposes the configured employer-defined distance immediately rather than requiring a map lookup.

Example:

```text
Office -> Church
5 km one way
10 km return

[ - ]  2  [ + ]
```

Return travel requires a configured reverse route. A return journey is persisted as two legs, preserving the actual route and its distance provenance.

## Data principle

A business trip is an actual event. A multi-location journey is represented as ordered legs rather than as a single flattened distance. Employer-defined distances are persisted on each leg and are never replaced by a calculated map distance in this phase.

## Exit criteria

A user can construct a normal week of workplace-to-workplace trips quickly, inspect the resulting trips in history, and obtain correct daily, weekly, and monthly mileage totals. Domain tests cover employment/date filtering, ordered-leg route matching, one-way vs return trips, and multi-leg distance summation.
