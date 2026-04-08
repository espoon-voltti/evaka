// SPDX-FileCopyrightText: 2017-2023 City of Espoo
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
import evaka.core.invoicing.data.findFeeDecisionsForHeadOfFamily
import evaka.core.invoicing.data.getIncomesForPerson
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeThresholds
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.serviceneed.ShiftCareType
import evaka.core.serviceneed.insertServiceNeed
import evaka.core.shared.ChildId
import evaka.core.shared.IncomeStatementId
import evaka.core.shared.PartnershipId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevFridgePartner
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevIncome
import evaka.core.shared.dev.DevIncomeStatement
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.job.ScheduledJobs
import evaka.core.snDaycareContractDays15
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
            restrictedDetailsEnabled = false,
        )

    private val fridgeHeadOfChild = DevPerson(email = "fridge_hoc@example.com")
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(fridgeHeadOfChild, DevPersonType.ADULT)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(testChild, DevPersonType.CHILD)
            val placementStart = clock.today().minusMonths(2)
            val placementEnd = clock.today().plusMonths(2)

            tx.insert(
                DevFridgeChild(
                    childId = testChild.id,
                    headOfChild = fridgeHeadOfChild.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild.id,
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
    fun `only last income is considered when finding outdated incomes`() {
        db.transaction {
            // This income is expiring, but not notified because there is another income starting
            // afterwards
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today(),
                )
            )

            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().plusDays(1),
                    validTo = clock.today().plusMonths(6),
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
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4),
                )
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
        assertEquals(
            IncomeNotificationType.INITIAL_EMAIL,
            getIncomeNotifications(fridgeHeadOfChild.id)[0].notificationType,
        )
    }

    @Test
    fun `notification is not sent if placement unit is not invoiced by municipality`() {
        db.transaction {
            it.execute {
                sql(
                    """
                    UPDATE daycare
                    SET provider_type = 'MUNICIPAL', invoiced_by_municipality = false
                    WHERE id = ${bind(daycare.id)}
                    """
                )
            }

            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4),
                )
            )
        }

        assertEquals(0, getEmails().size)
        assertEquals(0, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `notification is sent to customers of private service voucher unit`() {
        db.transaction {
            it.execute {
                sql(
                    """
                    UPDATE daycare
                    SET provider_type = 'PRIVATE_SERVICE_VOUCHER', invoiced_by_municipality = false
                    WHERE id = ${bind(daycare.id)}
                    """
                )
            }

            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4),
                )
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
    }

    @Test
    fun `fridge partner expiring income is notified 4 weeks beforehand`() {
        val fridgePartner = DevPerson(email = "partner@example.com")

        db.transaction {
            it.insert(fridgePartner, DevPersonType.ADULT)

            it.insert(
                DevIncome(
                    personId = fridgePartner.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4),
                )
            )

            val partnershipId = PartnershipId(UUID.randomUUID())
            it.insert(
                DevFridgePartner(
                    partnershipId,
                    1,
                    2,
                    fridgeHeadOfChild.id,
                    clock.today(),
                    clock.today(),
                    clock.now(),
                )
            )
            it.insert(
                DevFridgePartner(
                    partnershipId,
                    2,
                    1,
                    fridgePartner.id,
                    clock.today(),
                    clock.today(),
                    clock.now(),
                )
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(0, getIncomeNotifications(fridgeHeadOfChild.id).size)
        assertEquals(1, getIncomeNotifications(fridgePartner.id).size)
        assertEquals(
            IncomeNotificationType.INITIAL_EMAIL,
            getIncomeNotifications(fridgePartner.id)[0].notificationType,
        )
    }

    @Test
    fun `fridge partner with partnership end date as null and expiring income is notified 4 weeks beforehand`() {
        val fridgePartner = DevPerson(email = "partner@example.com")
        db.transaction {
            it.insert(fridgePartner, DevPersonType.ADULT)

            it.insert(
                DevIncome(
                    personId = fridgePartner.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4),
                )
            )

            val partnershipId = PartnershipId(UUID.randomUUID())
            it.insert(
                DevFridgePartner(
                    partnershipId,
                    1,
                    2,
                    fridgeHeadOfChild.id,
                    clock.today(),
                    clock.today(),
                    clock.now(),
                )
            )
            it.insert(
                DevFridgePartner(
                    partnershipId,
                    2,
                    1,
                    fridgePartner.id,
                    clock.today(),
                    null,
                    clock.now(),
                )
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(0, getIncomeNotifications(fridgeHeadOfChild.id).size)
        assertEquals(1, getIncomeNotifications(fridgePartner.id).size)
        assertEquals(
            IncomeNotificationType.INITIAL_EMAIL,
            getIncomeNotifications(fridgePartner.id)[0].notificationType,
        )
    }

    @Test
    fun `if expiration date is before yesterday no notification is sent`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().minusDays(2),
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
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusMonths(1),
                    validTo = incomeExpirationDate,
                )
            )

            it.insert(
                DevIncomeStatement(
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(incomeExpirationDate.plusDays(1)),
                    status = IncomeStatementStatus.SENT,
                    handlerId = null,
                )
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is not notified if there is a new income statement being handled reaching after expiration`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)

        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusMonths(1),
                    validTo = incomeExpirationDate,
                )
            )

            it.insert(
                DevIncomeStatement(
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(incomeExpirationDate.plusDays(1)),
                    status = IncomeStatementStatus.HANDLING,
                    handlerId = null,
                )
            )
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `expiring income is notified if there is a handled income statement reaching after expiration`() {
        val incomeExpirationDate = clock.today().plusWeeks(4)
        val employee = DevEmployee()

        db.transaction {
            it.insert(employee)

            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusMonths(1),
                    validTo = incomeExpirationDate,
                )
            )

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(incomeExpirationDate.plusDays(1)),
                    status = IncomeStatementStatus.HANDLED,
                    handlerId = employee.id,
                    handledAt = clock.now(),
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
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeExpirationDate.minusMonths(1),
                    validTo = incomeExpirationDate,
                )
            )

            it.insert(
                DevIncomeStatement(
                    id = IncomeStatementId(UUID.randomUUID()),
                    personId = fridgeHeadOfChild.id,
                    data = createGrossIncome(incomeExpirationDate.plusDays(1)),
                    status = IncomeStatementStatus.SENT,
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
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = expirationDate,
                )
            )

            // The placement ends on the same day as the income, and there is no billable placement
            // afterwards,
            // so no notification should be sent
            it.execute {
                sql(
                    "UPDATE placement SET end_date = ${bind(expirationDate)} WHERE child_id = ${bind(testChild.id)}"
                )
            }
        }

        assertEquals(0, getEmails().size)

        // There is a billable placement a day after the income has expired, so a notification
        // should be sent
        db.transaction {
            it.execute {
                sql(
                    "UPDATE placement SET end_date = ${bind(expirationDate)} + INTERVAL '1 day' WHERE child_id = ${bind(testChild.id)}"
                )
            }
        }

        assertEquals(1, getEmails().size)
    }

    @Test
    fun `If there is no invoicable service need option for the placement no notification is sent`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusDays(13),
                )
            )

            it.execute {
                sql(
                    "UPDATE service_need_option SET fee_coefficient = 0 WHERE id = ${bind(snDaycareContractDays15.id)}"
                )
            }
        }

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `if first notification was already sent and it is not yet time for the second notification a new first notification is not sent`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4),
                )
            )
        }

        val mails = getEmails()
        assertEquals(1, mails.size)
        assertTrue(
            mails[0]
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
                        incomeTypesProvider,
                        coefficientMultiplierProvider,
                        fridgeHeadOfChild.id,
                    )
                }
                .size,
        )

        assertEquals(0, getEmails().size)
    }

    @Test
    fun `second notification is only sent after first notification`() {
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusDays(6),
                )
            )
        }

        val mails = getEmails()
        assertEquals(1, mails.size)
        assertTrue(
            mails[0]
                .content
                .text
                .contains(
                    "Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain"
                )
        )

        val secondMails = getEmails()
        assertEquals(1, secondMails.size)
        assertTrue(
            secondMails[0].content.text.contains("Ette ole vielä toimittaneet uusia tulotietoja")
        )

        // A check that no new income has yet been generated (it is generated only after the third
        // email)
        assertEquals(
            1,
            db.read {
                    it.getIncomesForPerson(
                        incomeTypesProvider,
                        coefficientMultiplierProvider,
                        fridgeHeadOfChild.id,
                    )
                }
                .size,
        )
    }

    @Test
    fun `Final notification is sent after income expires`() {
        val expirationDay = clock.today().minusDays(1)
        db.transaction {
            it.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = expirationDay.minusMonths(1),
                    validTo = expirationDay,
                    effect = IncomeEffect.INCOME,
                )
            )

            it.createIncomeNotification(
                receiverId = fridgeHeadOfChild.id,
                IncomeNotificationType.INITIAL_EMAIL,
            )
            it.createIncomeNotification(
                receiverId = fridgeHeadOfChild.id,
                IncomeNotificationType.REMINDER_EMAIL,
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
                    temporaryFeeSiblingPartDay = 800,
                )
            )
        }

        val mails = getEmails()
        assertEquals(1, mails.size)
        assertTrue(
            mails[0]
                .content
                .text
                .contains("Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan")
        )

        assertEquals(0, getEmails().size)

        val incomes =
            db.read {
                it.getIncomesForPerson(
                    incomeTypesProvider,
                    coefficientMultiplierProvider,
                    fridgeHeadOfChild.id,
                )
            }
        assertEquals(2, incomes.size)
        assertEquals(IncomeEffect.INCOMPLETE, incomes[0].effect)
        val firstDayAfterExpiration = expirationDay.plusDays(1)
        assertEquals(firstDayAfterExpiration, incomes[0].validFrom)

        val feeFecisions =
            db.read { it.findFeeDecisionsForHeadOfFamily(fridgeHeadOfChild.id, null, null) }
        assertEquals(
            1,
            feeFecisions
                .filter {
                    it.status == FeeDecisionStatus.DRAFT &&
                        it.validFrom == firstDayAfterExpiration &&
                        it.headOfFamilyIncome!!.effect == IncomeEffect.INCOMPLETE
                }
                .size,
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
                    restrictedDetailsEnabled = false,
                )

            tx.insert(testChild2, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = fridgeHeadOfChild.id, childId = testChild2.id))
            val placementStart = clock.today().minusMonths(2)
            val placementEnd = clock.today().plusMonths(2)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild2.id,
                        unitId = daycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
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

            tx.insert(
                DevIncome(
                    personId = fridgeHeadOfChild.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = clock.today().minusMonths(1),
                    validTo = clock.today().plusWeeks(4),
                )
            )
        }

        assertEquals(1, getEmails().size)
        assertEquals(1, getIncomeNotifications(fridgeHeadOfChild.id).size)
        assertEquals(
            IncomeNotificationType.INITIAL_EMAIL,
            getIncomeNotifications(fridgeHeadOfChild.id)[0].notificationType,
        )
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
