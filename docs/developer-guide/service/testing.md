<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Testing

Quick reference for writing backend tests in eVaka. See [Design Philosophy](../design-philosophy.md) for testing principles.

## Unit Tests

**Location:** `service/src/test/kotlin/`

**Running:**
```bash
cd service
./gradlew test --tests "fi.espoo.evaka.shared.domain.TimeRangeTest"
```

**Example:**
```kotlin
package fi.espoo.evaka.shared.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class TimeRangeTest {
    @Test
    fun `start is inclusive and end is exclusive`() {
        val range = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))

        assertTrue(range.includes(LocalTime.of(8, 0)))
        assertFalse(range.includes(LocalTime.of(16, 0)))
    }
}
```

Use unit tests for pure functions with complex logic. Use `kotlin.test` package for assertions.

## Integration Tests

**Location:** `service/src/integrationTest/kotlin/`

**Running:**
```bash
cd service
./gradlew integrationTest --tests "fi.espoo.evaka.absence.AbsenceControllerIntegrationTest"
```

**Base class:** `FullApplicationTest(resetDbBeforeEach = true)` - runs full Spring application and resets database between tests.

For testing database queries without the Spring application (faster startup), use `PureJdbiTest(resetDbBeforeEach = true)` instead.

**Key Patterns:**

- **No universal fixtures** - Each test file creates its own test data
- **Dev API** - Use `tx.insert(DevFoo(...))` to create test data
- **Mock time** - Always use `MockEvakaClock`, never rely on actual execution time
- **Helper methods** - Wrap controller calls with default parameters for readability
- **Assertions** - Either call controller endpoints or query database directly
- **Async jobs** - Call `asyncJobRunner.runPendingJobsSync(clock)` to execute scheduled jobs

**Example:**
```kotlin
class AbsenceControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var absenceController: AbsenceController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val now = HelsinkiDateTime.of(LocalDate.of(2024, 1, 15), LocalTime.of(12, 0))
    private val today = now.toLocalDate()

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val employee = DevEmployee()
    private val child = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insertDaycareAclRow(daycare.id, employee.id, UserRole.UNIT_SUPERVISOR)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today.plusMonths(6)
                )
            )
        }
    }

    @Test
    fun `creating absence schedules notification email`() {
        // Create absence via controller
        createAbsence(
            childId = child.id,
            date = today,
            absenceType = AbsenceType.SICKLEAVE
        )

        // Execute async jobs
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        // Assert via mock email client
        assertEquals(1, MockEmailClient.emails.size)
        assertEquals("Absence notification", MockEmailClient.emails[0].content.subject)
    }

    // Helper method with defaults
    private fun createAbsence(
        childId: ChildId,
        date: LocalDate,
        absenceType: AbsenceType,
        time: HelsinkiDateTime = now
    ) {
        absenceController.upsertAbsences(
            dbInstance(),
            employee.user,
            MockEvakaClock(time),
            listOf(
                AbsenceUpsert(
                    childId = childId,
                    date = date,
                    category = AbsenceCategory.BILLABLE,
                    absenceType = absenceType
                )
            ),
            group.id
        )
    }
}
```

## Test Data Patterns

### Prefer `val` over `lateinit var` for test data

Dev\* objects generate their own IDs at construction time, so they don't need to be created inside `@BeforeEach`. Declare test data as `private val` at class level and only insert in `@BeforeEach`:

```kotlin
// GOOD: val at class level, insert in @BeforeEach
private val area = DevCareArea()
private val daycare = DevDaycare(areaId = area.id)

@BeforeEach
fun beforeEach() {
    db.transaction { tx ->
        tx.insert(area)
        tx.insert(daycare)
    }
}

// BAD: unnecessary lateinit var
private lateinit var daycareId: DaycareId

@BeforeEach
fun beforeEach() {
    db.transaction { tx ->
        val areaId = tx.insert(DevCareArea())
        daycareId = tx.insert(DevDaycare(areaId = areaId))
    }
}
```

`lateinit var` should only be used for:
- `@Autowired` Spring-injected beans (controllers, services)
- Values that genuinely require a DB query to obtain (e.g., `MessageAccountId` from `tx.getCitizenMessageAccount()`)
- DB-read domain objects (e.g., `Daycare` from `tx.getDaycare()`)
- Mutable clocks where `clock.tick()` is called and the class uses `@TestInstance(PER_CLASS)`

### Dev\* convenience methods

Use `.user` properties instead of manually constructing `AuthenticatedUser`:

```kotlin
// DevEmployee.user returns AuthenticatedUser.Employee(id, roles)
private val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
// Use: employee.user

// DevPerson.user(authLevel) returns AuthenticatedUser.Citizen(id, authLevel)
private val guardian = DevPerson()
// Use: guardian.user(CitizenAuthLevel.STRONG)

// DevMobileDevice.user returns AuthenticatedUser.MobileDevice(id)
private val device = DevMobileDevice(unitId = daycare.id)
// Use: device.user
```

Other useful properties: `DevEmployee.evakaUserId`, `DevPerson.evakaUserId()`.

### Tests needing variant data

When a specific test needs different data from the shared setup, create additional Dev\* objects inline in that test method:

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
