# Phase 3 — Business-Trip Recording

## Goal

Build the fast workplace-to-workplace business-trip workflow.

## Features

- Employer/predefined business locations.
- Employer-defined one-way routes and distances.
- Seven-day simple view.
- Plus/minus controls for daily trip counts.
- One-way and return travel.
- Multiple legs in one business trip.
- Transport mode.
- Trip purpose/notes where appropriate.
- Business-trip history table.
- Daily/weekly/monthly totals.

## Simple view

The user primarily selects existing locations. The UI should expose the known distance immediately rather than requiring a map lookup.

Example:

```text
Office -> Church
5 km one way
10 km return

[ - ]  2  [ + ]
```

## Data principle

A business trip is an actual event. A multi-location journey is represented as ordered legs rather than as a single flattened distance.

## Exit criteria

A user can construct a normal week of workplace-to-workplace trips quickly, inspect the resulting trips in history, and obtain correct total mileage.
