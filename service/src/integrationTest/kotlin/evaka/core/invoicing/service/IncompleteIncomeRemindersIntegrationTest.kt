// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.FullApplicationTest
import evaka.core.emailclient.Email
import evaka.core.emailclient.MockEmailClient
import evaka.core.incomestatement.Gross
import evaka.core.incomestatement.IncomeSource
import evaka.core.incomestatement.IncomeStatementBody
import evaka.core.incomestatement.IncomeStatementStatus
import evaka.core.insertServiceNeedOptions
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.serviceneed.ShiftCareType
import evaka.core.serviceneed.insertServiceNeed
import evaka.core.shared.ChildId
import evaka.core.shared.PartnershipId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevFridgePartner
import evaka.core.shared.dev.DevIncome
import evaka.core.shared.dev.DevIncomeStatement
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.job.ScheduledJobs
import evaka.core.snDaycareContractDays15
import evaka.core.webpush.CitizenPushNotification
import evaka.core.webpush.getPlannedCitizenPushNotifications
import evaka.core.webpush.insertTestCitizenPushSubscription
import evaka.core.webpush.mockWebPushEndpoint
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class IncompleteIncomeRemindersIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 24), LocalTime.of(6, 45)))

    private val child =
        DevPerson(
            id = ChildId(UUID.randomUUID()),
            dateOfBirth = LocalDate.of(2017, 6, 1),
            ssn = "010617A123U",
        )
    private val headOfChild = DevPerson(email = "head@example.com")
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(headOfChild, DevPersonType.ADULT)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
            val placementStart = clock.today().minusYears(2)
            val placementEnd = clock.today().plusYears(2)
            tx.insert(
                DevFridgeChild(
                    childId = child.id,
                    headOfChild = headOfChild.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
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
        }
    }

    @Test
    fun `reminder is sent when the current income has been incomplete for a year`() {
        insertIncompleteIncome(headOfChild.id, validFrom = clock.today().minusYears(1))

        assertEquals(1, sendReminders().size)
        assertEquals(
            listOf(IncomeNotificationType.INCOMPLETE_INCOME_REMINDER),
            getIncomeNotificationTypes(headOfChild.id),
        )
    }

    @Test
    fun `reminder is pushed to a guardian who has only a push device`() {
        db.transaction { tx ->
            tx.execute { sql("UPDATE person SET email = NULL WHERE id = ${bind(headOfChild.id)}") }
            tx.insertTestCitizenPushSubscription(headOfChild.id, mockWebPushEndpoint(httpPort))
        }
        insertIncompleteIncome(headOfChild.id, validFrom = clock.today().minusYears(1))

        assertEquals(0, sendReminders().size)
        assertEquals(
            listOf(
                CitizenPushNotification.Income(
                    IncomeNotificationType.INCOMPLETE_INCOME_REMINDER,
                    expirationDate = null,
                    deadline = null,
                )
            ),
            db.read { it.getPlannedCitizenPushNotifications() },
        )
    }

    @Test
    fun `notification is recorded for a guardian without email or push so the calendar toast is shown`() {
        db.transaction { tx ->
            tx.execute { sql("UPDATE person SET email = NULL WHERE id = ${bind(headOfChild.id)}") }
        }
        insertIncompleteIncome(headOfChild.id, validFrom = clock.today().minusYears(1))

        assertEquals(0, sendReminders().size)
        assertEquals(emptyList(), db.read { it.getPlannedCitizenPushNotifications() })
        assertEquals(
            listOf(IncomeNotificationType.INCOMPLETE_INCOME_REMINDER),
            getIncomeNotificationTypes(headOfChild.id),
        )
        assertNotNull(
            db.read { it.getActiveIncompleteIncomeReminderCta(headOfChild.id, clock.now()) }
        )
    }

    @Test
    fun `reminder is not sent when the income has been incomplete for less than a year`() {
        insertIncompleteIncome(headOfChild.id, validFrom = clock.today().minusYears(1).plusDays(1))

        assertEquals(0, sendReminders().size)
    }

    @Test
    fun `reminder is not sent when the current income is not incomplete`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = headOfChild.id,
                    effect = IncomeEffect.MAX_FEE_ACCEPTED,
                    validFrom = clock.today().minusYears(2),
                    validTo = null,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
            )
        }

        assertEquals(0, sendReminders().size)
    }

    @Test
    fun `reminder is sent once a year`() {
        insertIncompleteIncome(headOfChild.id, validFrom = clock.today().minusYears(1))

        assertEquals(1, sendReminders().size)
        assertEquals(0, sendReminders().size)

        clock.tick(Duration.ofDays(334))
        assertEquals(0, sendReminders().size)

        clock.tick(Duration.ofDays(31))
        assertEquals(1, sendReminders().size)
        assertEquals(2, getIncomeNotificationTypes(headOfChild.id).size)
    }

    @Test
    fun `reminder is not sent when an income statement was sent within a year`() {
        insertIncompleteIncome(headOfChild.id, validFrom = clock.today().minusYears(2))
        db.transaction {
            it.insert(
                DevIncomeStatement(
                    personId = headOfChild.id,
                    data = grossIncome(clock.today().minusMonths(11)),
                    status = IncomeStatementStatus.SENT,
                    createdAt = clock.now().minusMonths(11),
                    modifiedAt = clock.now().minusMonths(11),
                    sentAt = clock.now().minusMonths(11),
                )
            )
        }

        assertEquals(0, sendReminders().size)
    }

    @Test
    fun `reminder is not sent when the partner has accepted the highest fee, but the partner gets one when incomplete`() {
        val partner = DevPerson(email = "partner@example.com")
        db.transaction {
            it.insert(partner, DevPersonType.ADULT)
            insertPartnership(it, headOfChild.id, partner.id)
            it.insert(
                DevIncome(
                    personId = partner.id,
                    effect = IncomeEffect.MAX_FEE_ACCEPTED,
                    validFrom = clock.today().minusYears(2),
                    validTo = null,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
            )
        }
        insertIncompleteIncome(headOfChild.id, validFrom = clock.today().minusYears(2))

        assertEquals(0, sendReminders().size)

        db.transaction {
            it.execute { sql("DELETE FROM income WHERE person_id = ${bind(partner.id)}") }
        }
        insertIncompleteIncome(partner.id, validFrom = clock.today().minusYears(2))

        assertEquals(2, sendReminders().size)
        assertEquals(1, getIncomeNotificationTypes(partner.id).size)
    }

    @Test
    fun `reminder is not sent when the placement is not invoiced`() {
        db.transaction {
            it.execute {
                sql(
                    "UPDATE daycare SET provider_type = 'MUNICIPAL', invoiced_by_municipality = false WHERE id = ${bind(daycare.id)}"
                )
            }
        }
        insertIncompleteIncome(headOfChild.id, validFrom = clock.today().minusYears(2))

        assertEquals(0, sendReminders().size)
    }

    @Test
    fun `reminder is not sent when the incomplete income has an end date`() {
        // staff have typically already entered the next income after a finite incomplete one
        insertIncompleteIncome(
            headOfChild.id,
            validFrom = clock.today().minusYears(2),
            validTo = clock.today().plusMonths(6),
        )

        assertEquals(0, sendReminders().size)
    }

    private fun insertIncompleteIncome(
        personId: PersonId,
        validFrom: LocalDate,
        validTo: LocalDate? = null,
    ) {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = personId,
                    effect = IncomeEffect.INCOMPLETE,
                    validFrom = validFrom,
                    validTo = validTo,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
            )
        }
    }

    private fun insertPartnership(
        tx: Database.Transaction,
        a: PersonId,
        b: PersonId,
    ) {
        val partnershipId = PartnershipId(UUID.randomUUID())
        val start = clock.today().minusYears(2)
        tx.insert(DevFridgePartner(partnershipId, 1, 2, a, start, null, clock.now()))
        tx.insert(DevFridgePartner(partnershipId, 2, 1, b, start, null, clock.now()))
    }

    private fun grossIncome(startDate: LocalDate) =
        IncomeStatementBody.Income(
            startDate = startDate,
            endDate = null,
            gross =
                Gross.Income(
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    estimatedMonthlyIncome = 42,
                    otherIncome = emptySet(),
                    otherIncomeInfo = "",
                ),
            entrepreneur = null,
            student = false,
            alimonyPayer = false,
            otherInfo = "",
            attachmentIds = emptyList(),
        )

    private fun sendReminders(): List<Email> {
        scheduledJobs.sendIncompleteIncomeReminders(db, clock)
        asyncJobRunner.runPendingJobsSync(clock)
        val emails = MockEmailClient.emails
        MockEmailClient.clear()
        return emails
    }

    private fun getIncomeNotificationTypes(receiverId: PersonId): List<IncomeNotificationType> =
        db.read { tx ->
            tx.getIncomeNotifications(receiverId).map { it.notificationType }
        }
}
