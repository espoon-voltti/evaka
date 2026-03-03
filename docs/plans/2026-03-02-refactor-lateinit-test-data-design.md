<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Refactor `lateinit` Test Data Out of Integration Tests

## Problem

Many integration tests declare test data as `private lateinit var`, construct them inside `@BeforeEach`, and store results back. The documented pattern (see `docs/developer-guide/service/testing.md`) defines test data as `private val` at class level and only inserts in `@BeforeEach`. The `lateinit` pattern is unnecessary since Dev* objects generate their own IDs at construction time.

## Refactoring Rules

### Rule 1: Convert `lateinit var` Dev* objects to `val`

```kotlin
// BEFORE
private lateinit var daycare: DevDaycare
@BeforeEach fun beforeEach() {
    daycare = DevDaycare(areaId = area.id)
    db.transaction { tx -> tx.insert(daycare) }
}

// AFTER
private val daycare = DevDaycare(areaId = area.id)
@BeforeEach fun beforeEach() {
    db.transaction { tx -> tx.insert(daycare) }
}
```

### Rule 2: Convert ID-only `lateinit` to Dev* objects, access `.id`

```kotlin
// BEFORE
private lateinit var daycareId: DaycareId
@BeforeEach fun beforeEach() {
    db.transaction { tx ->
        daycareId = tx.insert(DevDaycare(areaId = area.id))
    }
}
// Uses: daycareId

// AFTER
private val daycare = DevDaycare(areaId = area.id)
@BeforeEach fun beforeEach() {
    db.transaction { tx -> tx.insert(daycare) }
}
// Uses: daycare.id
```

### Rule 3: Use `.user` convenience methods

Dev* classes provide `.user` properties — use them instead of manually constructing `AuthenticatedUser`:

- `DevEmployee.user` → `AuthenticatedUser.Employee(id, roles)`
- `DevPerson.user(authLevel)` → `AuthenticatedUser.Citizen(id, authLevel)`
- `DevMobileDevice.user` → `AuthenticatedUser.MobileDevice(id)`

```kotlin
// BEFORE
private lateinit var serviceWorker: AuthenticatedUser.Employee
@BeforeEach {
    val employee = DevEmployee()
    tx.insert(employee)
    serviceWorker = AuthenticatedUser.Employee(employee.id, setOf(UserRole.SERVICE_WORKER))
}

// AFTER
private val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
@BeforeEach { tx.insert(employee) }
// Use: employee.user
```

### Rule 4: Clocks and simple values become `val`

```kotlin
private val clock = MockEvakaClock(2024, 1, 15, 12, 0)
private val today = clock.today()
```

### Rule 5: Tests needing variant data

Create additional Dev* objects inline in the specific test method. Do not reuse the globally defined ones if the test needs different data:

```kotlin
private val area = DevCareArea()
private val daycare = DevDaycare(areaId = area.id)

@Test
fun `test with a different daycare`() {
    val specialDaycare = DevDaycare(areaId = area.id, name = "Special")
    db.transaction { tx -> tx.insert(specialDaycare) }
    // test with specialDaycare
}
```

### Rule 6: Parameterized setup functions (complex cases)

When many tests in a class need different base data, replace `@BeforeEach` with a parameterized function:

```kotlin
// No @BeforeEach — each test calls initTestData with its needs
private fun initTestData(
    providerType: ProviderType = ProviderType.MUNICIPAL,
    childCount: Int = 1
): TestData {
    val area = DevCareArea()
    val daycare = DevDaycare(areaId = area.id, providerType = providerType)
    val children = (1..childCount).map { DevPerson() }
    db.transaction { tx ->
        tx.insert(area)
        tx.insert(daycare)
        children.forEach { tx.insert(it, DevPersonType.CHILD) }
    }
    return TestData(area = area, daycare = daycare, children = children)
}

private data class TestData(
    val area: DevCareArea,
    val daycare: DevDaycare,
    val children: List<DevPerson>
)
```

### Rule 7: DB-generated IDs (e.g., MessageAccountId)

Handle case-by-case. Options:
- Return from a setup helper function as part of a data class
- Use lazy property if the value can be computed once
- Accept `lateinit` as an exception only when no Dev* constructor exists for the type

## What stays `lateinit`

- `@Autowired` Spring-injected controllers, services, beans
- Constructed services/clients that aren't test data (e.g., `AsyncJobRunner`, `AccessControl`)

## Documentation update

Add a section to `docs/developer-guide/service/testing.md` explaining:
- Why `val` is preferred over `lateinit var` for test data
- How to handle tests needing variant data
- The `.user` convenience methods on Dev* classes

## Task List

Each item is a test file to refactor. Status: `todo` / `in progress` / `completed`.

### Simple conversions (mechanical `lateinit var` → `val`)

| # | Status | File | Lateinit count | Notes |
|---|--------|------|----------------|-------|
| 1 | completed | `aromi/AromiControllerTest.kt` | 12 | Dev* objects and IDs |
| 2 | completed | `reports/ReportSmokeTests.kt` | 0 | Already follows pattern |
| 3 | completed | `absence/AbsenceControllerIntegrationTest.kt` | 4 | Dev* objects |
| 4 | completed | `reservations/MissingHolidayReservationsRemindersTest.kt` | 4 | ID vars |
| 5 | completed | `attachments/AttachmentQueriesTest.kt` | 4 | Auth/parent vars |
| 6 | completed | `shared/security/UnitAccessControlTest.kt` | 3 | ID vars |
| 7 | completed | `reservations/MissingReservationsRemindersTest.kt` | 3 | ID vars |
| 8 | completed | `invoicing/service/NewCustomerIncomeNotificationIntegrationTest.kt` | 0 | Clock/dates kept as `lateinit` — `FullApplicationTest` uses `@TestInstance(PER_CLASS)` and `clock.tick()` mutates shared state |
| 9 | completed | `daycare/dao/PlacementQueriesIntegrationTest.kt` | 3 | ID vars |
| 10 | completed | `application/ApplicationQueriesSmokeTest.kt` | 3 | Promoted area/daycare/child/guardian to Dev* vals, dropped unused applicationId |
| 11 | completed | `absence/AbsencePushNotificationsTest.kt` | 3 | Clock to val, daycare/group/device to Dev* vals, removed @BeforeAll |
| 12 | completed | `reports/PreschoolApplicationReportTest.kt` | 2 | Promoted area/daycare1/daycare2 to Dev* vals |
| 13 | completed | `pis/controller/FamilyControllerTest.kt` | 2 | Employee with `.user`/`.evakaUserId`, child with `.id` |
| 14 | completed | `pis/SystemControllerTest.kt` | 2 | Area/daycare to Dev* vals |
| 15 | completed | `pairing/PairingIntegrationTest.kt` | 2 | Employee with `.user`, daycare with `.id` |
| 16 | completed | `invoicing/controller/FinanceDecisionCitizenIntegrationTest.kt` | 2 | Extracted computed lists and dependencies to class-level vals |
| 17 | completed | `assistanceaction/AssistanceActionIntegrationTest.kt` | 2 | Area/daycare/child to Dev* vals with `.id` |
| 18 | completed | `application/UnitTransferApplicationsIntegrationTest.kt` | 1 | Admin to DevEmployee val with `.user` |
| 19 | completed | `absence/AbsenceApplicationControllersTest.kt` | 0 | All `lateinit` vars are `@Autowired` Spring beans — nothing to convert |
| 20 | completed | `webpush/WebPushTest.kt` | 0 | `PureJdbiTest` uses `PER_CLASS` and `clock.tick()` mutates state — must stay `lateinit` |
| 21 | completed | `shared/security/ChildAccessControlTest.kt` | 1 | Promoted child to `DevPerson` val, use `.id` |
| 22 | completed | `reports/AssistanceNeedsAndActionsReportControllerTest.kt` | 1 | Admin to `DevEmployee` val with `.user` |
| 23 | completed | `messaging/MessageNotificationEmailServiceIntegrationTest.kt` | 1 | Clock to val (no `tick()` calls) |

### Complex conversions (need case-by-case handling)

| # | Status | File | Lateinit count | Notes |
|---|--------|------|----------------|-------|
| 24 | completed | `messaging/MessageIntegrationTest.kt` | 0 | All 14 `lateinit` are DB-generated `MessageAccountId` — no Dev* constructor exists, must stay `lateinit` |
| 25 | completed | `messaging/MessagePushNotificationsTest.kt` | 3 | Promoted daycare/group/device/citizen/child to Dev* vals; clock + 3 `MessageAccountId` stay `lateinit` |
| 26 | completed | `invoicing/controller/IncomeControllerCitizenIntegrationTest.kt` | 5 | Guardian/employee to Dev* vals with `.id`/`.evakaUserId`/`.user()`, testChild `.id` directly |
| 27 | completed | `shared/security/ApplicationAccessControlTest.kt` | 2 | Child/daycare to Dev* vals; `creatorCitizen`/`applicationId` stay `lateinit` (parent helper + DB-dependent) |
| 28 | completed | `invoicing/service/OutdatedIncomeNotificationsIntegrationTest.kt` | 3 | Guardian/daycare to Dev* vals, `testChild.id` directly |
| 29 | completed | `messaging/MessageQueriesTest.kt` | 1 | Clock to val; `accounts` stays `lateinit` (DB-generated `TestAccounts`) |
| 30 | completed | `messaging/MessageAccountQueriesTest.kt` | 1 | Clock to val; `supervisorAccountId` stays `lateinit` (DB-generated). **Note:** test has pre-existing failure (`daycare_group_acl.created` generated column) unrelated to this refactoring |
| 31 | completed | `koski/KoskiIntegrationTest.kt` | 0 | Both `lateinit` are constructed services/mocks — nothing to convert |
| 32 | completed | `decision/DecisionCreationIntegrationTest.kt` | 1 | `serviceWorker` to `DevEmployee` val with `.user`; `testDaycare` stays `lateinit` (DB-read `Daycare` object) |
| 33 | completed | `placement/PlacementControllerIntegrationTest.kt` | 0 | `testPlacement: DaycarePlacementDetails` is a DB-read domain object — must stay `lateinit` |
| 34 | completed | `calendarevent/CalendarEventServiceIntegrationTest.kt` | 2 | Promoted placement/groupPlacement to Dev* vals with `.id`. **Note:** 1 test has pre-existing `daycare_group_acl.created` failure |

### Documentation

| # | Status | File | Notes |
|---|--------|------|-------|
| 35 | completed | `docs/developer-guide/service/testing.md` | Added "Test Data Patterns" section: val vs lateinit, Dev* convenience methods, variant data |
