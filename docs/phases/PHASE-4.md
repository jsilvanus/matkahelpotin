# Phase 4 — Complex Routing and Calculations

## Goal

Support arbitrary-address business trips and make the calculation model robust enough for real reimbursement/tax use.

## Features

- Arbitrary origin/destination addresses.
- Place search and geocoding.
- Routing provider abstraction.
- Route result with distance and source.
- Map presentation where useful.
- Manual distance override.
- Multi-leg business trips.
- Explicit route/distance provenance.
- Versioned reimbursement rates.
- Annual mileage policies.
- Historical calculation snapshots.

## Provider boundary

Define a domain-level routing interface. Do not couple business-trip entities to a particular map provider.

```text
RoutingProvider
    -> geocode(address)
    -> route(origin, destination)
```

## Calculation rules

Rates are selected by effective date. Recording a trip must never be blocked merely because an annual limit is reached; limits are information used for calculation/reporting.

## Exit criteria

A user can record a business trip between arbitrary addresses, obtain a sourced distance, edit it when appropriate, and see the resulting reimbursement/mileage calculation. Historical records remain stable when settings or rates later change.
