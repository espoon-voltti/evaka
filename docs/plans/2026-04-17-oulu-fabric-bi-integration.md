# Oulu Fabric BI Integration — Implementation Plan

**Goal:** Ship an Oulu Fabric BI integration that pushes zipped-CSV extracts via SFTP to Oulu's Fabric ingest endpoint, reusing Tampere's BI query catalog via a new shared `evaka.core.bi` module with a small `BiExportConfig` (`includePII`, `includeLegacyColumns`, `windowMonths`, `excludedTables`). Preserve Tampere's existing behavior exactly.

**Architecture:** Extract Tampere's `evaka.instance.tampere.bi` into a shared `evaka.core.bi` module. Add `@Pii` / `@LegacyColumn` annotations that `BiCsvUtils` honors against `BiExportConfig`. Add `QueryKind` (Reference, Temporal, RangeBearing) and window-parameterized queries. Tampere wires `includePII=true, windowMonths=0` (full snapshots, no filter) — bit-for-bit behavior preserved. Oulu wires `includePII=false, windowMonths=3`, an SFTP-based `BiExportClient`, and Oulu-specific async/scheduled jobs.

**Tech Stack:** Kotlin, Spring Boot, Jdbi3, JSch (SFTP via existing `evaka.core.shared.sftp.SftpClient`), JUnit5, Apache MINA SSHD (for SFTP integration tests — verify availability in test deps during Task 13).

**Design doc:** `docs/plans/2026-04-17-oulu-fabric-bi-integration-design.md`

**Meeting source:** Oulu Fabric DW integration meeting, April 2026.

**Execution model:** User drives implementation interactively and commits. This document's code blocks and commands are reference material; the user points the assistant at each step as they work through it.

---

## Prerequisites

- PR #8884 (trevaka → evaka) is merged to `master` (confirmed at commit `71c7a27d49`).
- Tampere BI module currently lives at `service/src/main/kotlin/evaka/instance/tampere/bi/` (7 files: `BiCsvUtils`, `BiExportClient`, `BiExportJob`, `BiModels`, `BiQueries`, `BiTable`, `FileBiExportS3Client`).
- Existing Oulu DW code (to be partly deleted in Task 1, fully deleted later in a separate PR) lives at `service/src/main/kotlin/evaka/instance/oulu/dw/`.
- Shared SFTP client at `service/src/main/kotlin/evaka/core/shared/sftp/SftpClient.kt`; `SftpEnv` at `service/src/main/kotlin/evaka/core/EvakaEnv.kt:624`.
- Baseline test run should be green before starting:

  ```bash
  cd service && ./gradlew test --tests "*Bi*" --tests "*Tampere*" --tests "*Oulu*"
  ```

  If red on master, stop and investigate — the plan assumes a green baseline.

---

## Task 1: Delete existing Oulu Fabric / FabricHistory code

**Context:** The meeting step 1 calls for deleting the never-deployed Oulu Fabric transfer. The old `DwQuery` pathway stays — it's the existing ETL-DW exporter that continues running until Oulu cuts over (meeting step 7).

**Files to modify:**
- `service/src/main/kotlin/evaka/instance/oulu/dw/DwQuery.kt` — remove `FabricQuery`, `FabricHistoryQuery` enums; keep `DwQuery`.
- `service/src/main/kotlin/evaka/instance/oulu/dw/DwQueries.kt` — remove `FabricQueries` object (and any helpers used only by it).
- `service/src/main/kotlin/evaka/instance/oulu/dw/DwModels.kt` — remove all `FabricXxx` data classes.
- `service/src/main/kotlin/evaka/instance/oulu/dw/DwExportJob.kt` — remove `sendFabricQuery()` and `sendFabricHistoryQuery()`.
- `service/src/main/kotlin/evaka/instance/oulu/OuluAsyncJob.kt` — remove `SendFabricQuery`, `SendFabricHistoryQuery` cases and their registrations.
- `service/src/main/kotlin/evaka/instance/oulu/OuluScheduledJobs.kt` — remove `PlanFabricExportJobs`, `PlanFabricHistoryJobs` enum cases and the `planFabricJobs` / `planFabricHistoryJobs` functions.

**Step 1:** Read each of the 6 files to locate the Fabric/FabricHistory-specific code, and `grep -r "FabricQuery\|FabricHistoryQuery\|sendFabricQuery\|sendFabricHistoryQuery\|PlanFabricExportJobs\|PlanFabricHistoryJobs\|SendFabricQuery\|SendFabricHistoryQuery\|FabricQueries\|Fabric[A-Z]"` across the whole repo to ensure no external references remain after deletion. Scope includes tests and YAML.

**Step 2:** Delete the code. For `DwExportJob.kt`, also delete any helper functions used only by the Fabric paths. For `OuluAsyncJob`, update the pool's registered set so it contains only `SendDWQuery::class`.

**Step 3:** Verify compilation:

```bash
cd service && ./gradlew compileKotlin compileTestKotlin
```

Expected: green.

**Step 4:** Verify no orphans:

```bash
grep -r "Fabric" service/src/main/kotlin/evaka/instance/oulu/ service/src/test/kotlin/evaka/instance/oulu/ service/src/integrationTest/kotlin/evaka/instance/oulu/
```

Expected: no matches (or only in `evaka.instance.oulu.invoice.*` if there happens to be anything unrelated — review manually).

**Step 5:** Run Oulu tests:

```bash
./gradlew test --tests "*Oulu*"
```

Expected: green.

---

## Task 2: Extract shared BI module — file moves

**Context:** Move the BI module from Tampere-specific to shared, keeping every file's content unchanged. Only package declarations and imports change. Goal: bit-for-bit Tampere behavior after Task 2 finishes.

**Files to create (new locations):**
- `service/src/main/kotlin/evaka/core/bi/BiTable.kt`
- `service/src/main/kotlin/evaka/core/bi/BiQueries.kt`
- `service/src/main/kotlin/evaka/core/bi/BiModels.kt`
- `service/src/main/kotlin/evaka/core/bi/BiCsvUtils.kt`
- `service/src/main/kotlin/evaka/core/bi/BiExportJob.kt`
- `service/src/main/kotlin/evaka/core/bi/BiExportClient.kt`

**Files to delete (old locations):** the five non-S3-specific files above at `service/src/main/kotlin/evaka/instance/tampere/bi/` — but NOT `FileBiExportS3Client.kt` (Tampere-specific implementation of the shared interface; it stays in the Tampere package).

**Step 1:** `git mv` each of the 6 source files into `service/src/main/kotlin/evaka/core/bi/`. Use `git mv` so git tracks the rename.

**Step 2:** For each moved file, change the package declaration:

```kotlin
package evaka.instance.tampere.bi
```

→

```kotlin
package evaka.core.bi
```

Use `sed -i '' 's|^package evaka\.instance\.tampere\.bi$|package evaka.core.bi|' service/src/main/kotlin/evaka/core/bi/*.kt`.

**Step 3:** In `BiExportJob.kt`, the import `import evaka.instance.tampere.TampereAsyncJob` must be removed, and the existing `fun sendBiTable(db, clock, msg: TampereAsyncJob.SendBiTable)` overload must be moved out of the shared file. Move that overload into a new file at `service/src/main/kotlin/evaka/instance/tampere/bi/TampereBiExportJob.kt` as an **extension function** on the shared `evaka.core.bi.BiExportJob`:

```kotlin
// SPDX-FileCopyrightText: 2024 Tampere region
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.bi

import evaka.core.bi.BiExportJob
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.instance.tampere.TampereAsyncJob

fun BiExportJob.sendBiTable(
    db: Database.Connection,
    clock: EvakaClock,
    msg: TampereAsyncJob.SendBiTable,
) = sendBiTable(db, clock, msg.table.fileName, msg.table.query)
```

**Step 4:** Update `TampereAsyncJob.kt` import and registration. Change:

```kotlin
import evaka.instance.tampere.bi.BiExportJob
```

to

```kotlin
import evaka.core.bi.BiExportJob
import evaka.instance.tampere.bi.sendBiTable
```

The `registerHandler(it::sendBiTable)` call keeps working because the extension is resolvable in this scope.

**Step 5:** Update `FileBiExportS3Client.kt` imports. The `package evaka.instance.tampere.bi` declaration stays; change the `import` of `BiExportClient` to `evaka.core.bi.BiExportClient`.

**Step 6:** Update all other references. In particular:
- `TampereScheduledJobs.kt` — change `import evaka.instance.tampere.bi.BiTable` to `import evaka.core.bi.BiTable`.
- Any test files — grep for `evaka.instance.tampere.bi.Bi` and update imports.

Run `grep -rn "evaka\.instance\.tampere\.bi\." service/src/ | grep -v "sendBiTable\|FileBiExportS3Client"` and fix every import found.

**Step 7:** Compile:

```bash
./gradlew compileKotlin compileTestKotlin
```

Expected: green.

**Step 8:** Run Tampere tests and the integration test suite touching BI:

```bash
./gradlew test --tests "*Bi*" --tests "*Tampere*"
./gradlew integrationTest --tests "*Bi*" --tests "*Tampere*"
```

Expected: green.

---

## Task 3: Introduce BiExportConfig data class and @Pii / @LegacyColumn annotations

**Context:** Add the config type and annotations. No behavior change yet — `BiCsvUtils` still serializes every column. The flags are wired in Task 4.

**Files to create:**
- `service/src/main/kotlin/evaka/core/bi/BiExportConfig.kt`

**Step 1:** Write the new file:

```kotlin
// SPDX-FileCopyrightText: 2026 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Pii

/**
 * Column retained for downstream DW schema stability even though the
 * underlying eVaka data has been removed (e.g., field dropped during refactor).
 * Controlled independently from @Pii via BiExportConfig.includeLegacyColumns.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class LegacyColumn

data class BiExportConfig(
    val includePII: Boolean,
    val includeLegacyColumns: Boolean = true,
    val windowMonths: Int = 0,
    val excludedTables: Set<BiTable> = emptySet(),
)
```

**Step 2:** Compile:

```bash
./gradlew compileKotlin
```

Expected: green.

---

## Task 4: Make BiCsvUtils honor @Pii and @LegacyColumn (TDD)

**Context:** `toCsvRecords` currently serializes every `declaredMemberProperties`. Change it to skip any property annotated with `@Pii` (when `includePII=false`) or `@LegacyColumn` (when `includeLegacyColumns=false`). Columns are omitted entirely (not emptied), per design decision.

**Files:**
- Modify: `service/src/main/kotlin/evaka/core/bi/BiCsvUtils.kt`
- Test (create): `service/src/test/kotlin/evaka/core/bi/BiCsvUtilsTest.kt`

**Step 1: Write the failing tests.** Create `BiCsvUtilsTest.kt`:

```kotlin
// SPDX-FileCopyrightText: 2026 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class BiCsvUtilsTest {

    data class Sample(
        val id: Int,
        @Pii val name: String,
        @LegacyColumn val legacy: String?,
        val keep: String,
    )

    private val rows = sequenceOf(Sample(1, "Alice", null, "K"))

    private fun render(config: BiExportConfig): List<String> =
        toCsvRecords(::convertToCsv, Sample::class, rows, config).toList()

    @Test
    fun `includes all columns when includePII=true and includeLegacyColumns=true`() {
        val out = render(BiExportConfig(includePII = true, includeLegacyColumns = true))
        assertEquals("id,name,legacy,keep\r\n", out[0])
        assertEquals("1,Alice,,K\r\n", out[1])
    }

    @Test
    fun `omits @Pii columns when includePII=false`() {
        val out = render(BiExportConfig(includePII = false, includeLegacyColumns = true))
        assertEquals("id,legacy,keep\r\n", out[0])
        assertEquals("1,,K\r\n", out[1])
    }

    @Test
    fun `omits @LegacyColumn columns when includeLegacyColumns=false`() {
        val out = render(BiExportConfig(includePII = true, includeLegacyColumns = false))
        assertEquals("id,name,keep\r\n", out[0])
        assertEquals("1,Alice,K\r\n", out[1])
    }

    @Test
    fun `omits both categories when both flags are false`() {
        val out = render(BiExportConfig(includePII = false, includeLegacyColumns = false))
        assertEquals("id,keep\r\n", out[0])
        assertEquals("1,K\r\n", out[1])
    }
}
```

**Step 2: Run test to verify it fails:**

```bash
./gradlew test --tests "evaka.core.bi.BiCsvUtilsTest"
```

Expected: FAIL with "unresolved reference: toCsvRecords" overload mismatch, because `toCsvRecords` doesn't yet accept `BiExportConfig`.

**Step 3: Change `toCsvRecords` signature and body.** In `BiCsvUtils.kt`:

```kotlin
fun <T : Any> toCsvRecords(
    converter: (value: Any?) -> String,
    clazz: KClass<T>,
    values: Sequence<T>,
    config: BiExportConfig,
): Sequence<String> {
    check(clazz.isData)
    val props = clazz.declaredMemberProperties
        .filter { prop ->
            val isPii = prop.annotations.any { it is Pii }
            val isLegacy = prop.annotations.any { it is LegacyColumn }
            (!isPii || config.includePII) && (!isLegacy || config.includeLegacyColumns)
        }
        .toList()
    val header = props.joinToString(CSV_FIELD_SEPARATOR, postfix = CSV_RECORD_SEPARATOR) { it.name }
    return sequenceOf(header) +
        values.map { record ->
            props.joinToString(CSV_FIELD_SEPARATOR, postfix = CSV_RECORD_SEPARATOR) {
                CsvEscape.escapeCsv(converter(it.get(record)))
            }
        }
}
```

Note: Kotlin reflection sometimes exposes property-targeted annotations via `javaField?.annotations` rather than `prop.annotations`. Use `prop.annotations` first — if the test fails because annotations aren't seen, switch to `(prop.javaField?.annotations ?: emptyArray())`. Verify empirically.

**Step 4: Run the tests again:**

```bash
./gradlew test --tests "evaka.core.bi.BiCsvUtilsTest"
```

Expected: PASS. If the `@Pii` / `@LegacyColumn` annotations are not seen by `prop.annotations`, replace with `prop.javaField?.annotations?.toList() ?: emptyList()`.

**Step 5: Update call sites.** `BiExportJob` and anything else calling the old 3-arg `toCsvRecords` must pass a `BiExportConfig`. For now, Tampere passes `BiExportConfig(includePII = true)`. To keep this task tight, do a minimal plumbing change:

- Change `class BiExportJob(private val client: BiExportClient)` to `class BiExportJob(private val client: BiExportClient, private val config: BiExportConfig)`.
- `StreamingCsvQuery` (in `BiQueries.kt`) currently passes `clazz` and the sequence to `toCsvRecords`. Change its `stream` function to also accept config. Look at the existing `csvQuery` DSL in `BiQueries.kt` around line 466 (`private inline fun <reified T : Any> csvQuery(...)`). Adapt the call chain so the config flows from `BiExportJob` into `toCsvRecords`.

**Step 6: Compile and run the whole test suite:**

```bash
./gradlew compileKotlin compileTestKotlin
./gradlew test --tests "evaka.core.bi.*" --tests "*Tampere*"
```

Expected: green. Tampere tests pass because Tampere still passes `includePII=true, includeLegacyColumns=true` (defaulting) — no behavior change.

---

## Task 5: Add audit regression test for PII leaks

**Context:** Iterate every `BiTable`, and assert that with `includePII=false` and `includeLegacyColumns=false`, the generated CSV header exactly matches the set of non-`@Pii`, non-`@LegacyColumn` data-class properties. A regression signal for accidental annotation strips or ordering drift.

**Files:**
- Test (create): `service/src/test/kotlin/evaka/core/bi/BiCsvUtilsAuditTest.kt`

**Step 1: Write the test:**

```kotlin
// SPDX-FileCopyrightText: 2026 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class BiCsvUtilsAuditTest {
    @Test
    fun `generated header for every BiTable excludes @Pii and @LegacyColumn properties when both flags are false`() {
        for (table in BiTable.entries) {
            val modelClass = modelClassOf(table)
            val expected = modelClass.declaredMemberProperties
                .filter { prop ->
                    val anns = (prop.javaField?.annotations ?: prop.annotations).toList()
                    anns.none { it is Pii || it is LegacyColumn }
                }
                .map { it.name }
                .joinToString(CSV_FIELD_SEPARATOR, postfix = CSV_RECORD_SEPARATOR)

            val header = renderHeader(table, BiExportConfig(includePII = false, includeLegacyColumns = false))
            assertEquals(expected, header, "BiTable.${table.name} header mismatch")
        }
    }

    // helper to pull the data class bound to each BiTable entry; may require
    // adding a `val modelClass: KClass<*>` to CsvQuery (see Step 3).
    private fun modelClassOf(table: BiTable): kotlin.reflect.KClass<*> =
        table.query.modelClass

    private fun renderHeader(table: BiTable, config: BiExportConfig): String {
        @Suppress("UNCHECKED_CAST")
        val clazz = table.query.modelClass as kotlin.reflect.KClass<Any>
        return toCsvRecords(::convertToCsv, clazz, emptySequence(), config).first()
    }
}
```

**Step 2: Run test to verify it fails (compile error):**

```bash
./gradlew test --tests "evaka.core.bi.BiCsvUtilsAuditTest"
```

Expected: FAIL with unresolved `modelClass` on `CsvQuery`.

**Step 3: Make it executable.**

`CsvQuery` currently doesn't expose its `T::class`. Modify `BiQueries.CsvQuery` (now `evaka.core.bi.CsvQuery`) to expose the model class. Update `StreamingCsvQuery` and the `csvQuery<T>` DSL so `T::class` is retained:

```kotlin
interface CsvQuery {
    val modelClass: KClass<*>
    operator fun <R> invoke(tx: ..., use: (Sequence<String>) -> R): R
}

class StreamingCsvQuery<T : Any>(
    override val modelClass: KClass<T>,
    private val sql: QuerySql,
) : CsvQuery { ... }
```

Also pass the `KClass` in `csvQuery<T>` helper:

```kotlin
private inline fun <reified T : Any> csvQuery(
    noinline builder: QuerySql.Builder.() -> Unit,
): CsvQuery = StreamingCsvQuery(T::class, QuerySql.of(builder))
```

**Step 4: Run tests:**

```bash
./gradlew test --tests "evaka.core.bi.*"
```

Expected: PASS. The test confirms every `BiTable`'s header, when both flags are false, consists exactly of the non-annotated properties.

---

## Task 6: Add QueryKind sealed interface and update BiTable

**Context:** Introduce the three query kinds. No filtering logic yet — just mark each table with a kind. Queries still ignore the marker until Task 7.

**Files:**
- Modify: `service/src/main/kotlin/evaka/core/bi/BiTable.kt`
- Create: `service/src/main/kotlin/evaka/core/bi/QueryKind.kt`

**Step 1: Write the new file:**

```kotlin
// SPDX-FileCopyrightText: 2026 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

sealed interface QueryKind {
    /** Entity / reference table, exported as a full snapshot on every run. */
    data object Reference : QueryKind

    /** Row belongs to a single date (e.g., an attendance row on 2026-04-17). */
    data object Temporal : QueryKind

    /** Row covers a date range — uses overlap semantics when filtering. */
    data object RangeBearing : QueryKind
}
```

**Step 2: Add `kind: QueryKind` parameter to the `BiTable` enum.** Default is `Reference` — preserves today's behavior and minimizes the change. Then set each entry to its actual kind. Existing code:

```kotlin
enum class BiTable(val fileName: String, val query: BiQueries.CsvQuery) {
    Absence("absence_DELTA", BiQueries.getAbsencesDelta),
    ...
```

New:

```kotlin
enum class BiTable(
    val fileName: String,
    val query: CsvQuery,
    val kind: QueryKind = QueryKind.Reference,
) {
    Absence("absence_DELTA", BiQueries.getAbsencesDelta, QueryKind.Temporal),
    Application("application", BiQueries.getApplications, QueryKind.Reference),
    ApplicationForm("application_form", BiQueries.getApplicationForms, QueryKind.Reference),
    AssistanceAction("assistance_action", BiQueries.getAssistanceActions, QueryKind.RangeBearing),
    AssistanceActionOption("assistance_action_option", BiQueries.getAssistanceActionOptions, QueryKind.Reference),
    AssistanceActionOptionRef("assistance_action_option_ref", BiQueries.getAssistanceActionOptionRefs, QueryKind.Reference),
    AssistanceFactor("assistance_factor", BiQueries.getAssistanceFactors, QueryKind.RangeBearing),
    AssistanceNeedVoucherCoefficient("assistance_need_voucher_coefficient", BiQueries.getAssistanceNeedVoucherCoefficients, QueryKind.RangeBearing),
    AttendanceReservation("attendance_reservation", BiQueries.getAttendanceReservations, QueryKind.Temporal),
    BackupCare("backup_care", BiQueries.getBackupCares, QueryKind.RangeBearing),
    CareArea("care_area", BiQueries.getAreas, QueryKind.Reference),
    Child("child", BiQueries.getChildren, QueryKind.Reference),
    ChildAttendance("child_attendance_DELTA", BiQueries.getChildAttendanceDelta, QueryKind.Temporal),
    Daycare("daycare", BiQueries.getDaycares, QueryKind.Reference),
    DaycareAssistance("daycare_assistance", BiQueries.getDaycareAssistances, QueryKind.RangeBearing),
    DaycareCaretaker("daycare_caretaker", BiQueries.getDaycareCaretakers, QueryKind.RangeBearing),
    DaycareGroup("daycare_group", BiQueries.getDaycareGroups, QueryKind.Reference),
    DaycareGroupPlacement("daycare_group_placement", BiQueries.getDaycareGroupPlacements, QueryKind.RangeBearing),
    Decision("decision", BiQueries.getDecisions, QueryKind.Reference),
    Employee("employee", BiQueries.getEmployees, QueryKind.Reference),
    EvakaUser("evaka_user", BiQueries.getEvakaUsers, QueryKind.Reference),
    FeeAlteration("fee_alteration", BiQueries.getFeeAlterations, QueryKind.RangeBearing),
    FeeDecision("fee_decision", BiQueries.getFeeDecisions, QueryKind.RangeBearing),
    FeeDecisionChild("fee_decision_child", BiQueries.getFeeDecisionChildren, QueryKind.Reference),
    FeeThresholds("fee_thresholds", BiQueries.getFeeThresholds, QueryKind.RangeBearing),
    FridgeChild("fridge_child", BiQueries.getFridgeChildren, QueryKind.RangeBearing),
    FridgePartner("fridge_partner", BiQueries.getFridgePartners, QueryKind.Reference),
    Guardian("guardian", BiQueries.getGuardians, QueryKind.Reference),
    GuardianBlocklist("guardian_blocklist", BiQueries.getGuardianBlockLists, QueryKind.Reference),
    HolidayPeriod("holiday_period", BiQueries.getHolidayPeriods, QueryKind.RangeBearing),
    HolidayPeriodQuestionnaireAnswer("holiday_period_questionnaire_answer", BiQueries.getHolidayQuestionnaireAnswers, QueryKind.Reference),
    Income("income", BiQueries.getIncomes, QueryKind.RangeBearing),
    OtherAssistanceMeasure("other_assistance_measure", BiQueries.getOtherAssistanceMeasures, QueryKind.RangeBearing),
    Person("person", BiQueries.getPersons, QueryKind.Reference),
    Placement("placement", BiQueries.getPlacements, QueryKind.RangeBearing),
    PreschoolAssistance("preschool_assistance", BiQueries.getPreschoolAssistances, QueryKind.RangeBearing),
    ServiceNeed("service_need", BiQueries.getServiceNeeds, QueryKind.RangeBearing),
    ServiceNeedOption("service_need_option", BiQueries.getServiceNeedOptions, QueryKind.Reference),
    ServiceNeedOptionVoucherValue("service_need_option_voucher_value", BiQueries.getServiceNeedOptionVoucherValues, QueryKind.RangeBearing),
    StaffAttendance("staff_attendance", BiQueries.getStaffAttendance, QueryKind.Temporal),
    StaffAttendanceExternal("staff_attendance_external", BiQueries.getStaffAttendanceExternals, QueryKind.Temporal),
    StaffAttendancePlan("staff_attendance_plan", BiQueries.getStaffAttendancePlans, QueryKind.Temporal),
    StaffOccupancyCoefficient("staff_occupancy_coefficient", BiQueries.getStaffOccupancyCoefficients, QueryKind.Reference),
    VoucherValueDecision("voucher_value_decision", BiQueries.getVoucherValueDecisions, QueryKind.RangeBearing),
}
```

Note on kind assignment — **read each query's actual SQL before deciding a kind**. If a query lacks a date column or date range, it's `Reference`. If it has a single date (`date`, `occurred_at`, `departed`), it's `Temporal`. If it has `daterange`/`start_date`+`end_date`, it's `RangeBearing`. The list above is a first pass — verify in Task 6 Step 3.

**Step 3: Verify kind assignments.** Open `BiQueries.kt` and cross-check each query's SQL against the kind in the `BiTable` entry. Fix any mismatches.

**Step 4: Compile and test:**

```bash
./gradlew compileKotlin test --tests "evaka.core.bi.*" --tests "*Tampere*"
```

Expected: green. No behavior change — `kind` is just metadata.

---

## Task 7: Parameterize queries with a `since` window and apply it in BiExportJob

**Context:** Add a `since: LocalDate?` parameter to `CsvQuery` invocation. When `since == null` (Tampere), queries run unfiltered. When `since` is set (Oulu), Temporal and RangeBearing queries filter by it; Reference queries ignore it.

**Files:**
- Modify: `service/src/main/kotlin/evaka/core/bi/BiQueries.kt`
- Modify: `service/src/main/kotlin/evaka/core/bi/BiExportJob.kt`
- Test (create): `service/src/test/kotlin/evaka/core/bi/BiExportJobTest.kt`

**Step 1: Write the failing test.**

```kotlin
// SPDX-FileCopyrightText: 2026 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class BiExportJobTest {
    @Test
    fun `windowMonths = 0 yields null since`() {
        val config = BiExportConfig(includePII = true, windowMonths = 0)
        val clock = MockEvakaClock(2026, 4, 17, 1, 0)
        assertNull(resolveSince(clock, config))
    }

    @Test
    fun `windowMonths = 3 yields today minus 3 months`() {
        val config = BiExportConfig(includePII = false, windowMonths = 3)
        val clock = MockEvakaClock(2026, 4, 17, 1, 0)
        assertEquals(LocalDate.of(2026, 1, 17), resolveSince(clock, config))
    }

    @Test
    fun `windowMonths = 24 yields today minus 24 months`() {
        val config = BiExportConfig(includePII = false, windowMonths = 24)
        val clock = MockEvakaClock(2026, 4, 17, 1, 0)
        assertEquals(LocalDate.of(2024, 4, 17), resolveSince(clock, config))
    }
}
```

**Step 2: Run test to verify it fails:**

```bash
./gradlew test --tests "evaka.core.bi.BiExportJobTest"
```

Expected: FAIL with "unresolved reference: resolveSince".

**Step 3: Implement `resolveSince` in `BiExportJob.kt` as a top-level function (or companion):**

```kotlin
internal fun resolveSince(clock: EvakaClock, config: BiExportConfig): LocalDate? =
    if (config.windowMonths > 0)
        clock.now().toLocalDate().minusMonths(config.windowMonths.toLong())
    else null
```

**Step 4: Extend the `CsvQuery` interface to accept `since`:**

```kotlin
interface CsvQuery {
    val modelClass: KClass<*>
    operator fun <R> invoke(tx: Database.Read, since: LocalDate?, use: (Sequence<String>) -> R): R
}
```

Update `StreamingCsvQuery`:

```kotlin
class StreamingCsvQuery<T : Any>(
    override val modelClass: KClass<T>,
    private val sqlBuilder: (LocalDate?) -> QuerySql,
    private val config: BiExportConfig,
) : CsvQuery {
    override fun <R> invoke(tx: Database.Read, since: LocalDate?, use: (Sequence<String>) -> R): R =
        tx.createQuery(sqlBuilder(since))
            .mapTo(modelClass.java)
            .useSequence { rows ->
                val records = toCsvRecords(::convertToCsv, modelClass, rows, config)
                use(records)
            }
}
```

The exact integration depends on current internals — read `BiQueries.kt` lines 449–500 first to see `StreamingCsvQuery`. Adapt accordingly.

**Step 5: Modify `csvQuery<T>` DSL** so the SQL can optionally use `since`:

```kotlin
private inline fun <reified T : Any> csvQuery(
    noinline builder: QuerySql.Builder.(since: LocalDate?) -> Unit,
): CsvQuery = StreamingCsvQuery(
    modelClass = T::class,
    sqlBuilder = { since -> QuerySql { builder(since) } },
)
```

Existing query definitions (Reference) ignore the parameter:

```kotlin
val getAreas = csvQuery<BiArea> { _ ->
    sql("SELECT ... FROM care_area")
}
```

**Step 6: Update `BiExportJob` to pass `since`:**

```kotlin
class BiExportJob(private val client: BiExportClient, private val config: BiExportConfig) {
    fun sendBiTable(db: Database.Connection, clock: EvakaClock, tableName: String, query: CsvQuery) {
        val since = resolveSince(clock, config)
        db.read { tx ->
            tx.setStatementTimeout(Duration.ofMinutes(10))
            query(tx, since) { records ->
                val stream = EspooBiJob.CsvInputStream(CSV_CHARSET, records)
                client.sendBiCsvFile(tableName, clock, stream)
            }
        }
    }
}
```

**Step 7: Update all existing queries in `BiQueries.kt` to take (and ignore) the `since` parameter.** Because most are `Reference`, the modification is a mechanical `csvQuery<X> { _ -> ...` change.

**Step 8: Compile and run tests:**

```bash
./gradlew test --tests "evaka.core.bi.*" --tests "*Tampere*"
```

Expected: green. Temporary shim: if Tampere still constructs `BiExportJob(client)` without config at this point, add a temporary default: `class BiExportJob(private val client: BiExportClient, private val config: BiExportConfig = BiExportConfig(includePII = true))`. Remove the default in Task 10.

---

## Task 8: Apply window filters to Temporal and RangeBearing queries

**Context:** For every `Temporal` / `RangeBearing` query (per Task 6 classification), edit the SQL so that when `since != null`, the WHERE clause filters appropriately.

**Files:**
- Modify: `service/src/main/kotlin/evaka/core/bi/BiQueries.kt`

**Step 1: For each Temporal query**, add a `since` filter on the row's date column. Example for `getAbsencesDelta`:

```kotlin
val getAbsencesDelta = csvQuery<BiAbsence> { since ->
    sql("""
        SELECT ... FROM absence
        WHERE ${if (since != null) "date >= <since>" else "true"}
    """)
        .apply { if (since != null) bind("since", since) }
}
```

Use the `QuerySql` DSL's actual binding syntax (look up an example in evaka core, e.g., `evaka.core.shared.db.QuerySql`).

**Step 2: For each RangeBearing query**, use overlap semantics:

```kotlin
val getPlacements = csvQuery<BiPlacement> { since ->
    sql("""
        SELECT ... FROM placement
        WHERE ${if (since != null) "daterange(start_date, end_date, '[]') && daterange(<since>, null, '[]')" else "true"}
    """)
        .apply { if (since != null) bind("since", since) }
}
```

**Step 3: Add integration tests for window semantics.** Create `service/src/integrationTest/kotlin/evaka/core/bi/BiQueriesWindowIntegrationTest.kt`. Minimum coverage — three representative cases:

- One Temporal query: assert a row outside `since` is excluded; a row on/after `since` is included.
- One RangeBearing query: assert overlap semantics — a range ending before `since` is excluded, a range spanning `since` is included, an open-ended range is included if start is in the past.
- One Reference query: assert that `since = arbitrary-date` still returns all rows (no filter applied).

Use the existing PureJdbiTest pattern. Look at other integration tests under `service/src/integrationTest/` for the fixture setup.

**Step 4: Run tests:**

```bash
./gradlew integrationTest --tests "evaka.core.bi.BiQueriesWindowIntegrationTest"
```

Expected: green.

---

## Task 9: Tag PII and legacy fields in BiModels

**Context:** Apply `@Pii` to identifier and sensitive-text fields; `@LegacyColumn` to always-null fields (those selected as `NULL AS ...` in `BiQueries`).

**Files:**
- Modify: `service/src/main/kotlin/evaka/core/bi/BiModels.kt`

**Step 1: Read `BiModels.kt` fully** to see every data class.

**Step 2: Read `BiQueries.kt`** to identify every `NULL AS <column>` — these are the `@LegacyColumn` candidates. Grep: `grep -n "NULL AS" service/src/main/kotlin/evaka/core/bi/BiQueries.kt`.

**Step 3: Apply annotations:**

- `BiPerson`: `@Pii` on `firstName`, `lastName`, `ssn` (`socialSecurityNumber`), `email`, `phone`, `backupPhone`, `invoicingStreetAddress`, `invoicingPostalCode`, `invoicingPostOffice`. NOT on `streetAddress`, `postalCode`, `postOffice` (home address stays).
- `BiPerson.enabledEmailTypes`: `@LegacyColumn` (NULL in query).
- `BiEmployee`: `@Pii` on `firstName`, `lastName`, `email`.
- `BiGuardian`: whatever identifier fields exist (same as `BiPerson`).
- `BiApplicationForm`: `@Pii` on `document` (JSON blob with nested PII).
- `BiDecision`, `BiFeeDecision`, `BiVoucherValueDecision`: `@Pii` on free-text rationale/justification fields if present.
- `BiAssistanceAction.otherAction`: `@Pii` (free-text).
- `BiAssistanceNeedDecision` (if exists): free-text reasoning → `@Pii`.

**Step 4: Run the audit regression test** (`BiCsvUtilsAuditTest`):

```bash
./gradlew test --tests "evaka.core.bi.BiCsvUtilsAuditTest"
```

Expected: green.

**Step 5: Run full BI tests and Tampere tests:**

```bash
./gradlew test --tests "evaka.core.bi.*" --tests "*Tampere*"
./gradlew integrationTest --tests "*Bi*" --tests "*Tampere*"
```

Expected: green. Tampere's output is unchanged because its config has `includePII=true`.

---

## Task 10: Wire Tampere to the new BiExportConfig — behavior preserved

**Context:** Tampere must behave exactly as before: full snapshots, all columns, legacy columns included.

**Files:**
- Modify: `service/src/main/kotlin/evaka/instance/tampere/TampereProperties.kt`
- Modify: Spring bean wiring for Tampere's `BiExportJob` (search for where `BiExportJob(...)` is instantiated with `@Bean`).

**Step 1: Find the Tampere bean that creates `BiExportJob`.** Grep:

```bash
grep -rn "BiExportJob(" service/src/main/kotlin/evaka/instance/tampere/
```

**Step 2: Instantiate with an explicit config:**

```kotlin
@Bean
fun biExportJob(client: BiExportClient): BiExportJob =
    BiExportJob(client, BiExportConfig(includePII = true, includeLegacyColumns = true, windowMonths = 0))
```

**Step 3: Remove the temporary default value** added in Task 7 (`BiExportJob(client, config = BiExportConfig(includePII = true))`) so all call sites must pass config explicitly.

**Step 4: Compile and test:**

```bash
./gradlew test --tests "*Tampere*"
./gradlew integrationTest --tests "*Tampere*"
```

Expected: green.

---

## Task 11: Add staff_attendance_realtime to the shared catalog

**Context:** Tampere currently exports `staff_attendance` (old), not `staff_attendance_realtime`. Oulu needs the realtime table. Per design, adding to the shared catalog adds for Tampere too — Tampere's product owner can opt out via `excludedTables` if they don't want it.

**Files:**
- Modify: `service/src/main/kotlin/evaka/core/bi/BiModels.kt`
- Modify: `service/src/main/kotlin/evaka/core/bi/BiQueries.kt`
- Modify: `service/src/main/kotlin/evaka/core/bi/BiTable.kt`

**Step 1: Add data class `BiStaffAttendanceRealtime` to `BiModels.kt`.** Check the `staff_attendance_realtime` table schema via `service/src/main/resources/db/migration/V487__staff_attendance_realtime_modified.sql` and subsequent migrations. Columns roughly: `id UUID`, `employee_id UUID`, `group_id UUID?`, `arrived TIMESTAMPTZ`, `departed TIMESTAMPTZ?`, `type VARCHAR`, `occupancy_coefficient NUMERIC`, `departed_automatically BOOLEAN`, `created_at`, `modified_at`.

```kotlin
data class BiStaffAttendanceRealtime(
    val id: UUID,
    val employeeId: UUID,
    val groupId: UUID?,
    val arrived: String,   // ISO timestamp, match existing ::text pattern in BiQueries
    val departed: String?,
    val type: StaffAttendanceType,
    val occupancyCoefficient: BigDecimal,
    val departedAutomatically: Boolean,
    val created: String,
    val updated: String,
)
```

Import `evaka.core.attendance.StaffAttendanceType` (check current location — `grep -rn "enum class StaffAttendanceType"`).

**Step 2: Add the query to `BiQueries.kt`:**

```kotlin
val getStaffAttendanceRealtime = csvQuery<BiStaffAttendanceRealtime> { since ->
    sql("""
        SELECT id, employee_id, group_id, arrived::text, departed::text, type,
               occupancy_coefficient, departed_automatically,
               created_at::text AS created, modified_at::text AS updated
        FROM staff_attendance_realtime
        WHERE ${if (since != null) "arrived::date >= <since>" else "true"}
    """)
        .apply { if (since != null) bind("since", since) }
}
```

Treat it as `Temporal` (filter by `arrived::date`).

**Step 3: Add enum entry to `BiTable.kt`:**

```kotlin
StaffAttendanceRealtime("staff_attendance_realtime", BiQueries.getStaffAttendanceRealtime, QueryKind.Temporal),
```

**Step 4: Integration test.** Add a test case to `BiQueriesWindowIntegrationTest` seeding a realtime row inside and outside the window, assert filtering correctness.

**Step 5: Run tests:**

```bash
./gradlew test --tests "evaka.core.bi.*"
./gradlew integrationTest --tests "evaka.core.bi.*"
```

Expected: green.

---

## Task 12: Add Oulu BI properties to OuluEnv

**Context:** Introduce `evakaoulu.bi.*` configuration (window, SFTP creds, remote path). Reuses `evaka.core.SftpEnv`.

**Files:**
- Modify: `service/src/main/kotlin/evaka/instance/oulu/OuluProperties.kt`
- Modify: `service/src/main/resources/application-oulu_evaka.yaml` (local defaults only — secrets stay in deployment config)

**Step 1: Read `OuluEnv` / `OuluProperties.kt`** to see the current structure.

**Step 2: Add a `BiExportProperties` class and field.**

```kotlin
data class OuluBiProperties(
    val windowMonths: Int = 3,
    val excludedTables: Set<String> = emptySet(),  // BiTable enum names
    val sftp: SftpEnv,
    val remotePath: String,
)
```

Wire it into the top-level Oulu props (env var `evakaoulu.bi.window_months` etc.).

**Step 3: Add YAML defaults (local profile).** For `application-oulu_evaka.yaml`:

```yaml
evakaoulu:
  bi:
    window_months: 3
    remote_path: /upload
    sftp:
      host: localhost
      port: 2222
      username: evaka
      private_key: |-
        -----BEGIN OPENSSH PRIVATE KEY-----
        ...
      host_keys:
        - "AAAAB3NzaC1yc2EA..."
```

Structure only; real keys injected via deployment.

**Step 4: Compile:**

```bash
./gradlew compileKotlin
```

Expected: green.

---

## Task 13: Create OuluBiSftpExportClient

**Context:** Implement the `BiExportClient` interface for Oulu — zip the CSV, push via SFTP using the shared `SftpClient`.

**Files:**
- Create: `service/src/main/kotlin/evaka/instance/oulu/bi/OuluBiSftpExportClient.kt`
- Test (create): `service/src/integrationTest/kotlin/evaka/instance/oulu/bi/OuluBiSftpExportClientTest.kt`

**Step 1: Read `FileBiExportS3Client.kt` (Tampere)** — model the zip-and-upload pattern after it.

**Step 2: Write the client:**

```kotlin
// SPDX-FileCopyrightText: 2026 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.bi

import evaka.core.bi.BiExportClient
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.sftp.SftpClient
import evaka.instance.espoo.bi.EspooBiJob
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val logger = KotlinLogging.logger {}

class OuluBiSftpExportClient(
    private val sftpClient: SftpClient,
    private val remotePath: String,
) : BiExportClient {
    override fun sendBiCsvFile(
        tableName: String,
        clock: EvakaClock,
        stream: EspooBiJob.CsvInputStream,
    ): Pair<String, String> {
        val date = clock.now().toLocalDate()
        val entryName = "$tableName.csv"
        val fileName = "${tableName}_${date.format(DateTimeFormatter.ISO_DATE)}.zip"

        logger.info { "Sending BI content for '$tableName' to Oulu SFTP" }

        val buffer = ByteArrayOutputStream()
        ZipOutputStream(BufferedOutputStream(buffer)).use { zip ->
            zip.putNextEntry(ZipEntry(entryName))
            stream.transferTo(zip)
            zip.closeEntry()
        }

        sftpClient.put(ByteArrayInputStream(buffer.toByteArray()), "$remotePath/$fileName")

        logger.info { "BI file '$fileName' successfully sent to Oulu SFTP" }
        return remotePath to fileName
    }
}
```

Note: in-memory buffering. For 50k-row tables at ~500 bytes/row, that's ~25 MB — acceptable. If profiling reveals a problem, switch to a temp-file-based approach mirroring `FileBiExportS3Client`.

**Step 3: Integration test using an SFTP test server.** Look at `service/src/integrationTest/kotlin/evaka/core/shared/sftp/SftpClientTest.kt` for the pattern — it uses `EVAKA_SFTP_PORT=2222`. Follow that pattern. Minimum coverage:

- Happy path: upload, verify file exists on the test server.
- Host-key mismatch: verify the client refuses to connect (should propagate a JSch exception).

**Step 4: Run the test:**

```bash
./gradlew integrationTest --tests "OuluBiSftpExportClientTest"
```

Expected: green. If Apache MINA SSHD isn't in test deps, the existing `SftpClientTest` will show how the project runs SFTP tests — mirror that exactly. If it requires an external SFTP server, document the requirement at the top of the test file.

---

## Task 14: Add OuluAsyncJob.SendBiTable

**Context:** Oulu's async job pool currently only handles DW queries. Add a new `SendBiTable` case for shared `BiTable` entries.

**Files:**
- Modify: `service/src/main/kotlin/evaka/instance/oulu/OuluAsyncJob.kt`

**Step 1: Extend `OuluAsyncJob`:**

```kotlin
sealed interface OuluAsyncJob : AsyncJobPayload {
    data class SendDWQuery(val query: DwQuery) : OuluAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendBiTable(val table: BiTable) : OuluAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    companion object {
        val pool =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(OuluAsyncJob::class, "oulu"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(SendDWQuery::class, SendBiTable::class),
            )
    }
}
```

Import `evaka.core.bi.BiTable`.

**Step 2: Update `OuluAsyncJobRegistration`:**

```kotlin
class OuluAsyncJobRegistration(
    runner: AsyncJobRunner<OuluAsyncJob>,
    dwExportJob: DwExportJob,
    biExportJob: BiExportJob,
) {
    init {
        runner.registerHandler(dwExportJob::sendDwQuery)
        runner.registerHandler { db, clock, msg: OuluAsyncJob.SendBiTable ->
            biExportJob.sendBiTable(db, clock, msg.table.fileName, msg.table.query)
        }
    }
}
```

**Step 3: Compile:**

```bash
./gradlew compileKotlin
```

Expected: green (BiExportJob bean for Oulu is wired in Task 16).

---

## Task 15: Add OuluScheduledJob.PlanBiExportJobs

**Files:**
- Modify: `service/src/main/kotlin/evaka/instance/oulu/OuluScheduledJobs.kt`

**Step 1: Add the scheduled job case and planning function** (mirroring Tampere's `PlanBiExportJobs`):

```kotlin
enum class OuluScheduledJob(...) {
    PlanDwExportJobs(...),
    PlanBiExportJobs(
        { jobs, db, clock -> jobs.planBiJobs(db, clock) },
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(2, 0))),
    ),
}
```

```kotlin
fun planBiJobs(db: Database.Connection, clock: EvakaClock) {
    val excluded = biConfig.excludedTables
    val tables = BiTable.entries.filterNot { it in excluded }
    logger.info { "Planning Oulu BI jobs for ${tables.size} tables" }
    db.transaction { tx ->
        tx.removeUnclaimedJobs(setOf(AsyncJobType(OuluAsyncJob.SendBiTable::class)))
        asyncJobRunner.plan(
            tx,
            tables.asSequence().map(OuluAsyncJob::SendBiTable),
            runAt = clock.now(),
            retryCount = 1,
        )
    }
}
```

Inject `biConfig: BiExportConfig` via the `OuluScheduledJobs` constructor.

**Step 2: Compile:**

```bash
./gradlew compileKotlin
```

Expected: green.

---

## Task 16: Wire Oulu BI beans in OuluConfig

**Context:** Create the beans so Spring can assemble the Oulu BI pipeline.

**Files:**
- Modify: `service/src/main/kotlin/evaka/instance/oulu/OuluConfig.kt`

**Step 1: Read `OuluConfig.kt`** (focus on how `DwExportJob`, `FileDwExportClient`, and SFTP-related beans are currently wired).

**Step 2: Add:**

```kotlin
@Bean
fun ouluBiConfig(env: OuluEnv): BiExportConfig =
    BiExportConfig(
        includePII = false,
        includeLegacyColumns = false,
        windowMonths = env.bi.windowMonths,
        excludedTables = env.bi.excludedTables.map { BiTable.valueOf(it) }.toSet(),
    )

@Bean
fun ouluBiSftpClient(env: OuluEnv): SftpClient =
    SftpClient(env.bi.sftp)

@Bean
fun ouluBiExportClient(env: OuluEnv, sftpClient: SftpClient): BiExportClient =
    OuluBiSftpExportClient(sftpClient, env.bi.remotePath)

@Bean
fun ouluBiExportJob(client: BiExportClient, config: BiExportConfig): BiExportJob =
    BiExportJob(client, config)
```

Confirm there's no bean collision with Tampere (there shouldn't be — Tampere beans are profile-scoped to `tampere_evaka`, Oulu to `oulu_evaka`).

**Step 3: Update the `OuluScheduledJobs` bean wiring** to inject `BiExportConfig`.

**Step 4: Compile and run Oulu tests:**

```bash
./gradlew compileKotlin
./gradlew test --tests "*Oulu*"
```

Expected: green.

---

## Task 17: End-to-end smoke test for Oulu BI pipeline

**Context:** Verify the full pipeline — config → `BiExportJob` → `OuluBiSftpExportClient` → test SFTP server — produces a zipped CSV with no PII columns.

**Files:**
- Test (create): `service/src/integrationTest/kotlin/evaka/instance/oulu/bi/OuluBiExportJobIntegrationTest.kt`

**Step 1: Write a test** that:

1. Sets up an SFTP test server (reuse the pattern from `OuluBiSftpExportClientTest`).
2. Seeds rows into `person`, `placement`, `staff_attendance_realtime`.
3. Runs `BiExportJob.sendBiTable(...)` for `BiTable.Person`, `BiTable.Placement`, `BiTable.StaffAttendanceRealtime` with Oulu config (`includePII=false, windowMonths=3`).
4. Downloads the resulting zips, unpacks the CSVs.
5. Asserts the Person CSV header has no `first_name`, `last_name`, `ssn`, `email`, etc.
6. Asserts the Placement CSV contains only rows that overlap the 3-month window.
7. Asserts the StaffAttendanceRealtime CSV contains only rows within the window.

**Step 2: Run the test:**

```bash
./gradlew integrationTest --tests "evaka.instance.oulu.bi.OuluBiExportJobIntegrationTest"
```

Expected: green.

---

## Task 18: Final verification

**Step 1: Full build:**

```bash
./gradlew clean check
```

Expected: green.

**Step 2: Grep audit for leftover references:**

```bash
grep -rn "FabricQuery\|FabricHistoryQuery\|evaka\.instance\.tampere\.bi\.BiTable\|evaka\.instance\.tampere\.bi\.BiCsvUtils" service/src/
```

Expected: only expected results (`FabricQuery` — none; Tampere-package imports — only `FileBiExportS3Client.kt`, `TampereBiExportJob.kt`, and `TampereAsyncJob.kt`).

**Step 3: Verify the design contract.** Cross-check against the design doc (`docs/plans/2026-04-17-oulu-fabric-bi-integration-design.md`):

- [ ] `BiExportConfig` has the four fields with the right types.
- [ ] `@Pii` and `@LegacyColumn` annotations exist and are honored.
- [ ] Tampere behavior is unchanged (same output files, same column list).
- [ ] Oulu config is `includePII=false, includeLegacyColumns=false, windowMonths=3`.
- [ ] `staff_attendance_realtime` is in the shared catalog.
- [ ] Old `DwQuery` pathway still exists and compiles (meeting step 7 is deferred).
- [ ] `FabricQuery`/`FabricHistoryQuery` pathway is deleted (meeting step 1 is done).

**Step 4: Do not merge yet.** This PR needs:
- Reaktor↔Oulu SFTP key exchange (meeting steps 3–4) — operational, outside this plan.
- Staging smoke test (meeting step 5) — post-merge in staging.
- Production cutover (meeting step 6) — post-staging validation.

Open a PR with title `Oulu Fabric BI integration`. Link the design doc in the PR body.

---

## Out-of-scope (deferred)

- Meeting step 7 — deleting the old `DwQuery` pathway — is a separate PR that runs only after Oulu's old DW consumer is decommissioned on their end.
- Adding additional Oulu-requested tables ("mm. staff realtime attendance") beyond `staff_attendance_realtime` — identified during implementation; not pre-listed.
- Backfill execution — operational (set `OULU_BI_WINDOW_MONTHS=24`, redeploy, run, restore).
- Infrastructure changes (SFTP server DNS, key secrets, env var plumbing into AWS Secrets Manager / SSM) — outside this repo.
