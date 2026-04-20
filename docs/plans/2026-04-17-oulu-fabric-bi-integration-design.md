<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Oulu Fabric BI Integration — Design

Date: 2026-04-17
Status: Design approved, pending PR #8884 merge before implementation.

## 1. Overview and Goals

### Problem

Oulu has chosen Microsoft Fabric as their data warehouse and wants eVaka to push extracts there via SFTP (Fabric ingests from an Oulu-hosted, Azure-backed SFTP endpoint). The existing Oulu "Fabric" code path in eVaka (`service/.../oulu/dw/`) was never deployed and no longer matches the target approach. Tampere already operates a working BI extraction (44 tables, daily, CSV-to-S3) that is about to land in evaka via PR #8884. Oulu's requirements are close enough to Tampere's that the two should converge.

### Goal

After PR #8884 merges, refactor Tampere's BI module into a shared `evaka.bi` layer that both Tampere and Oulu subscribe to via a small per-municipality configuration. Build Oulu's SFTP output on that foundation.

### Per-municipality configuration (minimal)

```kotlin
data class BiExportConfig(
    val includePII: Boolean,
    val includeLegacyColumns: Boolean = true,
    val windowMonths: Int? = 3,
    val excludedTables: Set<BiTable> = emptySet(),
)
```

- Tampere: `includePII = true`, `includeLegacyColumns = true`, `windowMonths = null` (no filter), `excludedTables = emptySet()` (or opt-out of selected Oulu-requested tables).
- Oulu: `includePII = false`, `includeLegacyColumns = false`, `windowMonths = 3` (24 during backfill), `excludedTables = emptySet()`.

Output destination (S3 vs SFTP, packaging) is selected via Spring bean wiring per municipality, not via `BiExportConfig`.

### Non-goals

- Not redesigning Tampere's destination (stays S3, stays zipped CSV).
- Not introducing per-column projection config beyond the `includePII` / `includeLegacyColumns` toggles.
- Not rewriting scheduled-job or async-job infrastructure.
- Not touching Oulu's invoice SFTP utilities — separate concern.

### Phasing

1. Wait for PR #8884 (trevaka → evaka) to merge.
2. Refactor BI into a shared module in evaka, add `@Pii` / `@LegacyColumn` annotations, `BiExportConfig`, pluggable output client. Tampere's behavior preserved.
   - 2a. Extend the shared catalog with Oulu-required tables (starting with `staff_attendance_realtime`). Tampere opts out via `excludedTables` if they don't want the new tables.
3. Delete the existing Oulu Fabric/FabricHistory code paths (meeting note step 1).
4. Add Oulu SFTP destination + Oulu wiring.
5. Reaktor ↔ Oulu key / host-key exchange (meeting notes steps 3–4); run in staging.
6. Cutover to prod; initial 2-year backfill via `OULU_BI_WINDOW_MONTHS=24`, then restore to 3.
7. After old DW consumer is decommissioned, remove remaining `DwQuery` pathway + config (meeting note step 7).

## 2. Module Layout and Shared Surface

New shared module at `service/src/main/kotlin/fi/espoo/evaka/bi/`:

| File | Responsibility |
|---|---|
| `BiTable.kt` | Enum of all catalog tables. Each entry holds `fileName`, `CsvQuery`, and a `QueryKind` (Reference, Temporal, RangeBearing). |
| `BiModels.kt` | Shared data classes (`BiPerson`, `BiPlacement`, …). Fields tagged with `@Pii` or `@LegacyColumn` where applicable. |
| `BiQueries.kt` | SQL definitions. Temporal and RangeBearing queries accept a `since: LocalDate?` parameter; Reference queries ignore it. |
| `BiExportConfig.kt` | `BiExportConfig` data class plus `@Pii` and `@LegacyColumn` annotation definitions. |
| `BiCsvUtils.kt` | CSV serializer. Omits columns tagged with `@Pii` when `includePII = false`, and columns tagged with `@LegacyColumn` when `includeLegacyColumns = false`. |
| `BiExportClient.kt` | Interface: `sendBiTable(tableName, date, InputStream)`. Implementations per municipality. |
| `BiExportJob.kt` | Shared job handler: resolves config, computes window, runs query, serializes, invokes client. |

Per-municipality packages hold only glue:

- `tampere/bi/` (moves from trevaka via #8884, then refactored) — `TampereBiExportConfig` bean, `TampereBiExportClient : BiExportClient` (S3, zipped CSV, unchanged behavior), `TampereScheduledJob.PlanBiExportJobs`.
- `oulu/bi/` (new) — `OuluBiExportConfig` bean, `OuluBiSftpExportClient : BiExportClient` (uses `shared/sftp/SftpClient`), `OuluScheduledJob.PlanBiExportJobs`.

Output client interface:

```kotlin
interface BiExportClient {
    fun sendBiTable(tableName: String, date: LocalDate, data: InputStream)
}
```

Annotations:

```kotlin
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Pii

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class LegacyColumn  // kept for downstream schema stability, no current eVaka source
```

## 3. PII Tagging and Column Handling

### What gets tagged `@Pii`

Direct identifiers on entity data classes:

- `BiPerson`: `firstName`, `lastName`, `ssn`, `email`, `phone`, `backupPhone`, `invoicingStreetAddress`, `invoicingPostalCode`, `invoicingPostOffice`.
- `BiEmployee`: `firstName`, `lastName`, `email`.
- `BiGuardian`: same set as `BiPerson`.
- `BiApplicationForm`: `document` JSON blob — tag the whole field.

Sensitive free-text:

- `BiDecision`, `BiFeeDecision`, `BiVoucherValueDecision`: rationale / justification fields (`perustelutekstit`).
- `BiAssistanceNeedDecision`: free-form reasoning text.
- `BiAssistanceAction`: description / "other action" free-text fields.

### NOT tagged (always exported)

- `id`, foreign keys (`personId`, `unitId`, …).
- `streetAddress`, `postalCode`, `postOffice` on `BiPerson` — home address kept for Oulu too (explicit scope).
- Dates, amounts, enum codes, counts.
- Audit timestamps (`createdAt`, `updatedAt`).

### `@LegacyColumn`

Applied to properties that correspond to always-null `NULL AS ...` selects kept for Tampere's downstream schema stability (e.g., `BiPerson.enabledEmailTypes`). Acts as documentation for why a column is null and as a gate on serialization.

### Serialization behavior

`BiCsvUtils` reads each data class via Kotlin reflection. For each property:

- `@Pii` present AND `includePII = false` → **column omitted from CSV entirely** (not emptied).
- `@LegacyColumn` present AND `includeLegacyColumns = false` → column omitted from CSV entirely.
- Otherwise → value serialized normally.

Each municipality gets a stable schema within its own runs; schemas may differ between municipalities, which is acceptable since each municipality's DW only ingests its own files.

### Audit regression test

`BiCsvUtilsTest` iterates every `BiTable`, generates output with `includePII = false`, and asserts that no header names a `@Pii`-tagged property. Cheap guard against future PII leaks.

## 4. Query Kinds and Windowing

Every `BiTable` entry declares a `QueryKind`:

```kotlin
enum class BiTable(
    val fileName: String,
    val query: CsvQuery,
    val kind: QueryKind,
) {
    Person("person",                    BiQueries.getPerson,             QueryKind.Reference),
    Placement("placement",              BiQueries.getPlacements,         QueryKind.RangeBearing),
    ChildAttendance("child_attendance", BiQueries.getChildAttendances,   QueryKind.Temporal),
    Absence("absence",                  BiQueries.getAbsences,           QueryKind.Temporal),
    StaffAttendanceRealtime("staff_attendance_realtime",
                                        BiQueries.getStaffAttendanceRealtime,
                                                                         QueryKind.Temporal),
    // ...
}

sealed interface QueryKind {
    data object Reference : QueryKind      // full snapshot
    data object Temporal : QueryKind       // filters by event date
    data object RangeBearing : QueryKind   // filters by daterange overlap
}

typealias CsvQuery = (since: LocalDate?) -> StreamingCsvQuery<*>
```

### How each kind is processed

- **Reference** (e.g., `person`, `daycare`, `fee_thresholds`): full snapshot. No WHERE window applied.
- **Temporal** (e.g., `child_attendance`, `absence`, `staff_attendance_realtime`): `WHERE date >= :since`.
- **RangeBearing** (e.g., `placement`, `service_need`, `fee_decision`): overlap semantics, `WHERE daterange(start_date, end_date, '[]') && daterange(:since, null, '[]')`.

Overlap semantics mean a placement starting 3 years ago and still active today is included — it's currently relevant.

### Window resolution

```kotlin
val since = config.windowMonths?.let { clock.today().minusMonths(it.toLong()) }
```

- Tampere: `windowMonths = null` → `since = null` → queries run without filter (current Tampere behavior preserved exactly).
- Oulu: `windowMonths = 3` → `since = today - 3 months` → Temporal and RangeBearing queries filter; Reference queries ignore the parameter.

### Backfill (Oulu)

Set `OULU_BI_WINDOW_MONTHS=24`, redeploy, wait for the next scheduled run (or run again after it), then restore to 3. Reference queries re-emit full snapshots (harmless idempotent). Temporal / RangeBearing emit 2 years of history.

Rerunnable: can be run more than once with longer windows if needed during reconciliation.

## 5. Output Clients

### Tampere

`tampere/bi/TampereBiExportClient.kt` — unchanged from current `FileBiExportS3Client` after it moves into evaka via #8884. Zips CSV into `{table}_{date}.zip` with a single `{table}.csv` entry; writes to S3 at `{prefix}/{table}_{date}.zip`.

### Oulu

`oulu/bi/OuluBiSftpExportClient.kt` — new. Same zip packaging as Tampere (shared CSV handling stays uniform), pushes via SFTP to Oulu's server:

```kotlin
class OuluBiSftpExportClient(
    private val sftp: SftpClient,           // shared/sftp/SftpClient (key auth + host-key verification)
    private val remoteBasePath: String,
) : BiExportClient {
    override fun sendBiTable(tableName: String, date: LocalDate, data: InputStream) {
        val zipped = zipCsv(tableName, data)
        sftp.put("$remoteBasePath/${tableName}_${date}.zip", zipped)
    }
}
```

Uses `service/.../shared/sftp/SftpClient.kt` (key-auth, read-only `known_hosts`). **Not** the invoice SFTP (`oulu/invoice/service/SftpConnector.kt`) — that one uses password auth with strict-host-key disabled.

### Filename convention

`{tableName}_{YYYY-MM-DD}.zip` — matches Tampere. Date is the run date, not the window start.

### Oulu configuration

```yaml
# application-oulu_evaka.yaml (per-environment overrides)
evakaoulu:
  bi:
    window_months: 3
    sftp:
      address: sftp.oulu-fabric.example
      port: 22
      username: evaka
      private_key_path: /etc/evaka/oulu-bi-sftp.key
      host_key: <SHA256 fingerprint from Oulu>
      remote_path: /upload
```

Private key is Reaktor-generated (meeting note step 3); public key delivered to Oulu. Host key received from Oulu (step 4) is pinned in config — the shared SFTP client refuses connection on mismatch.

### Environment separation (dev/staging vs prod)

Staging and prod eVaka deployments each load their own Spring profile with their own `evakaoulu.bi.sftp.*` values, pointing at Oulu's test SFTP (Fabric dev blob) vs prod SFTP (Fabric prod blob). No code change — separate config secrets per environment.

### Failure mode

SFTP push failure propagates as async-job failure; retried per evaka's existing async-job machinery. No intermediate S3 buffering for Oulu — Azure blob is on Oulu's side; we don't own a staging bucket for Oulu BI.

## 6. Scheduling, Job Registration, Observability

### Scheduled jobs

```kotlin
// tampere/TampereScheduledJobs.kt — moves in via #8884
TampereScheduledJob.PlanBiExportJobs   // daily at 01:00 UTC (current)

// oulu/OuluScheduledJobs.kt — new
OuluScheduledJob.PlanBiExportJobs      // daily at 02:00 UTC (staggered from Tampere; configurable)
```

Both planning jobs enumerate `BiTable.entries`, filter by `config.excludedTables`, and enqueue one async task per remaining table.

### Async job types

```kotlin
// TampereAsyncJob.SendBiTable(table: BiTable)   — existing
// OuluAsyncJob.SendBiTable(table: BiTable)      — new
```

Each has its own `AsyncJobRunner<T>` pool with concurrency = 1. Handlers delegate to the shared `BiExportJob.run(tx, table, config, client)`. Separate job types per municipality matches existing evaka conventions and keeps queue state per-municipality.

### Planning job (pseudocode)

```kotlin
fun planBiExportJobs(db, clock, config, asyncRunner) {
    db.transaction { tx ->
        asyncRunner.clearUnclaimed(tx, SendBiTable::class)
        BiTable.entries
            .filterNot { it in config.excludedTables }
            .forEach { tx.enqueue(SendBiTable(it)) }
    }
}
```

### Shared export job

```kotlin
fun BiExportJob.run(tx, table, config, client) {
    val since = config.windowMonths?.let { clock.today().minusMonths(it.toLong()) }
    val query = table.query(since)
    val csvStream = BiCsvUtils.stream(query, config)   // applies @Pii / @LegacyColumn filtering
    client.sendBiTable(table.fileName, clock.today(), csvStream)
}
```

### Observability

- Per-async-job INFO log: table, row count, window, duration, destination result.
- SFTP failure: ERROR with host, remote path, exception class (no credentials).
- Existing async-job metrics (success/failure counters per job type); no new metrics infra.
- 2-year backfill watched via application logs — rare, attended operation, no extra tooling.

No admin endpoint for backfill — `OULU_BI_WINDOW_MONTHS` env flip + redeploy is the path.

## 7. Cleanup Plan

### Step 1 — immediate, delete Fabric/FabricHistory pathway

`service/src/main/kotlin/fi/espoo/evaka/oulu/dw/`:

- `DwQuery.kt` → remove `FabricQuery`, `FabricHistoryQuery` enums; keep `DwQuery`.
- `DwQueries.kt` → remove `FabricQueries` object and helpers used only by it.
- `DwModels.kt` → remove all `FabricXxx` data classes.
- `DwExportJob.kt` → remove `sendFabricQuery()`, `sendFabricHistoryQuery()`.

`service/src/main/kotlin/fi/espoo/evaka/oulu/`:

- `OuluAsyncJob.kt` → remove `SendFabricQuery`, `SendFabricHistoryQuery` cases and registrations.
- `OuluScheduledJobs.kt` → remove `PlanFabricExportJobs`, `PlanFabricHistoryJobs` and planning functions.
- `OuluConfig.kt` → remove any bean wiring specific to Fabric.
- `application-oulu_evaka.yaml` → remove any Fabric-specific local overrides if present.

No migrations — nothing was deployed. Verify no external references via repo-wide grep.

### Step 2 — Oulu BI buildout

Covered in sections 2–6.

### Step 7 — deferred, delete remaining `DwQuery` pathway

Runs only after Oulu's Fabric BI has been stable in prod and the old DW consumer is decommissioned on Oulu's side (business decision).

- Delete `oulu/dw/` package entirely: `DwQuery.kt`, `DwQueries.kt`, `DwModels.kt`, `DwExportJob.kt`, `FileDwExportClient.kt`, `DwCsvUtils.kt`.
- Remove `SendDWQuery` from `OuluAsyncJob.kt` and its registration.
- Remove `PlanDwExportJobs` from `OuluScheduledJobs.kt` and its planning function.
- Remove `evakaoulu.bucket.export`, `evakaoulu.dw_export.*` from `OuluProperties.kt` and YAML (including old-server SFTP credentials).
- Remove the old S3 bucket and SFTP server from infrastructure (separate IaC repo).

Invoice SFTP utilities (`oulu/invoice/service/SftpConnector.kt`, `SftpSender.kt`) stay — they serve invoice integrations, unrelated.

## 8. Testing

### Unit tests — shared module

- `BiCsvUtilsTest`:
  - always-include fields serialize normally.
  - `@Pii` property: included when `includePII = true`, omitted when `false`.
  - `@LegacyColumn` property: included when `includeLegacyColumns = true`, omitted when `false`.
  - Audit test: for every `BiTable`, generate output with `includePII = false` and assert no header names a `@Pii`-tagged property.
- `BiExportJobTest`:
  - `windowMonths = null` → no WHERE filter in generated SQL.
  - `windowMonths = 3` → `since = today - 3 months`.
  - Reference queries receive `null` for `since` regardless.

### Integration tests — per query

One test per `BiTable` entry in `BiQueriesIntegrationTest` (`PureJdbiTest` against real Postgres fixture). Seeds edge cases, runs the query, asserts row count and key columns.

Range-bearing overlap edge cases:

- Placement ending before window → excluded.
- Placement starting after window → included.
- Placement spanning the entire window → included.
- Open-ended placement (`end_date = null`) → included if `start_date <= today`.

### Integration tests — per municipality

- `TampereBiExportJobTest` — adapted post-#8884 to use shared module. Asserts `includePII = true` and no window filter.
- `OuluBiExportJobTest` — new. Asserts `includePII = false`, 3-month window, and no `@Pii`-tagged column appears in any generated CSV.

### SFTP client testing

`OuluBiSftpExportClientTest` using existing SFTP test-server pattern (Apache MINA SSHD in evaka's test deps — verify during implementation). Covers happy-path upload, host-key mismatch rejection, connection failure propagation.

### Not tested in code (operational)

- Actual connectivity to Oulu's real SFTP — meeting step 5 (staging smoke) and step 6 (prod cutover).
- 2-year backfill performance — smoke-tested in staging against realistic volumes before the prod run.
