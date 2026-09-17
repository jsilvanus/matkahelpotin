# Phase 2 — Commute Recording

## Goal

Make commuting recording fast enough for everyday use.

## Features

- Employment/workplace configuration.
- Commute profiles per employment/workplace.
- Private-car distance and public-transport ticket-cost configuration.
- Monthly calendar.
- Day click cycles through configured commute profiles and no commute.
- Profile colours are configurable/presentation-only.
- Daily trip count and calculated distance/cost.
- Monthly and annual totals.
- Annual mileage policy and limit indicators.
- Commute history table.

## UI principle

The common action should require one tap: open the month and tap the day. Detailed editing is available separately for unusual days.

## Data principle

The daily record references the commute profile rather than copying its mutable configuration. Calculated historical values must be stored/snapshotted where necessary so later configuration changes do not rewrite history.

## Exit criteria

A user can configure several workplaces and record a complete month of commuting with minimal interaction, then inspect accurate monthly/yearly totals and history.
