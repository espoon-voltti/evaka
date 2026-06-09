// SPDX-FileCopyrightText: 2017-2024 City of Espoo
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
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ShiftCareType
import evaka.core.serviceneed.insertServiceNeed
import evaka.core.shared.ChildId
import evaka.core.shared.IncomeStatementId
import evaka.core.shared.PartnershipId
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NewCustomerIncomeNotificationIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val child =
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

    private val fridgeHeadOfChildEmail = "fridge_hoc@example.com"
    private val fridgePartnerEmail = "fridge_partner@example.com"

    private val fridgeHeadOfChild = DevPerson(email = fridgeHeadOfChildEmail)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    private lateinit var clock: MockEvakaClock
    private lateinit var placementStart: LocalDate
    private lateinit var placementEnd: LocalDate

    @BeforeEach
    fun beforeEach() {
        MockEmailClient.clear()
        clock = MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 2, 1), LocalTime.of(21, 0)))
        placementStart = clock.today().plusWeeks(2)
        placementEnd = clock.today().plusMonths(6)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(child, DevPersonType.CHILD)
            tx.insert(fridgeHeadOfChild, DevPersonType.ADULT)

            tx.insert(
                DevFridgeChild(
                    childId = child.id,
                    headOfChild = fridgeHeadOfChild.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                )
            )

            tx.insertServiceNeedOptions()
        }
    }

    private fun insertPlacement(
        child: DevPerson,
        start: LocalDate,
        end: LocalDate,
        type: PlacementType = PlacementType.DAYCARE,
        createdAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    ): PlacementId {
        return db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = type,
                    startDate = start,
                    endDate = end,
                    createdAt = createdAt,
                )
            )
        }
    }

    private fun insertServiceNeed(placementId: PlacementId, start: LocalDate, end: LocalDate) {
        db.transaction { tx ->
            tx.insertServiceNeed(
                placementId = placementId,
                startDate = start,
                endDate = end,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
                confirmedBy = null,
                confirmedAt = null,
            )
        }
    }

    @Test
    fun `notification is sent when placement starts in current month`() {
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
        assertEquals(
            IncomeNotificationType.NEW_CUSTOMER,
            getIncomeNotifications(fridgeHeadOfChild.id)[0].notificationType,
        )
    }

    @Test
    fun `does not send a second new customer notification on a later day`() {
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)

        clock.tick(Duration.ofDays(1))

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `notification is sent when the job runs later in the month for a placement starting later this month`() {
        clock.tick(Duration.ofDays(9)) // 2024-02-10
        val laterStart = clock.today().plusDays(10) // 2024-02-20, still the current month
        val placementId = insertPlacement(child, laterStart, placementEnd)
        insertServiceNeed(placementId, laterStart, placementEnd)

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `placement starting this month is not notified again the following month`() {
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(1, getEmails().size)

        clock.tick(Duration.ofDays(35)) // 2024-03-07, next calendar month

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `notification is sent for a retroactive placement that started in the previous month`() {
        val start = clock.today().minusWeeks(3) // 2024-01-11, previous month
        val placementId = insertPlacement(child, start, placementEnd)
        insertServiceNeed(placementId, start, placementEnd)

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `notification is sent for a placement created on the last day of the previous month`() {
        // The daily job runs at 06:45, so a placement created later on the last day of
        // the month is first evaluated on the first day of the next month. Its created_at
        // is then "before the current month", and its start is not in the current month
        // either, so it must still be picked up.
        val createdAt = HelsinkiDateTime.of(LocalDate.of(2024, 1, 31), LocalTime.of(20, 0))
        val start = LocalDate.of(2024, 1, 31) // last day of the previous month
        val placementId = insertPlacement(child, start, placementEnd, createdAt = createdAt)
        insertServiceNeed(placementId, start, placementEnd)

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `notification is not sent for a retroactive placement that already ended`() {
        val start = clock.today().minusWeeks(3) // 2024-01-11
        val end = clock.today().minusDays(1) // 2024-01-31, ended before today
        val placementId = insertPlacement(child, start, end)
        insertServiceNeed(placementId, start, end)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notification is not sent for an old placement created before this month`() {
        val start = clock.today().minusMonths(2) // 2023-12-01
        val placementId =
            insertPlacement(
                child,
                start,
                placementEnd,
                createdAt = HelsinkiDateTime.of(start, LocalTime.of(12, 0)),
            )
        insertServiceNeed(placementId, start, placementEnd)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `family with two retroactive siblings entered this month is notified once`() {
        val otherChild = DevPerson()
        db.transaction { tx ->
            tx.insert(otherChild, DevPersonType.CHILD)
            tx.insert(
                DevFridgeChild(
                    childId = otherChild.id,
                    headOfChild = fridgeHeadOfChild.id,
                    startDate = clock.today().minusYears(1),
                    endDate = clock.today().plusYears(1),
                )
            )
        }
        val start = clock.today().minusWeeks(3) // 2024-01-11, previous month
        val p1 = insertPlacement(child, start, placementEnd)
        insertServiceNeed(p1, start, placementEnd)
        val p2 = insertPlacement(otherChild, start, placementEnd)
        insertServiceNeed(p2, start, placementEnd)

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `sibling placement entered on the last day of the previous month does not suppress the notification`() {
        // A sibling's placement was entered on the last day of the previous month (and has
        // already ended). Because it was entered within the last month, it must not count
        // as a previous placement that marks the family as an existing customer, so the
        // notification for the child starting this month is still sent.
        val sibling = DevPerson()
        db.transaction { tx ->
            tx.insert(sibling, DevPersonType.CHILD)
            tx.insert(
                DevFridgeChild(
                    childId = sibling.id,
                    headOfChild = fridgeHeadOfChild.id,
                    startDate = clock.today().minusYears(1),
                    endDate = clock.today().plusYears(1),
                )
            )
        }
        val siblingCreatedAt = HelsinkiDateTime.of(LocalDate.of(2024, 1, 31), LocalTime.of(20, 0))
        val siblingStart = LocalDate.of(2024, 1, 11)
        val siblingEnd = LocalDate.of(2024, 1, 20) // already ended
        val siblingPlacement =
            insertPlacement(sibling, siblingStart, siblingEnd, createdAt = siblingCreatedAt)
        insertServiceNeed(siblingPlacement, siblingStart, siblingEnd)

        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `notifications are not sent when placement does not start in current month`() {
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)
        clock.tick(Duration.ofDays(-1))
        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notification is not sent when non-invoiced placement starts in current month`() {
        insertPlacement(child, placementStart, placementEnd, PlacementType.PRESCHOOL)
        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notifications are not sent when placement for other child exists`() {
        val otherChild = DevPerson()
        db.transaction { tx ->
            tx.insert(otherChild, DevPersonType.CHILD)
            tx.insert(
                DevFridgeChild(
                    childId = otherChild.id,
                    headOfChild = fridgeHeadOfChild.id,
                    startDate = clock.today().minusYears(1),
                    endDate = clock.today().plusYears(1),
                )
            )
        }

        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        val otherPlacementStart = clock.today().minusYears(1)
        val otherPlacementEnd = clock.today().plusYears(1)
        val otherPlacementId =
            insertPlacement(
                otherChild,
                otherPlacementStart,
                otherPlacementEnd,
                createdAt = HelsinkiDateTime.of(otherPlacementStart, LocalTime.of(12, 0)),
            )
        insertServiceNeed(otherPlacementId, otherPlacementStart, otherPlacementEnd)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notification is sent when non-invoiced placement for other child exists`() {
        val otherChild = DevPerson()
        db.transaction { tx ->
            tx.insert(otherChild, DevPersonType.CHILD)
            tx.insert(
                DevFridgeChild(
                    childId = otherChild.id,
                    headOfChild = fridgeHeadOfChild.id,
                    startDate = clock.today().minusYears(1),
                    endDate = clock.today().plusYears(1),
                )
            )
        }

        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        insertPlacement(
            otherChild,
            clock.today().minusYears(1),
            clock.today().plusYears(1),
            PlacementType.PRESCHOOL,
        )

        assertEquals(1, getEmails().size)
    }

    @Test
    fun `notifications are sent when placement for other child also starts in same month`() {
        val otherChild = DevPerson()
        db.transaction { tx ->
            tx.insert(otherChild, DevPersonType.CHILD)
            tx.insert(
                DevFridgeChild(
                    childId = otherChild.id,
                    headOfChild = fridgeHeadOfChild.id,
                    startDate = clock.today().minusYears(1),
                    endDate = clock.today().plusYears(1),
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        val otherPlacementStart = clock.today().plusWeeks(2)
        val otherPlacementEnd = clock.today().plusMonths(6)
        val otherPlacementId = insertPlacement(otherChild, otherPlacementStart, otherPlacementEnd)
        insertServiceNeed(otherPlacementId, otherPlacementStart, otherPlacementEnd)

        assertEquals(1, getEmails().size)
    }

    @Test
    fun `notification is sent to fridge partner also`() {
        val fridgePartner = DevPerson(email = fridgePartnerEmail)
        db.transaction { tx ->
            tx.insert(fridgePartner, DevPersonType.ADULT)
            val partnershipId = PartnershipId(UUID.randomUUID())
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 1,
                    otherIndx = 2,
                    personId = fridgeHeadOfChild.id,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    createdAt = clock.now(),
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = fridgePartner.id,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    createdAt = clock.now(),
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(2, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
        assertEquals(1, getIncomeNotifications(fridgePartner.id).size)
        assertEquals(
            IncomeNotificationType.NEW_CUSTOMER,
            getIncomeNotifications(fridgePartner.id)[0].notificationType,
        )
    }

    @Test
    fun `notifications are not sent when placement for other child for partner exists`() {
        val fridgePartner = DevPerson(email = fridgePartnerEmail)
        val partnersChild = DevPerson()

        db.transaction { tx ->
            tx.insert(fridgePartner, DevPersonType.ADULT)
            tx.insert(partnersChild, DevPersonType.CHILD)

            val partnershipId = PartnershipId(UUID.randomUUID())

            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 1,
                    otherIndx = 2,
                    personId = fridgeHeadOfChild.id,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    createdAt = clock.now(),
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = fridgePartner.id,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    createdAt = clock.now(),
                )
            )

            tx.insert(
                DevFridgeChild(
                    childId = partnersChild.id,
                    headOfChild = fridgePartner.id,
                    startDate = clock.today().minusYears(1),
                    endDate = clock.today().plusYears(1),
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        val otherPlacementStart = clock.today().minusYears(2)
        val otherPlacementEnd = clock.today().plusMonths(6)
        val otherPlacementId =
            insertPlacement(
                partnersChild,
                otherPlacementStart,
                otherPlacementEnd,
                createdAt = HelsinkiDateTime.of(otherPlacementStart, LocalTime.of(12, 0)),
            )
        insertServiceNeed(otherPlacementId, otherPlacementStart, otherPlacementEnd)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notifications are not sent if there is a new unhandled income statement`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)

        db.transaction {
            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(incomeExpirationDate.plusDays(1)),
                    status = IncomeStatementStatus.SENT,
                    handlerId = null,
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notifications are sent if there is a handled income statement`() {
        val employee = DevEmployee()

        db.transaction {
            it.insert(employee)

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(clock.today()),
                    status = IncomeStatementStatus.HANDLED,
                    handlerId = employee.id,
                    handledAt = clock.now(),
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(1, getEmails().size)
    }

    @Test
    fun `expiring income is notified 4 weeks beforehand`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)

        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusWeeks(4),
                    validTo = incomeExpirationDate,
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `notifications are not sent if there is a valid income`() {
        val incomeExpirationDate = clock.today().plusWeeks(4).plusDays(1)

        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusWeeks(4),
                    validTo = incomeExpirationDate,
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notifications are not sent if there is an indefinite income`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusYears(1),
                    validTo = null,
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is not notified if there is a new unhandled income statement`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)

        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusWeeks(4),
                    validTo = incomeExpirationDate,
                )
            )

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(clock.today()),
                    status = IncomeStatementStatus.SENT,
                    handlerId = null,
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is not notified if there is a new income statement being handled`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)

        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusWeeks(4),
                    validTo = incomeExpirationDate,
                )
            )

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(clock.today()),
                    status = IncomeStatementStatus.HANDLING,
                    handlerId = null,
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is notified if there is a handled income statement`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)
        val employee = DevEmployee()

        db.transaction {
            it.insert(employee)

            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusWeeks(4),
                    validTo = incomeExpirationDate,
                )
            )

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(clock.today()),
                    status = IncomeStatementStatus.HANDLED,
                    handlerId = employee.id,
                    handledAt = clock.now(),
                )
            )
        }
        val placementId = insertPlacement(child, placementStart, placementEnd)
        insertServiceNeed(placementId, placementStart, placementEnd)

        assertEquals(1, getEmails().size)
    }

    private fun getEmails(): List<Email> {
        scheduledJobs.sendNewCustomerIncomeNotifications(db, clock)
        asyncJobRunner.runPendingJobsSync(clock)
        val emails = MockEmailClient.emails
        return emails
    }

    private fun getIncomeNotifications(receiverId: PersonId): List<IncomeNotification> = db.read {
        it.getIncomeNotifications(receiverId)
    }

    private fun createGrossIncome(startDate: LocalDate) =
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
}
