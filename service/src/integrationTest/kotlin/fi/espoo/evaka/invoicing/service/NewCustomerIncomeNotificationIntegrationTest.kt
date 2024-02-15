// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevFridgePartner
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import fi.espoo.evaka.snDaycareContractDays15
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

    private lateinit var clock: MockEvakaClock

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

    private val fridgeHeadOfChildEmail = "fridge_hoc@example.com"
    private val fridgePartnerEmail = "fridge_partner@example.com"
    private lateinit var fridgeHeadOfChildId: PersonId
    private lateinit var childId: ChildId
    private lateinit var employeeId: EmployeeId
    private lateinit var employeeEvakaUserId: EvakaUserId
    private lateinit var daycareId: DaycareId

    @BeforeEach
    fun beforeEach() {
        MockEmailClient.clear()
        clock = MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 2, 1), LocalTime.of(21, 0)))

        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            fridgeHeadOfChildId =
                tx.insert(DevPerson(email = fridgeHeadOfChildEmail), DevPersonType.ADULT)
            daycareId = tx.insert(DevDaycare(areaId = areaId))
            childId = tx.insert(testChild, DevPersonType.CHILD)
            val placementStart = clock.today().plusWeeks(2)
            val placementEnd = clock.today().plusMonths(6)

            tx.insert(
                DevFridgeChild(
                    childId = childId,
                    headOfChild = fridgeHeadOfChildId,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1)
                )
            )
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
    fun `notification is sent when placement starts in current month`() {
        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChildId).size)
        assertEquals(
            IncomeNotificationType.NEW_CUSTOMER,
            getIncomeNotifications(fridgeHeadOfChildId)[0].notificationType
        )
    }

    @Test
    fun `notifications are not sent when placement does not start in current month`() {
        clock.tick(Duration.ofDays(-1))
        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notifications are not sent when placement for other child exists`() {
        db.transaction { tx ->
            val otherChild =
                DevPerson(
                    id = ChildId(UUID.randomUUID()),
                    dateOfBirth = LocalDate.of(2015, 12, 1),
                    ssn = "011215A9926",
                    firstName = "Jackie",
                    lastName = "Doe",
                    streetAddress = "Kamreerintie 2",
                    postalCode = "02770",
                    postOffice = "Espoo",
                    restrictedDetailsEnabled = false
                )

            val otherChildId = tx.insert(otherChild, DevPersonType.CHILD)
            val otherPlacementStart = clock.today().minusYears(1)
            val otherPlacementEnd = clock.today().plusYears(1)
            tx.insert(
                DevFridgeChild(
                    childId = otherChildId,
                    headOfChild = fridgeHeadOfChildId,
                    startDate = clock.today().minusYears(1),
                    endDate = clock.today().plusYears(1)
                )
            )
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = otherChildId,
                        unitId = daycareId,
                        startDate = otherPlacementStart,
                        endDate = otherPlacementEnd
                    )
                )
            tx.insertServiceNeed(
                placementId = placementId,
                startDate = otherPlacementStart,
                endDate = otherPlacementEnd,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `notification is sent to fridge partner also`() {
        lateinit var fridgePartnerId: PersonId
        db.transaction { tx ->
            fridgePartnerId = tx.insert(DevPerson(email = fridgePartnerEmail), DevPersonType.ADULT)
            val partnershipId = PartnershipId(UUID.randomUUID())
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 1,
                    otherIndx = 2,
                    personId = fridgeHeadOfChildId,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    createdAt = clock.now()
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = fridgePartnerId,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    createdAt = clock.now()
                )
            )
        }

        assertEquals(2, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChildId).size)
        assertEquals(1, getIncomeNotifications(fridgePartnerId).size)
        assertEquals(
            IncomeNotificationType.NEW_CUSTOMER,
            getIncomeNotifications(fridgePartnerId)[0].notificationType
        )
    }

    @Test
    fun `notifications are not sent when placement for other child for partner exists`() {
        db.transaction { tx ->
            val partnersChild =
                DevPerson(
                    id = ChildId(UUID.randomUUID()),
                    dateOfBirth = LocalDate.of(2018, 8, 1),
                    ssn = "010814A953E",
                    firstName = "Joey",
                    lastName = "Doe",
                    streetAddress = "Kamreerintie 2",
                    postalCode = "02770",
                    postOffice = "Espoo",
                    restrictedDetailsEnabled = false
                )
            val partnersChildId = tx.insert(partnersChild, DevPersonType.CHILD)
            val fridgePartnerId =
                tx.insert(DevPerson(email = fridgePartnerEmail), DevPersonType.ADULT)
            val partnershipId = PartnershipId(UUID.randomUUID())
            val partnerPlacementStart = clock.today().minusYears(2)
            val partnerPlacementEnd = clock.today().plusMonths(6)

            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 1,
                    otherIndx = 2,
                    personId = fridgeHeadOfChildId,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    createdAt = clock.now()
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = fridgePartnerId,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    createdAt = clock.now()
                )
            )

            tx.insert(
                DevFridgeChild(
                    childId = partnersChildId,
                    headOfChild = fridgePartnerId,
                    startDate = clock.today().minusYears(1),
                    endDate = clock.today().plusYears(1)
                )
            )
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = partnersChildId,
                        unitId = daycareId,
                        startDate = partnerPlacementStart,
                        endDate = partnerPlacementEnd
                    )
                )
            tx.insertServiceNeed(
                placementId = placementId,
                startDate = partnerPlacementStart,
                endDate = partnerPlacementEnd,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
        }

        assertEquals(0, getEmails().size)
    }

    private fun getEmails(): List<Email> {
        scheduledJobs.sendNewCustomerIncomeNotifications(db, clock)
        asyncJobRunner.runPendingJobsSync(clock)
        val emails = MockEmailClient.emails
        return emails
    }

    private fun getIncomeNotifications(receiverId: PersonId): List<IncomeNotification> =
        db.read { it.getIncomeNotifications(receiverId) }
}
