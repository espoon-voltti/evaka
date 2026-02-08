// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.caseprocess.CaseProcessMetadataService
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.controller.PaymentController
import fi.espoo.evaka.invoicing.controller.PaymentDistinctiveParams
import fi.espoo.evaka.invoicing.controller.PaymentSortParam
import fi.espoo.evaka.invoicing.controller.SearchPaymentsRequest
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.controller.sendVoucherValueDecisions
import fi.espoo.evaka.invoicing.data.PagedPayments
import fi.espoo.evaka.invoicing.data.readPayments
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.PaymentStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reports.freezeVoucherValueReportRows
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.toValueDecisionServiceNeed
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PaymentsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var paymentController: PaymentController

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id, name = "Daycare")
    private val voucherDaycare1 =
        DevDaycare(
            areaId = area.id,
            name = "Voucher Daycare 1",
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
            invoicedByMunicipality = false,
            businessId = "1234567-8",
            iban = "FI12 3456 7891 2345 67",
            providerId = "1234",
            partnerCode = "abcdefg",
        )
    private val voucherDaycare2 =
        DevDaycare(
            areaId = area.id,
            name = "Voucher Daycare 2",
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
            invoicedByMunicipality = false,
            businessId = "8765432-1-8",
            iban = "FI98 7654 3210 9876 54",
            providerId = "4321",
            partnerCode = "gfedcba",
        )
    private val employee = DevEmployee()
    private val adult1 =
        DevPerson(
            ssn = "010180-1232",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val adult2 =
        DevPerson(
            ssn = "010279-123L",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val adult3 = DevPerson()
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child3 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))

    private val janFirst = LocalDate.of(2020, 1, 1)
    private val janLast = LocalDate.of(2020, 1, 31)
    private val janFreeze = HelsinkiDateTime.of(LocalDate.of(2020, 1, 25), LocalTime.of(2, 0))
    private val febFirst = LocalDate.of(2020, 2, 1)
    private val febSecond = LocalDate.of(2020, 2, 2)
    private val febFreeze = HelsinkiDateTime.of(LocalDate.of(2020, 2, 25), LocalTime.of(2, 0))
    private val febLast = LocalDate.of(2020, 2, 28)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(voucherDaycare1)
            tx.insert(voucherDaycare2)
            listOf(adult1, adult2, adult3).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2, child3).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `search payment drafts`() {
        val (_, _, draftId1, draftId2, draftId3) = createSearchTestData()

        val result = searchPayments(defaultSearchParams)
        assertEquals(3, result.total)
        assertEquals(listOf(draftId1, draftId2, draftId3), result.data.map { it.id })
    }

    @Test
    fun `search sent payments`() {
        val (sentId1, sentId2, _, _) = createSearchTestData()

        val result = searchPayments(defaultSearchParams.copy(status = PaymentStatus.SENT))
        assertEquals(2, result.total)
        assertEquals(listOf(sentId1, sentId2), result.data.map { it.id })
    }

    @Test
    fun `search payments with missing payment details`() {
        val (_, _, _, _, draftId3) = createSearchTestData()

        val result =
            searchPayments(
                defaultSearchParams.copy(
                    distinctions = listOf(PaymentDistinctiveParams.MISSING_PAYMENT_DETAILS)
                )
            )
        assertEquals(1, result.total)
        assertEquals(listOf(draftId3), result.data.map { it.id })
    }

    @Test
    fun `search payments by number`() {
        createSearchTestData()

        val result =
            searchPayments(
                defaultSearchParams.copy(status = PaymentStatus.SENT, searchTerms = "9000000000")
            )
        assertEquals(1, result.total)
        assertNotNull(result.data.find { it.number == 9000000000 })
    }

    @Test
    fun `search payments by unit`() {
        val (sentId1, _, _, _) = createSearchTestData()

        val result =
            searchPayments(
                defaultSearchParams.copy(unit = voucherDaycare1.id, status = PaymentStatus.SENT)
            )
        assertEquals(1, result.total)
        assertEquals(listOf(sentId1), result.data.map { it.id })
    }

    @Test
    fun `creating drafts fails if there are no freezed voucher reports`() {
        assertThrows<BadRequest> { createPaymentDrafts(janLast) }
    }

    @Test
    fun `create payment drafts`() {
        createVoucherDecision(
            janFirst,
            unitId = voucherDaycare1.id,
            headOfFamilyId = adult1.id,
            childId = child1.id,
            value = 87000,
            coPayment = 28800,
        )
        createVoucherDecision(
            janFirst,
            unitId = voucherDaycare2.id,
            headOfFamilyId = adult2.id,
            childId = child2.id,
            value = 134850,
            coPayment = 28800,
        )
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }

        createPaymentDrafts(janLast)

        val payments = db.read { it.readPayments() }
        assertEquals(2, payments.size)
        payments.first().let { payment ->
            assertEquals(PaymentStatus.DRAFT, payment.status)
            assertEquals(DateRange(janFirst, janLast), payment.period)
            assertEquals(voucherDaycare1.id, payment.unit.id)
            assertEquals(87000 - 28800, payment.amount)
        }
        payments.last().let { payment ->
            assertEquals(PaymentStatus.DRAFT, payment.status)
            assertEquals(DateRange(janFirst, janLast), payment.period)
            assertEquals(voucherDaycare2.id, payment.unit.id)
            assertEquals(134850 - 28800, payment.amount)
        }
    }

    @Test
    fun `delete payment draft`() {
        createVoucherDecision(
            janFirst,
            unitId = voucherDaycare1.id,
            headOfFamilyId = adult1.id,
            childId = child1.id,
            value = 87000,
            coPayment = 28800,
        )
        createVoucherDecision(
            janFirst,
            unitId = voucherDaycare2.id,
            headOfFamilyId = adult2.id,
            childId = child2.id,
            value = 134850,
            coPayment = 28800,
        )
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }

        val draftIds = createPaymentDrafts(janLast)
        deletePaymentDrafts(today = febFirst, listOf(draftIds[0]))

        val payments = db.read { it.readPayments() }
        assertEquals(draftIds.size - 1, payments.size)
        payments.forEach { assert(it.id != draftIds[0]) }
    }

    @Test
    fun `confirm payments`() {
        createVoucherDecision(
            janFirst,
            // Doesn't have payment details
            unitId = daycare.id,
            headOfFamilyId = adult2.id,
            childId = child2.id,
            value = 134850,
            coPayment = 28800,
        )
        createVoucherDecision(
            janFirst,
            // Has payment details
            unitId = voucherDaycare1.id,
            headOfFamilyId = adult1.id,
            childId = child1.id,
            value = 87000,
            coPayment = 28800,
        )
        createVoucherDecision(
            janFirst,
            // Has payment details
            unitId = voucherDaycare2.id,
            headOfFamilyId = adult3.id,
            childId = child3.id,
            value = 35000,
            coPayment = 28800,
        )
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        val paymentDraftIds = createPaymentDrafts(janLast)

        val confirmedPayments1 = paymentDraftIds.subList(0, 2)
        confirmPaymentDrafts(janLast, confirmedPayments1)

        val payments = db.read { it.readPayments() }
        assertEquals(3, payments.size)

        payments.first().let { payment ->
            assertEquals(DateRange(janFirst, janLast), payment.period)
            assertEquals(PaymentStatus.DRAFT, payment.status)
            assertEquals(daycare.id, payment.unit.id)
            assertEquals(daycare.name, payment.unit.name)
            assertEquals(daycare.costCenter, payment.unit.costCenter)
            assertEquals(134850 - 28800, payment.amount)
        }
        payments[1].let { payment ->
            assertEquals(PaymentStatus.CONFIRMED, payment.status)
            assertEquals(voucherDaycare1.id, payment.unit.id)
            assertEquals(87000 - 28800, payment.amount)
            assertEquals(voucherDaycare1.name, payment.unit.name)
            assertEquals(voucherDaycare1.costCenter, payment.unit.costCenter)
        }
        payments.last().let { payment ->
            assertEquals(PaymentStatus.CONFIRMED, payment.status)
            assertEquals(voucherDaycare2.id, payment.unit.id)
            assertEquals(35000 - 28800, payment.amount)
            assertEquals(voucherDaycare2.name, payment.unit.name)
            assertEquals(voucherDaycare2.costCenter, payment.unit.costCenter)
        }

        // assert that sending details remain unset
        payments.forEach { payment ->
            assertEquals(null, payment.paymentDate)
            assertEquals(null, payment.dueDate)
            assertEquals(null, payment.number)
            assertEquals(null, payment.sentBy)
            assertEquals(null, payment.sentAt)
            assertEquals(null, payment.unit.businessId)
            assertEquals(null, payment.unit.iban)
            assertEquals(null, payment.unit.providerId)
            assertEquals(null, payment.unit.partnerCode)
        }

        val confirmedPayments2 = createPaymentDrafts(janLast)
        assertEquals(1, confirmedPayments2.size)

        val paymentIds = db.read { it.readPayments() }.sortedBy { it.amount }.map { it.id }
        assertEquals(confirmedPayments1 + confirmedPayments2, paymentIds)

        confirmPaymentDrafts(janLast, confirmedPayments2)

        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }
        val draftPaymentsFeb = createPaymentDrafts(febLast)
        assertEquals(3, draftPaymentsFeb.size)
    }

    @Test
    fun `revert confirmed payments to drafts`() {
        createVoucherDecision(
            janFirst,
            // Doesn't have payment details
            unitId = daycare.id,
            headOfFamilyId = adult2.id,
            childId = child2.id,
            value = 134850,
            coPayment = 28800,
        )
        createVoucherDecision(
            janFirst,
            // Has payment details
            unitId = voucherDaycare1.id,
            headOfFamilyId = adult1.id,
            childId = child1.id,
            value = 87000,
            coPayment = 28800,
        )
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        val paymentDraftIds = createPaymentDrafts(janLast)

        confirmPaymentDrafts(janLast, paymentDraftIds)

        revertPaymentsToDrafts(janLast, paymentDraftIds)

        val payments = db.read { it.readPayments() }
        assertEquals(2, payments.size)

        payments.first().let { payment ->
            assertEquals(DateRange(janFirst, janLast), payment.period)
            assertEquals(daycare.id, payment.unit.id)
            assertEquals(daycare.name, payment.unit.name)
            assertEquals(daycare.costCenter, payment.unit.costCenter)
            assertEquals(134850 - 28800, payment.amount)
        }
        payments.last().let { payment ->
            assertEquals(voucherDaycare1.id, payment.unit.id)
            assertEquals(87000 - 28800, payment.amount)
            assertEquals(voucherDaycare1.name, payment.unit.name)
            assertEquals(voucherDaycare1.costCenter, payment.unit.costCenter)
        }

        // assert that status is set and sending details remain unset
        payments.forEach { payment ->
            assertEquals(PaymentStatus.DRAFT, payment.status)
            assertEquals(null, payment.paymentDate)
            assertEquals(null, payment.dueDate)
            assertEquals(null, payment.number)
            assertEquals(null, payment.sentBy)
            assertEquals(null, payment.sentAt)
            assertEquals(null, payment.unit.businessId)
            assertEquals(null, payment.unit.iban)
            assertEquals(null, payment.unit.providerId)
            assertEquals(null, payment.unit.partnerCode)
        }
    }

    @Test
    fun `send payments`() {
        createVoucherDecision(
            janFirst,
            // Doesn't have payment details
            unitId = daycare.id,
            headOfFamilyId = adult2.id,
            childId = child2.id,
            value = 134850,
            coPayment = 28800,
        )
        createVoucherDecision(
            janFirst,
            // Has payment details
            unitId = voucherDaycare1.id,
            headOfFamilyId = adult1.id,
            childId = child1.id,
            value = 87000,
            coPayment = 28800,
        )
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        val paymentDraftIds = createPaymentDrafts(janLast)

        confirmPaymentDrafts(janLast, paymentDraftIds)
        sendPayments(
            today = febFirst,
            paymentDate = febSecond,
            dueDate = febLast,
            paymentIds = paymentDraftIds,
        )

        val payments = db.read { it.readPayments() }
        assertEquals(2, payments.size)
        payments.first().let { payment ->
            assertEquals(PaymentStatus.CONFIRMED, payment.status)
            assertEquals(DateRange(janFirst, janLast), payment.period)
            assertEquals(daycare.id, payment.unit.id)
            assertEquals(daycare.costCenter, payment.unit.costCenter)
            assertEquals(134850 - 28800, payment.amount)
        }
        payments.last().let { payment ->
            assertEquals(PaymentStatus.SENT, payment.status)
            assertEquals(voucherDaycare1.id, payment.unit.id)
            assertEquals(87000 - 28800, payment.amount)
            assertEquals(febSecond, payment.paymentDate)
            assertEquals(febLast, payment.dueDate)
            assertEquals(9000000000, payment.number)
            assertEquals(employee.evakaUserId, payment.sentBy)
            assertEquals(HelsinkiDateTime.of(febFirst, LocalTime.of(10, 0)), payment.sentAt)
            assertEquals(voucherDaycare1.name, payment.unit.name)
            assertEquals(voucherDaycare1.businessId, payment.unit.businessId)
            assertEquals(voucherDaycare1.iban, payment.unit.iban)
            assertEquals(voucherDaycare1.providerId, payment.unit.providerId)
            assertEquals(voucherDaycare1.partnerCode, payment.unit.partnerCode)
            assertEquals(voucherDaycare1.costCenter, payment.unit.costCenter)
        }
    }

    private val defaultSearchParams =
        SearchPaymentsRequest(
            searchTerms = "",
            area = listOf(),
            unit = null,
            distinctions = listOf(),
            status = PaymentStatus.DRAFT,
            paymentDateStart = null,
            paymentDateEnd = null,
            page = 1,
            sortBy = PaymentSortParam.AMOUNT,
            sortDirection = SortDirection.ASC,
        )

    private fun createSearchTestData(): List<PaymentId> {
        createVoucherDecision(
            janFirst,
            unitId = voucherDaycare1.id,
            headOfFamilyId = adult1.id,
            childId = child1.id,
            value = 87000,
            coPayment = 28800,
        )
        createVoucherDecision(
            janFirst,
            unitId = voucherDaycare2.id,
            headOfFamilyId = adult2.id,
            childId = child2.id,
            value = 87000,
            coPayment = 7000,
        )
        createVoucherDecision(
            febFirst,
            // Doesn't have payment details
            unitId = daycare.id,
            headOfFamilyId = adult3.id,
            childId = child3.id,
            value = 134850,
            coPayment = 0,
        )
        db.transaction {
            freezeVoucherValueReportRows(it, janFirst.year, janFirst.monthValue, janFreeze)
        }
        val paymentDraftIds1 = createPaymentDrafts(janLast)

        confirmPaymentDrafts(janLast, paymentDraftIds1)

        sendPayments(
            today = febFirst,
            paymentDate = febSecond,
            dueDate = febLast,
            paymentIds = paymentDraftIds1,
        )

        db.transaction {
            freezeVoucherValueReportRows(it, febFirst.year, febFirst.monthValue, febFreeze)
        }
        val paymentDraftIds2 = createPaymentDrafts(febLast)

        return paymentDraftIds1 + paymentDraftIds2
    }

    private val financeUser =
        AuthenticatedUser.Employee(id = employee.id, roles = setOf(UserRole.FINANCE_ADMIN))

    private fun createVoucherDecision(
        validFrom: LocalDate,
        unitId: DaycareId,
        value: Int,
        coPayment: Int,
        headOfFamilyId: PersonId,
        childId: ChildId,
        approvedAt: HelsinkiDateTime = HelsinkiDateTime.of(validFrom, LocalTime.of(15, 0)),
        alwaysUseDaycareFinanceDecisionHandler: Boolean = false,
        feeAlterations: List<FeeAlterationWithEffect> = listOf(),
    ) {
        db.transaction {
            val decision =
                createVoucherValueDecisionFixture(
                    status = VoucherValueDecisionStatus.DRAFT,
                    validFrom = validFrom,
                    validTo = validFrom.plusYears(1),
                    headOfFamilyId = headOfFamilyId,
                    childId = childId,
                    dateOfBirth = child1.dateOfBirth,
                    unitId = unitId,
                    value = value,
                    coPayment = coPayment,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed(),
                    feeAlterations = feeAlterations,
                )
            it.upsertValueDecisions(listOf(decision))

            sendVoucherValueDecisions(
                tx = it,
                asyncJobRunner = asyncJobRunner,
                user = financeUser,
                evakaEnv = evakaEnv,
                metadata = CaseProcessMetadataService(featureConfig),
                now = approvedAt,
                ids = listOf(decision.id),
                decisionHandlerId = null,
                alwaysUseDaycareFinanceDecisionHandler,
            )
            decision.id
        }
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
    }

    private fun searchPayments(params: SearchPaymentsRequest): PagedPayments {
        val (_, response, result) =
            http
                .post("/employee/payments/search")
                .asUser(financeUser)
                .jsonBody(jsonMapper.writeValueAsString(params))
                .responseObject<PagedPayments>(jackson2JsonMapper)
        assertEquals(200, response.statusCode)
        return result.get()
    }

    private fun createPaymentDrafts(today: LocalDate): List<PaymentId> {
        paymentController.createPaymentDrafts(
            dbInstance(),
            financeUser,
            MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(10, 0))),
        )

        return db.read { tx ->
            tx.createQuery { sql("SELECT id FROM payment WHERE status = 'DRAFT' ORDER BY amount") }
                .toList<PaymentId>()
        }
    }

    private fun confirmPaymentDrafts(today: LocalDate, ids: List<PaymentId>) {
        paymentController.confirmDraftPayments(
            dbInstance(),
            financeUser,
            MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(10, 0))),
            ids,
        )
    }

    private fun revertPaymentsToDrafts(today: LocalDate, ids: List<PaymentId>) {
        paymentController.revertPaymentsToDrafts(
            dbInstance(),
            financeUser,
            MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(10, 0))),
            ids,
        )
    }

    private fun deletePaymentDrafts(today: LocalDate, paymentIds: List<PaymentId>) {
        paymentController.deleteDraftPayments(
            dbInstance(),
            financeUser,
            MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(10, 0))),
            paymentIds,
        )
    }

    private fun sendPayments(
        today: LocalDate,
        paymentDate: LocalDate,
        dueDate: LocalDate,
        paymentIds: List<PaymentId>,
    ) {
        paymentController.sendPayments(
            dbInstance(),
            financeUser,
            MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(10, 0))),
            PaymentController.SendPaymentsRequest(paymentDate, dueDate, paymentIds),
        )
    }
}
