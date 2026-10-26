// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.FullApplicationTest
import evaka.core.incomestatement.IncomeStatementBody
import evaka.core.incomestatement.IncomeStatementStatus
import evaka.core.insertServiceNeedOptions
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.invoicing.service.createIncomeNotification
import evaka.core.serviceneed.ShiftCareType
import evaka.core.serviceneed.insertServiceNeed
import evaka.core.shared.ChildId
import evaka.core.shared.IncomeNotificationId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevIncome
import evaka.core.shared.dev.DevIncomeStatement
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.upsertEmployeeUser
import evaka.core.snDaycareContractDays15
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            restrictedDetailsEnabled = false,
        )

    private val guardian = DevPerson(email = "guardian@example.com")
    private val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            val areaId = tx.insert(DevCareArea())
            val daycareId = tx.insert(DevDaycare(areaId = areaId))
            tx.insert(testChild, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = testChild.id))
            val placementStart = clock.today().minusMonths(2)
            val placementEnd = clock.today().plusMonths(2)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild.id,
                        unitId = daycareId,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insertServiceNeedOptions()
            tx.insertServiceNeed(
                placementId = placementId,
                startDate = placementStart,
                endDate = placementEnd,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
                confirmedBy = null,
                confirmedAt = null,
            )
            tx.insert(employee)
            tx.upsertEmployeeUser(employee.id)
        }
    }

    @Test
    fun `expiring income date found`() {
        val expirationDate = clock.today().plusWeeks(4)
        db.transaction {
            it.insert(
                DevIncome(
                    personId = guardian.id,
                    modifiedBy = employee.evakaUserId,
                    validFrom = clock.today().minusMonths(6),
                    validTo = expirationDate,
                )
            )
            it.insert(
                DevFridgeChild(
                    headOfChild = guardian.id,
                    childId = testChild.id,
                    startDate = clock.today().minusMonths(6),
                    endDate = expirationDate.plusMonths(6),
                )
            )
        }

        assertEquals(expirationDate, getIncomeCtas().expiringIncomeDate)
    }

    @Test
    fun `dismissed expiring income cta is not shown again for the same expiration date`() {
        val expirationDate = clock.today().plusWeeks(3)
        db.transaction {
            it.insert(
                DevIncome(
                    personId = guardian.id,
                    modifiedBy = employee.evakaUserId,
                    validFrom = clock.today().minusMonths(6),
                    validTo = expirationDate,
                )
            )
            it.insert(
                DevFridgeChild(
                    headOfChild = guardian.id,
                    childId = testChild.id,
                    startDate = clock.today().minusMonths(6),
                    endDate = expirationDate.plusMonths(6),
                )
            )
        }
        assertEquals(expirationDate, getIncomeCtas().expiringIncomeDate)

        incomeControllerCitizen.dismissExpiringIncomeCta(
            dbInstance(),
            guardian.user(CitizenAuthLevel.WEAK),
            clock,
            IncomeControllerCitizen.DismissExpiringIncomeCtaRequest(expirationDate),
        )
        assertNull(getIncomeCtas().expiringIncomeDate)

        // the income is extended but expires again soon: a new expiration date is shown again
        val newExpirationDate = expirationDate.plusWeeks(1)
        db.transaction {
            it.execute {
                sql(
                    "UPDATE income SET valid_to = ${bind(newExpirationDate)} WHERE person_id = ${bind(guardian.id)}"
                )
            }
        }
        assertEquals(newExpirationDate, getIncomeCtas().expiringIncomeDate)
    }

    @Test
    fun `incomplete income reminder cta is shown for four weeks until dismissed`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = guardian.id,
                    effect = IncomeEffect.INCOMPLETE,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusYears(1),
                    validTo = null,
                )
            )
        }
        createReminder(guardian.id, clock.now().minusWeeks(5))
        assertNull(getIncomeCtas().incompleteIncomeReminder)

        val reminder = createReminder(guardian.id, clock.now().minusWeeks(3))
        assertEquals(reminder, getIncomeCtas().incompleteIncomeReminder)

        assertThrows<NotFound> { dismissReminder(IncomeNotificationId(UUID.randomUUID())) }

        dismissReminder(reminder)
        dismissReminder(reminder)
        assertNull(getIncomeCtas().incompleteIncomeReminder)
    }

    @Test
    fun `incomplete income reminder cta is not shown after an income statement has been sent`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = guardian.id,
                    effect = IncomeEffect.INCOMPLETE,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusYears(1),
                    validTo = null,
                )
            )
        }
        val reminder = createReminder(guardian.id, clock.now().minusDays(1))
        assertEquals(reminder, getIncomeCtas().incompleteIncomeReminder)

        db.transaction {
            it.insert(
                DevIncomeStatement(
                    personId = guardian.id,
                    data =
                        IncomeStatementBody.HighestFee(startDate = clock.today(), endDate = null),
                    status = IncomeStatementStatus.SENT,
                    createdAt = clock.now(),
                    modifiedAt = clock.now(),
                    sentAt = clock.now(),
                )
            )
        }
        assertNull(getIncomeCtas().incompleteIncomeReminder)
    }

    @Test
    fun `another person's incomplete income reminder cannot be dismissed`() {
        val otherPerson = DevPerson()
        db.transaction { it.insert(otherPerson, DevPersonType.ADULT) }
        val othersReminder = createReminder(otherPerson.id, clock.now())

        assertThrows<NotFound> { dismissReminder(othersReminder) }
    }

    private fun getIncomeCtas() =
        incomeControllerCitizen.getIncomeCtas(
            dbInstance(),
            guardian.user(CitizenAuthLevel.WEAK),
            clock,
        )

    private fun dismissReminder(id: IncomeNotificationId) =
        incomeControllerCitizen.dismissIncompleteIncomeReminderCta(
            dbInstance(),
            guardian.user(CitizenAuthLevel.WEAK),
            clock,
            id,
        )

    private fun createReminder(personId: PersonId, created: HelsinkiDateTime) = db.transaction {
        it.createIncomeNotification(
            personId,
            IncomeNotificationType.INCOMPLETE_INCOME_REMINDER,
            created,
        )
    }
}
