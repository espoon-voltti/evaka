<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Date & Time Utilities

Quick reference for eVaka's date and time utilities - custom classes that provide type-safe date/time handling across frontend and backend with automatic serialization.

**Key principles:**
- Always use these utilities instead of JavaScript Date
- Date ranges (DateRange, FiniteDateRange) use **inclusive end** semantics (end date is included in the range)
- Time ranges (TimeRange, TimeInterval, HelsinkiDateTimeRange) use **exclusive end** semantics (end time is not included)
- All date/time utilities are immutable - operations return new instances

## Core Types

### LocalDate

Immutable date without timezone (year, month, day). Frontend uses a TypeScript class, backend uses `java.time.LocalDate` with Kotlin extensions.

**Files:**
- Frontend: `frontend/src/lib-common/local-date.ts`
- Backend: `java.time.LocalDate` with extensions in `service/src/main/kotlin/fi/espoo/evaka/shared/domain/Time.kt`

### LocalTime

Time of day (hour:minute:second.nanosecond) without date or timezone. Frontend uses a TypeScript class, backend uses `java.time.LocalTime` with Kotlin extensions.

**Files:**
- Frontend: `frontend/src/lib-common/local-time.ts`
- Backend: `java.time.LocalTime` with extensions in `service/src/main/kotlin/fi/espoo/evaka/shared/domain/Time.kt`

### YearMonth

Year and month without day component. Frontend uses a TypeScript class, backend uses `java.time.YearMonth`.

**Files:**
- Frontend: `frontend/src/lib-common/year-month.ts`
- Backend: `java.time.YearMonth`

**Note:** Frontend YearMonth lacks comparison methods - convert to LocalDate if comparison is needed.

### HelsinkiDateTime

Timestamp in Europe/Helsinki timezone. Wraps a UTC timestamp (backend: `Instant`) but exposes date/time fields in Helsinki timezone.

**Files:**
- Frontend: `frontend/src/lib-common/helsinki-date-time.ts`
- Backend: `service/src/main/kotlin/fi/espoo/evaka/shared/domain/HelsinkiDateTime.kt`

## Range Types

**Common Operations:** See [Range Operations](#range-operations)

### DateRange

Date range with finite start and nullable end.

**Files:**
- Frontend: `frontend/src/lib-common/date-range.ts`
- Backend: `service/src/main/kotlin/fi/espoo/evaka/shared/domain/Time.kt` (data class)

**Structure:** `DateRange(start: LocalDate, end: LocalDate | null)`

**Type-Specific Methods:**
- `asFiniteDateRange()` - converts to FiniteDateRange if end is non-null

### FiniteDateRange

Date range with both start and end required.

**Files:**
- Frontend: `frontend/src/lib-common/finite-date-range.ts`
- Backend: `service/src/main/kotlin/fi/espoo/evaka/shared/domain/Time.kt` (data class implementing `BoundedRange`)

**Structure:** `FiniteDateRange(start: LocalDate, end: LocalDate)`

**Type-Specific Methods:**
- `dates()` - iterable sequence of all dates in the range
- `durationInDays()` - total number of days (inclusive)
- `asDateRange()` - converts to DateRange

### TimeRange

Time range with both start and end required.

**Files:**
- Frontend: `frontend/src/lib-common/time-range.ts`
- Backend: `service/src/main/kotlin/fi/espoo/evaka/shared/domain/TimeRange.kt` (data class)

**Structure:** `TimeRange(start: TimeRangeEndpoint.Start, end: TimeRangeEndpoint.End)` (convenience constructor accepts `LocalTime` values)

**Type-Specific Methods (frontend only):**
- `format()` - formats as "HH:mm–HH:mm" (en-dash, no spaces)

### TimeInterval

Time range with finite start and nullable end.

**Files:**
- Frontend: `frontend/src/lib-common/time-interval.ts`
- Backend: `service/src/main/kotlin/fi/espoo/evaka/shared/domain/TimeInterval.kt` (data class)

**Structure:** `TimeInterval(start: TimeRangeEndpoint.Start, end: TimeRangeEndpoint.End?)` (convenience constructor accepts `LocalTime` values)

**Type-Specific Methods:**
- `asTimeRange()` - converts to TimeRange if end is non-null

**Use when:** End time might be unknown or open-ended.

### TimeRangeEndpoint

Start/End endpoint markers for time ranges. Supports inclusive/exclusive semantics for range boundaries. Used internally by TimeRange/TimeInterval for boundary operations.

**Files:**
- Frontend: `frontend/src/lib-common/time-range-endpoint.ts` (namespace with functions)
- Backend: `service/src/main/kotlin/fi/espoo/evaka/shared/domain/TimeRangeEndpoint.kt` (sealed interface)

### HelsinkiDateTimeRange

Date and time range with both start and end required.

**Files:**
- Frontend: `frontend/src/lib-common/generated/api-types/shared.ts` (generated interface)
- Backend: `service/src/main/kotlin/fi/espoo/evaka/shared/domain/Time.kt` (data class)

**Structure:** `HelsinkiDateTimeRange(start: HelsinkiDateTime, end: HelsinkiDateTime)`

## Range Operations

This section documents operations that are shared across multiple range types.

### includes()

Checks if a single point (date/time/timestamp) is within the range. Not implemented for TimeInterval frontend or HelsinkiDateTimeRange frontend.

### overlaps()

Checks if two ranges have any overlap. Not implemented for TimeRange frontend and HelsinkiDateTimeRange frontend.

**Naming inconsistency:** Frontend `DateRange` uses `overlapsWith()` while other types use `overlaps()`. Backend consistently uses `overlaps()`. This is planned to be fixed.

### contains()

Checks if one range fully contains another range. Not implemented for TimeInterval (no `BoundedRange`).

### intersection()

Returns the overlapping portion of two ranges, or null if no overlap. Not implemented for TimeInterval backend and HelsinkiDateTimeRange frontend.

### Backend-Only Operations

The following operations are only available on the backend and are part of the `BoundedRange` interface:

- `gap()` - finds the gap between two non-overlapping ranges
- `merge()` - combines two overlapping or adjacent ranges
- `subtract()` - removes one range from another
- `relationTo()` - determines spatial relationship between ranges

**Available on:** FiniteDateRange, TimeRange, and HelsinkiDateTimeRange (types with required start and end). Not available on DateRange or TimeInterval (types with nullable end).

Additionally, `FiniteDateRange` has a `complement()` method that finds the inverse of a range (delegates to `subtract`).

These operations are used heavily by range-based collections (DateSet, DateMap, etc.). For detailed documentation, see [Range-based Collections](range-based-collections.md).

## Backend-Specific Features

### EvakaClock

Clock abstraction for testable time operations. Provides `today()` and `now()` methods that can be controlled during testing.

**Location:** `service/src/main/kotlin/fi/espoo/evaka/shared/domain/EvakaClock.kt`

**Critical E2E Testing Note:** EvakaClock can be controlled in the browser during E2E tests to set system time freely. This also affects `LocalDate.todayInHelsinkiTz()` and `HelsinkiDateTime.now()` in the frontend. Therefore, always use the injected `EvakaClock` instance in backend controllers to get current time, not `LocalDate.now()` or `HelsinkiDateTime.now()`.

**Example:**
```kotlin
@GetMapping("/current-date")
fun getCurrentDate(clock: EvakaClock): LocalDate {
    return clock.today()  // ✓ Correct - can be controlled in E2E tests
    // return LocalDate.now()  // ✗ Wrong - cannot be controlled in E2E tests
}
```

### Holidays

Finnish holiday calculations. Determines if a date is a Finnish public holiday.

**Location:** `service/src/main/kotlin/fi/espoo/evaka/shared/domain/Holidays.kt`

**Usage:** Used by `LocalDate.isHoliday()` extension function.

### OperationalDays

Determines child-specific operational days.

**Context**: Units are typically open Monday-Friday. Some are open on weekends/holidays too, but only for children with shift care allowed in their service need.

**Location:** `service/src/main/kotlin/fi/espoo/evaka/shared/domain/OperationalDays.kt`

### Range-Based Collections

Immutable data structures that optimize operations on date/time ranges. Use when working with many consecutive dates/times that share properties.

**Location:** `service/src/main/kotlin/fi/espoo/evaka/shared/data/`

**Types:** DateSet, DateMap, DateTimeSet, DateTimeMap, TimeSet

**Documentation:** For detailed usage patterns and examples, see [Range-based Collections](range-based-collections.md).

## Cross-Platform Serialization

All core types serialize automatically between Kotlin backend and TypeScript frontend.

JSON serialization uses ISO formats:
- Dates: `"2024-01-15"`
- Times: `"14:30:00"` or `"14:30:00.123456789"` (with nanoseconds)
- Timestamps: `"2024-01-15T14:30:00.000+02:00"`
- Ranges: `{ "start": "2024-01-01", "end": "2024-12-31" }`

## Database Integration

Jdbi has been configured to automatically map these types to appropriate SQL types (e.g. `LocalDate` to `DATE`, `FiniteDateRange` to `daterange`, etc.) with correct inclusive/exclusive semantics. This allows you to use these types directly in your SQL queries with safe parameter binding and result mapping.

**Example query construction:**
```kotlin
data class Placement(
    val id: PlacementId,
    val period: FiniteDateRange
)

val searchPeriod = FiniteDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))

// Database query - conversion happens automatically
createQuery {
    sql("""
    SELECT id, daterange(start_date, end_date, '[]') AS period
    FROM placement 
    WHERE daterange(start_date, end_date, '[]') && ${bind(searchPeriod)}
    """)
}.toList<Placement>()
```
