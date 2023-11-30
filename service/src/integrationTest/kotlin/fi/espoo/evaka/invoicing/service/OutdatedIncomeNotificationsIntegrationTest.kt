// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.incomestatement.IncomeStatementType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.IncomeStatementId
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
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevIncomeStatement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import fi.espoo.evaka.snDaycareContractDays15
import java.math.BigDecimal
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
    @Autowired lateinit var mapper: JsonMapper
    @Autowired lateinit var incomeTypesProvider: IncomeTypesProvider
    @Autowired lateinit var coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider

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

    private val fridgeHeadOfChildEmail = "fridge_hoc@example.com"
    private lateinit var fridgeHeadOfChildId: PersonId
    private lateinit var childId: ChildId
    private lateinit var employeeId: EmployeeId
    private lateinit var employeeEvakaUserId: EvakaUserId
    private lateinit var daycareId: DaycareId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            fridgeHeadOfChildId =
                tx.insert(DevPerson(email = fridgeHeadOfChildEmail), DevPersonType.ADULT)
            val areaId = tx.insert(DevCareArea())
            daycareId = tx.insert(DevDaycare(areaId = areaId))
            childId = tx.insert(testChild, DevPersonType.CHILD)
            val placementStart = clock.today().minusMonths(2)
            val placementEnd = clock.today().plusMonths(2)

            tx.insert(
                DevFridgeChild(
                    childId = childId,
                    headOfChild = fridgeHeadOfChildId,
                    startDate = placementStart,
                    endDate = placementEnd
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
    fun `only last income is considered when finding outdated incomes`() {
        db.transaction {
            // This income is expiring, but not notified because there is another income starting
            // afterwards
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today()
                )
            )

            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().plusDays(1),
                    validTo = clock.today().plusMonths(6)
                )
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is notified 4 weeks beforehand`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4)
                )
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChildId).size)
        assertEquals(
            IncomeNotificationType.INITIAL_EMAIL,
            getIncomeNotifications(fridgeHeadOfChildId)[0].notificationType
        )
    }

    private lateinit var fridgePartnerId: PersonId

    @Test
    fun `fridge partner expiring income is notified 4 weeks beforehand`() {
        db.transaction {
            fridgePartnerId =
                it.insert(DevPerson(email = "partner@example.com"), DevPersonType.ADULT)

            it.insert(
                DevIncome(
                    personId = fridgePartnerId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4)
                )
            )

            val partnershipId = PartnershipId(UUID.randomUUID())
            it.insert(
                DevFridgePartner(
                    partnershipId,
                    1,
                    2,
                    fridgeHeadOfChildId,
                    clock.today(),
                    clock.today()
                )
            )
            it.insert(
                DevFridgePartner(partnershipId, 2, 1, fridgePartnerId, clock.today(), clock.today())
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(0, getIncomeNotifications(fridgeHeadOfChildId).size)
        assertEquals(1, getIncomeNotifications(fridgePartnerId).size)
        assertEquals(
            IncomeNotificationType.INITIAL_EMAIL,
            getIncomeNotifications(fridgePartnerId)[0].notificationType
        )
    }

    @Test
    fun `if expiration date is in the past no notification is sent`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().minusDays(1)
                )
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is not notified if there is a new unhandled income statement reaching after expiration`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)

        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = incomeExpirationDate.minusMonths(1),
                    validTo = incomeExpirationDate
                )
            )

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChildId,
                    startDate = incomeExpirationDate.plusDays(1),
                    type = IncomeStatementType.INCOME,
                    grossEstimatedMonthlyIncome = 42,
                    handlerId = null
                )
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is notified if there is a handled income statement reaching after expiration`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)

        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = incomeExpirationDate.minusMonths(1),
                    validTo = incomeExpirationDate
                )
            )

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChildId,
                    startDate = incomeExpirationDate.plusDays(1),
                    type = IncomeStatementType.INCOME,
                    grossEstimatedMonthlyIncome = 42,
                    handlerId = employeeId
                )
            )
        }

        assertEquals(1, getEmails().size)
    }

    @Test
    fun `expiring income is notified if there is an unhandled income statement reaching after expiration`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)

        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = incomeExpirationDate.minusMonths(1),
                    validTo = incomeExpirationDate
                )
            )

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChildId,
                    startDate = incomeExpirationDate.plusDays(1),
                    type = IncomeStatementType.INCOME,
                    grossEstimatedMonthlyIncome = 42
                )
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `If there is no placement a day after expiration no notification is sent`() {
        val expirationDate = clock.today().plusWeeks(2)
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = expirationDate
                )
            )

            // The placement ends on the same day as the income, and there is no billable placement
            // afterwards,
            // so no notification should be sent
            it.createUpdate(
                    "UPDATE placement SET end_date = :expirationDate WHERE child_id = :personId"
                )
                .bind("expirationDate", expirationDate)
                .bind("personId", childId)
                .execute()
        }

        assertEquals(0, getEmails().size)

        // There is a billable placement a day after the income has expired, so a notification
        // should be sent
        db.transaction {
            it.createUpdate(
                    "UPDATE placement SET end_date = :expirationDate + INTERVAL '1 day' WHERE child_id = :personId"
                )
                .bind("expirationDate", expirationDate)
                .bind("personId", childId)
                .execute()
        }

        assertEquals(1, getEmails().size)
    }

    @Test
    fun `If there is no invoicable service need option for the placement no notification is sent`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusDays(13)
                )
            )

            it.createUpdate("UPDATE service_need_option SET fee_coefficient = 0 WHERE id = :snoId")
                .bind("snoId", snDaycareContractDays15.id)
                .execute()
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `if first notification was already sent and it is not yet time for the second notification a new first notification is not sent`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4)
                )
            )
        }

        val mails = getEmails()
        assertEquals(1, mails.size)
        assertTrue(
            mails
                .get(0)
                .content
                .text
                .contains(
                    "Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain"
                )
        )
        // A check that no new income has yet been generated (it is generated only after the third
        // email)
        assertEquals(
            1,
            db.read {
                    it.getIncomesForPerson(
                        mapper,
                        incomeTypesProvider,
                        coefficientMultiplierProvider,
                        fridgeHeadOfChildId
                    )
                }
                .size
        )

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `second notification is only sent after first notification`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
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
                .content
                .text
                .contains(
                    "Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain"
                )
        )

        val secondMails = getEmails()
        assertEquals(1, secondMails.size)
        assertTrue(
            secondMails
                .get(0)
                .content
                .text
                .contains("Ette ole vielä toimittaneet uusia tulotietoja")
        )

        // A check that no new income has yet been generated (it is generated only after the third
        // email)
        assertEquals(
            1,
            db.read {
                    it.getIncomesForPerson(
                        mapper,
                        incomeTypesProvider,
                        coefficientMultiplierProvider,
                        fridgeHeadOfChildId
                    )
                }
                .size
        )
    }

    @Test
    fun `Final notification is sent after income expires`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today(),
                    effect = IncomeEffect.INCOME
                )
            )

            it.createIncomeNotification(
                receiverId = fridgeHeadOfChildId,
                IncomeNotificationType.INITIAL_EMAIL
            )
            it.createIncomeNotification(
                receiverId = fridgeHeadOfChildId,
                IncomeNotificationType.REMINDER_EMAIL
            )

            it.insert(
                FeeThresholds(
                    validDuring = DateRange(LocalDate.of(2000, 1, 1), null),
                    minIncomeThreshold2 = 210200,
                    minIncomeThreshold3 = 271300,
                    minIncomeThreshold4 = 308000,
                    minIncomeThreshold5 = 344700,
                    minIncomeThreshold6 = 381300,
                    maxIncomeThreshold2 = 479900,
                    maxIncomeThreshold3 = 541000,
                    maxIncomeThreshold4 = 577700,
                    maxIncomeThreshold5 = 614400,
                    maxIncomeThreshold6 = 651000,
                    incomeMultiplier2 = BigDecimal("0.1070"),
                    incomeMultiplier3 = BigDecimal("0.1070"),
                    incomeMultiplier4 = BigDecimal("0.1070"),
                    incomeMultiplier5 = BigDecimal("0.1070"),
                    incomeMultiplier6 = BigDecimal("0.1070"),
                    incomeThresholdIncrease6Plus = 14200,
                    siblingDiscount2 = BigDecimal("0.5"),
                    siblingDiscount2Plus = BigDecimal("0.8"),
                    maxFee = 28900,
                    minFee = 2700,
                    temporaryFee = 2900,
                    temporaryFeePartDay = 1500,
                    temporaryFeeSibling = 1500,
                    temporaryFeeSiblingPartDay = 800
                )
            )
        }

        val mails = getEmails()
        assertEquals(1, mails.size)
        assertTrue(
            mails
                .get(0)
                .content
                .text
                .contains("Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan")
        )

        assertEquals(0, getEmails().size)

        val incomes =
            db.read {
                it.getIncomesForPerson(
                    mapper,
                    incomeTypesProvider,
                    coefficientMultiplierProvider,
                    fridgeHeadOfChildId
                )
            }
        assertEquals(2, incomes.size)
        assertEquals(IncomeEffect.INCOMPLETE, incomes[0].effect)
        val firstDayAfterExpiration = clock.today().plusDays(1)
        assertEquals(firstDayAfterExpiration, incomes[0].validFrom)

        val feeFecisions =
            db.read { it.findFeeDecisionsForHeadOfFamily(fridgeHeadOfChildId, null, null) }
        assertEquals(
            1,
            feeFecisions
                .filter {
                    it.status == FeeDecisionStatus.DRAFT &&
                        it.validFrom == firstDayAfterExpiration &&
                        it.headOfFamilyIncome!!.effect == IncomeEffect.INCOMPLETE
                }
                .size
        )
    }

    @Test
    fun `If guardian has more than 1 child expiring income is notified 4 weeks beforehand only once`() {
        db.transaction { tx ->
            val testChild2 =
                DevPerson(
                    id = ChildId(UUID.randomUUID()),
                    dateOfBirth = LocalDate.of(2016, 3, 1),
                    ssn = "010316A1235",
                    firstName = "Rachel",
                    lastName = "Doe",
                    streetAddress = "Kamreerintie 2",
                    postalCode = "02770",
                    postOffice = "Espoo",
                    restrictedDetailsEnabled = false
                )

            tx.insert(testChild2, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = fridgeHeadOfChildId, childId = testChild2.id))
            val placementStart = clock.today().minusMonths(2)
            val placementEnd = clock.today().plusMonths(2)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild2.id,
                        unitId = daycareId,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
            tx.insertServiceNeed(
                placementId = placementId,
                startDate = placementStart,
                endDate = placementEnd,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )

            tx.insert(
                DevIncome(
                    personId = fridgeHeadOfChildId,
                    updatedBy = employeeEvakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4)
                )
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChildId).size)
        assertEquals(
            IncomeNotificationType.INITIAL_EMAIL,
            getIncomeNotifications(fridgeHeadOfChildId)[0].notificationType
        )
    }

    private fun getEmails(): List<Email> {
        scheduledJobs.sendOutdatedIncomeNotifications(db, clock)
        asyncJobRunner.runPendingJobsSync(clock)
        val emails = MockEmailClient.emails
        MockEmailClient.clear()
        return emails
    }

    private fun getIncomeNotifications(receiverId: PersonId): List<IncomeNotification> =
        db.read { it.getIncomeNotifications(receiverId) }
}
