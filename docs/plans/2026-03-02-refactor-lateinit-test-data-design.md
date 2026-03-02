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
| 1 | todo | `aromi/AromiControllerTest.kt` | 12 | Dev* objects and IDs |
| 2 | todo | `reports/ReportSmokeTests.kt` | 4 | Uses @BeforeAll |
| 3 | todo | `absence/AbsenceControllerIntegrationTest.kt` | 4 | Dev* objects |
| 4 | todo | `reservations/MissingHolidayReservationsRemindersTest.kt` | 4 | ID vars |
| 5 | todo | `attachments/AttachmentQueriesTest.kt` | 4 | Auth/parent vars |
| 6 | todo | `shared/security/UnitAccessControlTest.kt` | 3 | ID vars |
| 7 | todo | `reservations/MissingReservationsRemindersTest.kt` | 3 | ID vars |
| 8 | todo | `invoicing/service/NewCustomerIncomeNotificationIntegrationTest.kt` | 2 | Clock and date |
| 9 | todo | `daycare/dao/PlacementQueriesIntegrationTest.kt` | 3 | ID vars |
| 10 | todo | `application/ApplicationQueriesSmokeTest.kt` | 3 | Uses @BeforeAll |
| 11 | todo | `absence/AbsencePushNotificationsTest.kt` | 3 | Clock and IDs |
| 12 | todo | `reports/PreschoolApplicationReportTest.kt` | 2 | DaycareId vars |
| 13 | todo | `pis/controller/FamilyControllerTest.kt` | 2 | User and childId |
| 14 | todo | `pis/SystemControllerTest.kt` | 2 | AreaId and DaycareId |
| 15 | todo | `pairing/PairingIntegrationTest.kt` | 2 | User and unitId |
| 16 | todo | `invoicing/controller/FinanceDecisionCitizenIntegrationTest.kt` | 2 | List vars |
| 17 | todo | `assistanceaction/AssistanceActionIntegrationTest.kt` | 2 | DaycareId and ChildId |
| 18 | todo | `application/UnitTransferApplicationsIntegrationTest.kt` | 1 | Admin user |
| 19 | todo | `absence/AbsenceApplicationControllersTest.kt` | 1-2 | Nested class vars |
| 20 | todo | `webpush/WebPushTest.kt` | 1 | MockEvakaClock |
| 21 | todo | `shared/security/ChildAccessControlTest.kt` | 1 | ChildId |
| 22 | todo | `reports/AssistanceNeedsAndActionsReportControllerTest.kt` | 1 | Admin user |
| 23 | todo | `messaging/MessageNotificationEmailServiceIntegrationTest.kt` | 1 | MockEvakaClock |

### Complex conversions (need case-by-case handling)

| # | Status | File | Lateinit count | Notes |
|---|--------|------|----------------|-------|
| 24 | todo | `messaging/MessageIntegrationTest.kt` | 14 | MessageAccountIds from DB queries |
| 25 | todo | `messaging/MessagePushNotificationsTest.kt` | 6 | Accounts from DB |
| 26 | todo | `invoicing/controller/IncomeControllerCitizenIntegrationTest.kt` | 5 | Dependent ID chain |
| 27 | todo | `shared/security/ApplicationAccessControlTest.kt` | 4 | Custom insertions |
| 28 | todo | `invoicing/service/OutdatedIncomeNotificationsIntegrationTest.kt` | 3 | Fridge parent setup |
| 29 | todo | `messaging/MessageQueriesTest.kt` | 2 | TestAccounts from DB |
| 30 | todo | `messaging/MessageAccountQueriesTest.kt` | 2 | DB-derived account ID |
| 31 | todo | `koski/KoskiIntegrationTest.kt` | 2 | Spy/mock variant data |
| 32 | todo | `decision/DecisionCreationIntegrationTest.kt` | 2 | Daycare from DB query |
| 33 | todo | `placement/PlacementControllerIntegrationTest.kt` | 1 | DaycarePlacementDetails from DB |
| 34 | todo | `calendarevent/CalendarEventServiceIntegrationTest.kt` | TBD | Verify actual lateinit usage |

### Documentation

| # | Status | File | Notes |
|---|--------|------|-------|
| 35 | todo | `docs/developer-guide/service/testing.md` | Add anti-pattern section and variant data guidance |
