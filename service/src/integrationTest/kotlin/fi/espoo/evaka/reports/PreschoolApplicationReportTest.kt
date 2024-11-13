// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPreschoolTerm
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PreschoolApplicationReportTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var preschoolApplicationReport: PreschoolApplicationReport

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 10, 16), LocalTime.of(8, 0)))
    private lateinit var unitId1: DaycareId
    private lateinit var unitId2: DaycareId

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            val currentTermStart = LocalDate.of(2024, 8, 7)
            FiniteDateRange(start = currentTermStart, end = LocalDate.of(2025, 5, 30)).let {
                tx.insert(
                    DevPreschoolTerm(
                        finnishPreschool = it,
                        swedishPreschool = it,
                        extendedTerm = it,
                        applicationPeriod = it,
                        termBreaks = DateSet.empty(),
                    )
                )
            }
            val nextTermStart = LocalDate.of(2025, 8, 6)
            FiniteDateRange(start = nextTermStart, end = LocalDate.of(2026, 5, 29)).let {
                tx.insert(
                    DevPreschoolTerm(
                        finnishPreschool = it,
                        swedishPreschool = it,
                        extendedTerm = it,
                        applicationPeriod = it,
                        termBreaks = DateSet.empty(),
                    )
                )
            }
            val areaId = tx.insert(DevCareArea())
            unitId1 = tx.insert(DevDaycare(areaId = areaId, name = "Koulu A"))
            unitId2 = tx.insert(DevDaycare(areaId = areaId, name = "Koulu B"))

            val guardian = DevPerson(lastName = "Testiläinen", firstName = "Matti")
            val guardianId = tx.insert(guardian, DevPersonType.ADULT)

            val child1 =
                DevPerson(
                    lastName = "Testiläinen",
                    firstName = "Teppo",
                    dateOfBirth = LocalDate.of(2019, 1, 1),
                    ssn = null,
                )
            val childId1 = tx.insert(child1, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardianId, childId = childId1))
            tx.insertTestApplication(
                type = ApplicationType.PRESCHOOL,
                status = ApplicationStatus.WAITING_UNIT_CONFIRMATION,
                guardianId = guardianId,
                childId = childId1,
                document =
                    DaycareFormV0(
                        type = ApplicationType.PRESCHOOL,
                        child = Child(dateOfBirth = child1.dateOfBirth),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(unitId1)),
                        preferredStartDate = nextTermStart,
                    ),
            )

            val child2 =
                DevPerson(
                    lastName = "Testiläinen",
                    firstName = "Seppo",
                    dateOfBirth = LocalDate.of(2019, 1, 2),
                    ssn = null,
                )
            val childId2 = tx.insert(child2, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardianId, childId = childId2))
            tx.insertTestApplication(
                type = ApplicationType.PRESCHOOL,
                status = ApplicationStatus.WAITING_UNIT_CONFIRMATION,
                guardianId = guardianId,
                childId = childId2,
                document =
                    DaycareFormV0(
                        type = ApplicationType.PRESCHOOL,
                        child = Child(dateOfBirth = child2.dateOfBirth),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(unitId2)),
                        preferredStartDate = nextTermStart,
                    ),
            )

            val child3 =
                DevPerson(
                    lastName = "Testiläinen",
                    firstName = "Matti",
                    dateOfBirth = LocalDate.of(2019, 1, 2),
                    ssn = null,
                )
            val childId3 = tx.insert(child3, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardianId, childId = childId3))
            tx.insertTestApplication(
                type = ApplicationType.PRESCHOOL,
                status = ApplicationStatus.WAITING_UNIT_CONFIRMATION,
                guardianId = guardianId,
                childId = childId3,
                document =
                    DaycareFormV0(
                        type = ApplicationType.PRESCHOOL,
                        child = Child(dateOfBirth = child3.dateOfBirth),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(unitId2)),
                        preferredStartDate = currentTermStart,
                    ),
            )
        }
    }

    @Test
    fun `admin can see the report`() {
        val user =
            db.transaction { tx ->
                val employee = DevEmployee().copy(roles = setOf(UserRole.ADMIN))
                tx.insert(employee)
                employee.user
            }

        val rows =
            preschoolApplicationReport.getPreschoolApplicationReport(dbInstance(), user, clock)

        assertThat(rows)
            .extracting(
                { it.applicationUnitName },
                { it.childLastName },
                { it.childFirstName },
                { it.currentUnitName },
                { it.isDaycareAssistanceNeed },
            )
            .containsExactlyInAnyOrder(
                Tuple("Koulu A", "Testiläinen", "Teppo", null, false),
                Tuple("Koulu B", "Testiläinen", "Seppo", null, false),
            )
    }

    @Test
    fun `unit supervisor can see the report`() {
        val user =
            db.transaction { tx ->
                val employee = DevEmployee()
                tx.insert(employee, unitRoles = mapOf(unitId1 to UserRole.UNIT_SUPERVISOR))
                employee.user
            }

        val rows =
            preschoolApplicationReport.getPreschoolApplicationReport(dbInstance(), user, clock)

        assertThat(rows)
            .extracting(
                { it.applicationUnitName },
                { it.childLastName },
                { it.childFirstName },
                { it.currentUnitName },
                { it.isDaycareAssistanceNeed },
            )
            .containsExactly(Tuple("Koulu A", "Testiläinen", "Teppo", null, false))
    }

    @Test
    fun `staff cannot see the report`() {
        val user =
            db.transaction { tx ->
                val employee = DevEmployee()
                tx.insert(employee, unitRoles = mapOf(unitId1 to UserRole.STAFF))
                employee.user
            }

        val rows =
            preschoolApplicationReport.getPreschoolApplicationReport(dbInstance(), user, clock)

        assertThat(rows).isEmpty()
    }
}
