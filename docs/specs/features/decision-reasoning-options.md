<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Decision Reasoning Options

> **Status: Work in progress.** This feature is being implemented in phases. Not all described functionality is available yet.

## Overview

Daycare and preschool decisions include legal reasoning text that explains why the decision was made. This reasoning typically references political decisions of the municipal board, applied laws, and delegation rules. Municipality admins manage this reasoning text through the employee UI, and the system includes it in the generated decision documents.

Reasoning is split into two categories:

- **Generic reasoning** applies to all decisions within a time period. It is automatically included in every decision based on the decision's start date.
- **Individual reasoning** is case-specific. Service workers select from a predefined list of options when preparing a decision that requires additional justification beyond the generic reasoning.

These reasonings are organized into two collections based on decision domain:

- **Preschool collection:** Used for preschool and preparatory education decisions.
- **Daycare collection:** Used for all other decision types — daycare, daycare part-time, preschool daycare, preschool club, and club decisions.

The decision type determines which collection is used.

## Generic Reasonings

Generic reasonings form a time series within each collection. Each entry defines a validity start date; its validity ends implicitly when the next entry's start date begins.

Each entry contains:

- The reasoning text body that appears in the decision document.
- The text is provided in Finnish and Swedish. Support for English may be added in the future.

The text format is plain text. Generic reasonings do not have a manually entered title — the UI displays a formatted label based on the collection type and validity date range.

### Immutability and superseding

A generic reasoning entry can only be edited while it is in the not-ready state. Once it is marked as ready, it becomes immutable — it cannot be edited or reverted to not-ready.

If a ready entry needs to be corrected, the admin creates a new entry with the same start date. When multiple entries share the same start date within a collection, the one with the latest creation time is the active one. The superseded entries are considered outdated.

This design ensures that the full history of reasoning entries is preserved.

### Ready state and decision blocking

Each generic reasoning entry has a ready/not-ready flag. New entries default to not ready, but the admin may choose to activate an entry immediately upon creation if the text is finalized. This allows the admin to prepare the next season's reasoning in advance with placeholder text while the political decisions are still being finalized, or to create and activate an entry in a single step when the text is already known.

While the active generic reasoning for a given validity period is not ready, any decision whose start date falls within that period is blocked from advancing beyond the "waiting for decision" state. This prevents decisions from being sent with incomplete or placeholder reasoning. Once the admin finalizes the text and marks the entry as ready, the block is lifted and the affected decisions can proceed.

### Outdated entries

A generic reasoning entry is considered outdated in two cases:

1. Its implicit end date is before today. Since decisions are not made retroactively, such an entry will in practice never be used for new decisions. Note that an entry is not outdated merely because a later entry exists — multiple entries covering future periods may be active simultaneously, each applicable to decisions with different start dates.
2. It has been superseded by a newer entry with the same start date.

Outdated entries are displayed separately in the management UI.

## Individual Reasonings

Individual reasonings are a flat list of options within each collection. When a service worker prepares a decision that needs additional justification beyond the generic reasoning, they select one or more options from this list.

Each entry contains:

- A short descriptive title in Finnish and Swedish.
- The reasoning text body in Finnish and Swedish.

Individual reasoning entries are immutable — they cannot be edited after creation. If an entry needs to be corrected, the admin removes it from use and creates a new one. When a service worker selects an individual reasoning for a decision, the decision stores a reference to the entry. Since entries are immutable, the referenced text is guaranteed to remain unchanged.

Service workers cannot write free-form reasoning text. They can only select from the options that the admin has defined.

The admin can remove an option from use. Removed options can no longer be added to new decisions, but decisions that already reference them are not affected. Removed options remain visible in an archived section of the management UI, preserving the full history of all options that have ever existed.

There are no validity time ranges for individual reasonings. If an option is only applicable for a certain period, this can be communicated through the title (e.g. "Sibling basis 2025-2026").

## Data Model

Reasonings are stored in two tables, one per reasoning type. Both use an enum `decision_reasoning_collection_type` with values `DAYCARE` and `PRESCHOOL`.

**`decision_reasoning_generic`** stores the generic reasoning time series:

| Column | Description |
|--------|-------------|
| `collection_type` | Which collection this entry belongs to (daycare or preschool) |
| `valid_from` | Date from which this reasoning is valid. Multiple entries may share the same start date within a collection; the latest-created one is active. |
| `text_fi`, `text_sv` | Reasoning text body in Finnish and Swedish |
| `ready` | Whether the reasoning text is finalized and decisions may proceed |

**`decision_reasoning_individual`** stores the individual reasoning options:

| Column | Description |
|--------|-------------|
| `collection_type` | Which collection this entry belongs to (daycare or preschool) |
| `title_fi`, `title_sv` | Short descriptive title in Finnish and Swedish |
| `text_fi`, `text_sv` | Reasoning text body in Finnish and Swedish |
| `removed_at` | Timestamp when the option was removed from use (null if active) |

## Management UI

The reasoning management page is accessible from the employee UI user context menu for admin users.

The page has two primary tabs for selecting the collection type: daycare and preschool. Within each tab, there are two sections: one for generic reasonings and one for individual reasonings. Each section heading shows the number of entries in parentheses and has an "Add new" button.

For generic reasonings, each entry is displayed as a full-width card with a formatted label derived from the collection type and validity date range. Finnish and Swedish text are shown side by side. Not-ready entries have an edit button that transforms the card into an inline edit form with cancel and save buttons. Ready entries are read-only.

For individual reasonings, each entry is displayed as a full-width card with Finnish and Swedish text side by side. Within each language block, the card shows a language label (FI / SV), the title, and the text body.

For individual reasonings, each active card has a "Remove from use" button. There is no edit functionality — entries are immutable after creation.

Outdated generic entries (past their implicit end date or superseded) and removed individual entries are shown in collapsible "Show outdated" sections at the bottom of their respective lists. These entries are fully read-only.

## Access Control

- Management UI page visibility and write operations: admin only.
- Read access to reasoning data: admin and service worker.

## Phased Delivery

- **Phase 1 — Admin management UI:** CRUD for both reasoning types, not yet connected to decisions. Hidden behind a feature flag.
- **Phase 2 — Decision draft and PDF integration:** Generic reasoning shown on the decision draft view based on the decision start date. Service workers can pick individual reasonings. Not-ready blocking mechanism prevents decisions from advancing. Decision PDFs include the database-sourced reasoning text.
