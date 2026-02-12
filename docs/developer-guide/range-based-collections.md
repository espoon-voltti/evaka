// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

# Range-based Collections

## Introduction

`DateSet`, `DateMap<T>`, and their time-based variants (`DateTimeSet`, `DateTimeMap<T>`, `TimeSet`) are immutable data structures optimized for storing consecutive date/time ranges with associated values. Unlike standard `Set<LocalDate>` or `Map<LocalDate, T>`, these collections internally merge adjacent and overlapping ranges, making them highly efficient for temporal data.

**When to use:**
- Storing values for consecutive dates/times (e.g., placement periods)
- A full year (365 dates) with the same value requires only 1 internal entry instead of 365

**When NOT to use:**
- Random, unconnected dates (use standard `Set<LocalDate>` or `Map<LocalDate, T>` instead)

## DateSet

`DateSet` is conceptually similar to `Set<LocalDate>` but provides batch operations using `FiniteDateRange` parameters. It maintains non-overlapping, sorted ranges internally.

### Key Operations

| Operation | Description |
|-----------|-------------|
| `empty(): DateSet` | Create empty set |
| `of(vararg ranges: FiniteDateRange): DateSet` | Create from ranges |
| `add(range: FiniteDateRange): DateSet` | Add range (returns new set) |
| `addAll(ranges: Iterable<FiniteDateRange>): DateSet` | Add multiple ranges (returns new set) |
| `remove(range: FiniteDateRange): DateSet` | Remove range (returns new set) |
| `contains(range: FiniteDateRange): Boolean` | Check if range is fully included |
| `includes(point: LocalDate): Boolean` | Check if point is included |
| `ranges(): Sequence<FiniteDateRange>` | Get internal ranges |
| `gaps(): Sequence<FiniteDateRange>` | Find gaps between ranges |
| `spanningRange(): FiniteDateRange?` | Get range covering entire set |
| `intersection(ranges: Iterable<FiniteDateRange>): DateSet` | Intersect with ranges |
| `isEmpty(): Boolean` | Check if set is empty |

**Note:** Many methods have overloads accepting `Iterable<...>` or `Sequence<...>` instead of vararg. Operators `+` and `-` are available as aliases for `add`/`addAll` and `remove`/`removeAll`.

## DateMap<T>

`DateMap<T>` is conceptually similar to `Map<LocalDate, T>` but provides batch operations using `FiniteDateRange` parameters. Adjacent ranges with the same value are automatically merged.

**Performance:** A `Map<LocalDate, T>` for a full year (365 dates) with one value contains 365 entries. The equivalent `DateMap<T>` contains just 1 entry.

### Key Operations

| Operation | Description |
|-----------|-------------|
| `<T> empty(): DateMap<T>` | Create empty map |
| `<T> of(vararg entries: Pair<FiniteDateRange, T>): DateMap<T>` | Create from entries |
| `<T> of(ranges: Iterable<FiniteDateRange>, value: T): DateMap<T>` | Create from ranges with same value |
| `set(range: FiniteDateRange, value: T): DateMap<T>` | Set value for range (returns new map) |
| `setAll(entries: Iterable<Pair<FiniteDateRange, T>>): DateMap<T>` | Set multiple entries (returns new map) |
| `update(entries: Iterable<Pair<FiniteDateRange, T>>, resolve: (FiniteDateRange, T, T) -> T): DateMap<T>` | Update entries with conflict resolution |
| `getValue(at: LocalDate): T?` | Get value for date (nullable) |
| `entries(): Sequence<Pair<FiniteDateRange, T>>` | Get range-value pairs |
| `ranges(): Sequence<FiniteDateRange>` | Get all ranges in the map |
| `values(): Sequence<T>` | Get all values (may include duplicates) |
| `transpose(): Map<T, DateSet>` | Invert to `Map<T, DateSet>` |
| `gaps(): Sequence<FiniteDateRange>` | Find gaps between ranges |
| `isEmpty(): Boolean` | Check if map is empty |

**Note:** Many methods have overloads accepting `Sequence<...>` instead of `Iterable<...>`. The `update()` method is particularly useful for merging maps with custom conflict resolution logic.

### Example 1: Placement with Backup Care

```kotlin
// Create placement map from database results
val placements: DateMap<DaycareId> = DateMap.of(
    placementResults.map { it.range to it.unitId }
)

// Override with backup care periods (backup care takes precedence)
val backupPlacements: DateMap<DaycareId> = DateMap.of(
    backupResults.map { it.range to it.unitId }
)

val realizedPlacements = placements.setAll(backupPlacements)

// Later: get the unit for a specific date
val unitId = realizedPlacements.getValue(date) ?: return false
```

### Example 2: Building Status Timeline (Koski)

```kotlin
// Layer states by priority using chained set() calls
val statusTimeline = DateMap.of(placements.ranges(), PRESENT)
    .set(placements.gaps(), INTERRUPTED)
    .set(getHolidayDates().ranges(), HOLIDAY)
    .set(getInterruptedDates().ranges(), INTERRUPTED)
    .let {
        when (termination) {
            is Qualified -> it.set(FiniteDateRange(termination.date, LocalDate.MAX), QUALIFIED)
            is Resigned -> it.set(FiniteDateRange(termination.date, LocalDate.MAX), RESIGNED)
            null -> it
        }
    }

// Convert to timeline entries
val timeline = statusTimeline.entries()
    .map { (range, state) -> TimelineEntry(start = range.start, state = state) }
    .toList()
```

### The `transpose()` Operation

`transpose()` inverts a `DateMap<T>` into a `Map<T, DateSet>`, grouping all ranges by their value:

```kotlin
// Before transpose: { [2024-01-01,2024-01-31]: UnitA, [2024-02-01,2024-02-28]: UnitB, [2024-03-01,2024-03-31]: UnitA }
val placements: DateMap<DaycareId> = ...

// After transpose: { UnitA: {[2024-01-01,2024-01-31], [2024-03-01,2024-03-31]}, UnitB: {[2024-02-01,2024-02-28]} }
val placementsByUnit: Map<DaycareId, DateSet> = placements.transpose()
```

This is useful for:
- Grouping ranges by their assigned value
- Analyzing which dates have specific values
- Calculating total duration per value

## Time-based Variants

The same API is available for time-based collections with different precision:

- **`DateTimeSet` / `DateTimeMap<T>`**: Use `HelsinkiDateTimeRange` instead of `FiniteDateRange`, `HelsinkiDateTime` instead of `LocalDate`
- **`TimeSet`**: Use `TimeRange` instead of `FiniteDateRange`, `TimeRangeEndpoint` instead of `LocalDate`

## Common Patterns

### Folding Multiple DateMaps with Conflict Resolution

```kotlin
// Combine multiple DateMaps, resolving conflicts with a custom function
val combined = dataMaps.fold(DateMap.empty<T>()) { acc, map ->
    acc.update(map.entries()) { existing, new ->
        resolveConflict(existing, new)
    }
}
```

### Finding Coverage Gaps

```kotlin
// Find dates that should have values but don't
val expectedRange = FiniteDateRange(start, end)
val missingDates = placements.ranges()
    .let { DateSet.of(it) }
    .gaps(expectedRange)
```

### Filtering with Intersection

```kotlin
// Keep only dates that match certain criteria
val validRange = FiniteDateRange(validStart, validEnd)
val filtered = allDates.intersection(listOf(validRange))
```
