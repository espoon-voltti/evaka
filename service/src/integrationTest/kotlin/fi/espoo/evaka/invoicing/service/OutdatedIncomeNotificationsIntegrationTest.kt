// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.incomestatement.IncomeStatementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevIncomeStatement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertIncomeStatement
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestGuardian
import fi.espoo.evaka.shared.dev.insertTestIncome
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.upsertCitizenUser
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OutdatedIncomeNotificationsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 23), LocalTime.of(21, 0)))

    private val guardianEmail = "guardian@example.com"
    private lateinit var guardianId: PersonId
    private lateinit var childId: ChildId
    private lateinit var employeeId: EmployeeId
    private lateinit var employeeEvakaUserId: EvakaUserId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            guardianId = tx.insertTestPerson(DevPerson(email = guardianEmail))
            tx.upsertCitizenUser(guardianId)
            val areaId = tx.insertTestCareArea(DevCareArea())
            val daycareId = tx.insertTestDaycare(DevDaycare(areaId = areaId))
            childId = tx.insertTestPerson(DevPerson()).also { tx.insertTestChild(DevChild(it)) }
            tx.insertTestGuardian(DevGuardian(guardianId = guardianId, childId = childId))
            tx.insertTestPlacement(
                DevPlacement(
                    childId = childId,
                    unitId = daycareId,
                    startDate = clock.today().minusMonths(2),
                    endDate = clock.today().plusMonths(2)
                )
            )
            employeeId = tx.insertTestEmployee(DevEmployee(roles = setOf(UserRole.SERVICE_WORKER)))
            tx.upsertEmployeeUser(employeeId)
            employeeEvakaUserId = EvakaUserId(employeeId.raw)
        }
    }

    @Test
    fun `only last income is considered when finding outdated incomes`() {
        db.transaction {
            // This income is expiring, but not notified because there is another income starting
            // afterwards
            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today()
                )
            )

            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().plusDays(1),
                    validTo = clock.today().plusMonths(6)
                )
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is notified 2 weeks beforehand`() {
        db.transaction {
            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusDays(13)
                )
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(guardianId).size)
    }

    @Test
    fun `expiring income is not notified if there is a new income statement`() {
        val incomeExpirationDate = clock.today().plusDays(13)

        db.transaction {
            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = incomeExpirationDate.minusMonths(1),
                    validTo = incomeExpirationDate
                )
            )

            it.insertIncomeStatement(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = guardianId,
                    startDate = incomeExpirationDate.plusDays(1),
                    type = IncomeStatementType.INCOME,
                    grossEstimatedMonthlyIncome = 42
                )
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `If there is no placement no notification is sent`() {
        db.transaction {
            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusDays(13)
                )
            )

            it.createUpdate("DELETE FROM placement WHERE child_id = :personId")
                .bind("personId", childId)
                .execute()
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `if first notification was already sent and it is not yet time for the second notification a new first notification is not sent`() {
        db.transaction {
            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusDays(13)
                )
            )
        }

        val mails = getEmails()
        assertEquals(1, mails.size)
        assertTrue(
            mails
                .get(0)
                .textBody
                .contains(
                    "Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain"
                )
        )

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `second notification is only sent after first notification`() {
        db.transaction {
            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusDays(6)
                )
            )
        }

        val mails = getEmails()
        assertEquals(1, mails.size)
        assertTrue(
            mails
                .get(0)
                .textBody
                .contains(
                    "Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain"
                )
        )

        val secondMails = getEmails()
        assertEquals(1, secondMails.size)
        assertTrue(
            secondMails.get(0).textBody.contains("Ette ole vielä toimittaneet uusia tulotietoja")
        )
    }

    @Test
    fun `Final notification is sent after income expires`() {
        db.transaction {
            it.insertTestIncome(
                DevIncome(
                    personId = guardianId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today()
                )
            )

            it.createIncomeNotification(
                receiverId = guardianId,
                IncomeNotificationType.INITIAL_EMAIL
            )
            it.createIncomeNotification(
                receiverId = guardianId,
                IncomeNotificationType.REMINDER_EMAIL
            )
        }

        val mails = getEmails()
        assertEquals(1, mails.size)
        assertTrue(
            mails
                .get(0)
                .textBody
                .contains("Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan")
        )

        assertEquals(0, getEmails().size)
    }

    private fun getEmails(): List<MockEmail> {
        scheduledJobs.sendOutdatedIncomeNotifications(db, clock)
        asyncJobRunner.runPendingJobsSync(clock)
        val emails = MockEmailClient.emails
        MockEmailClient.clear()
        return emails
    }

    private fun getIncomeNotifications(receiverId: PersonId): List<IncomeNotification> =
        db.read { it.getIncomeNotifications(receiverId) }
}
