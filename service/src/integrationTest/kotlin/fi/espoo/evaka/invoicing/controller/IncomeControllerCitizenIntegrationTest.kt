// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestGuardian
import fi.espoo.evaka.shared.dev.insertTestIncome
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.upsertCitizenUser
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import fi.espoo.evaka.snDaycareContractDays15
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class IncomeControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var incomeControllerCitizen: IncomeControllerCitizen

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 23), LocalTime.of(21, 0)))

    private val testChild =
        DevPerson(
            id = ChildId(UUID.randomUUID()),
            dateOfBirth = LocalDate.of(2017, 6, 1),
            ssn = "010617A123U",
            firstName = "Ricky",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false
        )

    private val guardianEmail = "guardian@example.com"
    private lateinit var guardianId: PersonId
    private lateinit var childId: ChildId
    private lateinit var employeeId: EmployeeId
    private lateinit var employeeEvakaUserId: EvakaUserId
    private lateinit var guardianAuthenticatedUser: AuthenticatedUser.Citizen

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            guardianId = tx.insert(DevPerson(email = guardianEmail))
            tx.upsertCitizenUser(guardianId)
            guardianAuthenticatedUser =
                AuthenticatedUser.Citizen(guardianId, CitizenAuthLevel.STRONG)
            val areaId = tx.insert(DevCareArea())
            val daycareId = tx.insert(DevDaycare(areaId = areaId))
            childId = tx.insert(testChild).also { tx.insert(DevChild(testChild.id)) }
            tx.insertTestGuardian(DevGuardian(guardianId = guardianId, childId = childId))
            val placementStart = clock.today().minusMonths(2)
            val placementEnd = clock.today().plusMonths(2)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
            tx.insertServiceNeedOptions()
            tx.insertServiceNeed(
                placementId = placementId,
                startDate = placementStart,
                endDate = placementEnd,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
            employeeId = tx.insert(DevEmployee(roles = setOf(UserRole.SERVICE_WORKER)))
            tx.upsertEmployeeUser(employeeId)
            employeeEvakaUserId = EvakaUserId(employeeId.raw)
        }
    }

    @Test
    fun `expiring income date found`() {
        val expirationDate = clock.today().plusWeeks(4)
        db.transaction {
            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(6),
                    validTo = expirationDate
                )
            )
        }

        val expirationDates =
            incomeControllerCitizen.getExpiringIncome(
                dbInstance(),
                guardianAuthenticatedUser,
                clock
            )

        assertEquals(1, expirationDates.size)
        assertEquals(expirationDate, expirationDates.get(0))
    }
}
