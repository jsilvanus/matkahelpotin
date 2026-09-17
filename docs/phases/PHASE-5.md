# Phase 5 — Export and Interchange Contract

## Goal

Turn the Android application into a reliable producer of a stable, versioned interchange package.

## Deliverables

- JSON Schema definitions.
- `manifest.json` schema and writer.
- `commute.json` schema and writer.
- `business-trips.json` schema and writer.
- `checksums.json` generation.
- Structured ZIP creation.
- Date-range selection.
- Commute-only export.
- Business-trip-only export.
- Combined export.
- Export preview and validation.
- Automated compatibility tests.

## Package format

```text
travel-export.zip
├── manifest.json
├── commute.json             # when selected
├── business-trips.json      # when selected
└── checksums.json
```

## Versioning

The manifest contains independent format/schema/application versions. The extension must be able to determine what it can consume without guessing.

## Design requirement

The export contains canonical domain data and required calculation results, not Compose/UI state or database implementation details.

## Exit criteria

For any selected date range and dataset selection, Android produces a valid ZIP that can be validated independently of the app. Repeated export/import validation produces consistent results.
