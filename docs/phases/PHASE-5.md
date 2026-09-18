# Phase 5 — Export and Interchange Contract

## Goal

Make Android produce a stable, versioned, import-ready ZIP package. The export package is the integration API for the future Chromium extension.

## Package

```text
travel-export.zip
├── manifest.json
├── commute.json             # when selected
├── business-trips.json      # when selected
└── checksums.json
```

Only selected datasets are included.

## User flow

The user selects:
- inclusive from date;
- inclusive until date;
- Commute, Business trips, or both.

The export is performed entirely offline through Android Storage Access Framework.

## Manifest

Minimum fields:
- format identifier: `matkahelpotin-travel-export`;
- format version;
- schema version;
- application version;
- creation timestamp;
- exported date range;
- included datasets.

Keep package compatibility versioning separate from application versioning.

## Commute dataset

Export the selected commute records plus the referenced data needed to interpret them:
- employments;
- commute profiles;
- referenced home/workplace places;
- historical distance snapshots;
- historical cost snapshots.

Do not export only current profile state: historical records must remain interpretable after profiles are edited.

## Business-trip dataset

Export selected trips and referenced data:
- employments;
- relevant business locations and routes;
- trips;
- ordered legs;
- transport modes;
- purposes/notes;
- effective distances;
- distance provenance;
- original routing distance/provider;
- manual overrides;
- reimbursement snapshots;
- mileage-policy snapshots;
- arbitrary origin/destination addresses where present.

The historical effective distance is authoritative; consumers must not recalculate it from current routes or policies.

## JSON schemas

Create and version:
```text
schemas/
├── manifest.schema.json
├── commute.schema.json
└── business-trips.schema.json
```

Schemas describe the interchange contract, not Room entities.

Use:
- ISO dates;
- ISO timestamps;
- UUID strings;
- integer metres for distances;
- integer cents for monetary amounts;
- explicit nullable fields and enums.

## Architecture

Keep export independent of Compose and Room:

```text
Export UI
  -> ExportViewModel
      -> ExportUseCase
          -> repositories
          -> export DTOs
          -> JSON serialization
          -> contract validation
          -> ZIP writer
          -> SHA-256
          -> Storage Access Framework
```

Export DTOs must be separate from database entities.

## Checksums

Generate `checksums.json` containing SHA-256 for every other file actually present in the archive.

```json
{
  "algorithm": "SHA-256",
  "files": {
    "manifest.json": "...",
    "commute.json": "...",
    "business-trips.json": "..."
  }
}
```

## Validation

Before saving:
1. build export DTOs;
2. serialize JSON;
3. validate archive structure, versions, date ranges and checksums;
4. keep the versioned JSON Schemas as the independent interchange contract;
5. calculate checksums;
5. write the ZIP;
6. verify archive contents and checksums;
7. expose success/failure to the user.

Export must never modify application data.

## Tests

Unit tests:
- inclusive date filtering;
- dataset selection;
- empty datasets;
- referenced-object inclusion;
- snapshot preservation;
- distance provenance;
- manual overrides;
- policy snapshots;
- serialization;
- schema validation;
- checksums.

Integration tests:
- complete ZIP generation;
- ZIP contents;
- checksum verification;
- omitted datasets;
- import dependency ordering;
- ADD / DESTROY / NONE date semantics;
- export → import reconstruction;
- date-scoped DESTROY behaviour;
- independent reconstruction of exported data.

Regression tests must cover multiple employments/profiles, public transport, employer-defined routes, multi-leg trips, routing distances, manual overrides, and later policy/configuration changes.

## Exit criteria

Phase 5 is complete when a user can select any date range and either/both datasets, produce a validated ZIP, inspect an import by date, choose ADD / DESTROY / NONE per dataset/date, and import it atomically without changing historical snapshots or distance provenance.
