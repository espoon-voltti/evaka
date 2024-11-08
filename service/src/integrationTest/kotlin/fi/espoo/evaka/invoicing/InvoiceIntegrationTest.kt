// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.invoicing.controller.InvoiceController
import fi.espoo.evaka.invoicing.controller.InvoiceSortParam
import fi.espoo.evaka.invoicing.controller.SearchInvoicesRequest
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.data.getInvoice
import fi.espoo.evaka.invoicing.data.getInvoicesByIds
import fi.espoo.evaka.invoicing.data.getMaxInvoiceNumber
import fi.espoo.evaka.invoicing.data.paginatedSearch
import fi.espoo.evaka.invoicing.data.searchInvoices
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.InvoiceSummary
import fi.espoo.evaka.invoicing.domain.RelatedFeeDecision
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevFeeDecision
import fi.espoo.evaka.shared.dev.DevInvoice
import fi.espoo.evaka.shared.dev.DevInvoiceRow
import fi.espoo.evaka.shared.dev.DevInvoicedFeeDecision
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toFeeDecisionServiceNeed
import fi.espoo.evaka.toPersonBasic
import fi.espoo.evaka.toPersonDetailed
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class InvoiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var invoiceController: InvoiceController

    private fun assertEqualEnough(expected: List<InvoiceSummary>, actual: List<InvoiceSummary>) {
        assertEquals(
            expected
                .map {
                    it.copy(
                        sentAt = null,
                        createdAt = null,
                        headOfFamily = it.headOfFamily.copy(email = null),
                    )
                }
                .toSet(),
            actual.map { it.copy(sentAt = null, createdAt = null) }.toSet(),
        )
    }

    private fun assertDetailedEqualEnough(
        expected: List<InvoiceDetailed>,
        actual: List<InvoiceDetailed>,
    ) {
        assertEquals(
            expected
                .map { it.copy(sentAt = null, headOfFamily = it.headOfFamily.copy(email = null)) }
                .toSet(),
            actual.map { it.copy(sentAt = null) }.toSet(),
        )
    }

    private val testInvoices =
        listOf(
            DevInvoice(
                status = InvoiceStatus.DRAFT,
                headOfFamilyId = testAdult_1.id,
                areaId = testArea.id,
                rows =
                    listOf(
                        DevInvoiceRow(
                            childId = testChild_1.id,
                            unitId = testDaycare.id,
                            amount = 1,
                            unitPrice = 28900,
                        ),
                        DevInvoiceRow(
                            childId = testChild_1.id,
                            unitId = testDaycare.id,
                            product = ProductKey("DAILY_REFUND"),
                            amount = 1,
                            unitPrice = -1000,
                        ),
                    ),
            ),
            DevInvoice(
                status = InvoiceStatus.SENT,
                headOfFamilyId = testAdult_1.id,
                areaId = testArea.id,
                number = 5000000001L,
                periodStart = LocalDate.of(2020, 1, 1),
                periodEnd = LocalDate.of(2020, 1, 31),
                rows = listOf(DevInvoiceRow(childId = testChild_1.id, unitId = testDaycare.id)),
            ),
            DevInvoice(
                status = InvoiceStatus.DRAFT,
                headOfFamilyId = testAdult_2.id,
                areaId = testArea.id,
                periodStart = LocalDate.of(2018, 1, 1),
                periodEnd = LocalDate.of(2018, 1, 31),
                rows = listOf(DevInvoiceRow(childId = testChild_2.id, unitId = testDaycare.id)),
            ),
        )

    private val decision1 =
        createFeeDecisionFixture(
            status = FeeDecisionStatus.SENT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_1.id,
            period = FiniteDateRange(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6)),
            children =
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_2.id,
                        dateOfBirth = testChild_2.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    )
                ),
        )
    private val decision2 =
        createFeeDecisionFixture(
            status = FeeDecisionStatus.SENT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_2.id,
            period = FiniteDateRange(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6)),
            children =
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    )
                ),
        )
    private val decisionNoSsn =
        createFeeDecisionFixture(
            status = FeeDecisionStatus.SENT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_3.id, // Does not have SSN
            period = FiniteDateRange(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6)),
            children =
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_3.id,
                        dateOfBirth = testChild_3.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    )
                ),
        )

    private val testUser =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))

    private val allAreas = listOf(testArea)
    private val allDaycares = listOf(testDaycare, testDaycare2)
    private val allAdults = listOf(testAdult_1, testAdult_2, testAdult_3)
    private val allChildren = listOf(testChild_1, testChild_2, testChild_3)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            allAreas.forEach { tx.insert(it) }
            allDaycares.forEach { tx.insert(it) }
            allAdults.forEach { tx.insert(it, DevPersonType.ADULT) }
            allChildren.forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(feeThresholds)
            tx.insert(snDaycareFullDay35)
        }
    }

    @Test
    fun `search works with draft status parameter`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val drafts =
            testInvoices.filter { it.status == InvoiceStatus.DRAFT }.sortedBy { it.dueDate }

        val result =
            searchInvoices(SearchInvoicesRequest(page = 1, status = listOf(InvoiceStatus.DRAFT)))
        assertEqualEnough(drafts.map(::toSummary), result)
    }

    @Test
    fun `search works with sent status parameter`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val sent = testInvoices.filter { it.status == InvoiceStatus.SENT }

        val result =
            searchInvoices(SearchInvoicesRequest(page = 1, status = listOf(InvoiceStatus.SENT)))
        assertEqualEnough(sent.map(::toSummary), result)
    }

    @Test
    fun `search works with multiple status parameters`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val sentAndDraft =
            testInvoices.filter {
                it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.DRAFT
            }

        val result =
            searchInvoices(
                SearchInvoicesRequest(
                    page = 1,
                    status = listOf(InvoiceStatus.SENT, InvoiceStatus.DRAFT),
                )
            )
        assertEqualEnough(sentAndDraft.map(::toSummary), result)
    }

    @Test
    fun `search works as expected with existing area param`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val invoices = testInvoices.sortedBy { it.status }.reversed()

        val result = searchInvoices(SearchInvoicesRequest(page = 1, area = listOf("test_area")))
        assertEqualEnough(invoices.map(::toSummary), result)
    }

    @Test
    fun `search works as expected with area and status params`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val invoices = testInvoices.sortedBy { it.status }.reversed()

        val result =
            searchInvoices(
                SearchInvoicesRequest(
                    page = 1,
                    area = listOf("test_area"),
                    status = listOf(InvoiceStatus.DRAFT),
                )
            )
        assertEqualEnough(
            invoices
                .filter { it.status == InvoiceStatus.DRAFT && it.areaId == testArea.id }
                .map(::toSummary),
            result,
        )
    }

    @Test
    fun `search works as expected with non-existent area param`() {
        db.transaction { tx -> tx.insert(testInvoices) }

        val result = searchInvoices(SearchInvoicesRequest(page = 1, area = listOf("non_existent")))
        assertEqualEnough(listOf(), result)
    }

    @Test
    fun `search works as expected with multiple partial search terms`() {
        db.transaction { tx -> tx.insert(testInvoices) }

        val result =
            searchInvoices(
                SearchInvoicesRequest(
                    page = 1,
                    searchTerms =
                        "${testAdult_1.streetAddress} ${testAdult_1.firstName.substring(0, 2)}",
                )
            )
        assertEqualEnough(testInvoices.map(::toSummary), result)
    }

    @Test
    fun `search works as expected with multiple more specific search terms`() {
        db.transaction { tx -> tx.insert(testInvoices) }

        val result =
            searchInvoices(
                SearchInvoicesRequest(
                    page = 1,
                    searchTerms = "${testAdult_1.lastName.substring(0, 2)} ${testAdult_1.firstName}",
                )
            )
        assertEqualEnough(testInvoices.take(2).map(::toSummary), result)
    }

    @Test
    fun `search works as expected with multiple search terms where one does not match anything`() {
        db.transaction { tx -> tx.insert(testInvoices) }

        val result =
            searchInvoices(
                SearchInvoicesRequest(
                    page = 1,
                    searchTerms = "${testAdult_1.lastName} ${testAdult_1.streetAddress} nomatch",
                )
            )
        assertEqualEnough(listOf(), result)
    }

    @Test
    fun `search works as expected with child name as search term`() {
        db.transaction { tx -> tx.insert(testInvoices) }

        val result =
            searchInvoices(SearchInvoicesRequest(page = 1, searchTerms = testChild_2.firstName))
        assertEqualEnough(testInvoices.takeLast(1).map(::toSummary), result)
    }

    @Test
    fun `search works as expected with ssn as search term`() {
        db.transaction { tx -> tx.insert(testInvoices) }

        val result = searchInvoices(SearchInvoicesRequest(page = 1, searchTerms = testAdult_1.ssn))
        assertEqualEnough(testInvoices.take(2).map(::toSummary), result)
    }

    @Test
    fun `search works as expected with date of birth as search term`() {
        db.transaction { tx -> tx.insert(testInvoices) }

        val result =
            searchInvoices(
                SearchInvoicesRequest(page = 1, searchTerms = testAdult_1.ssn!!.substring(0, 6))
            )
        assertEqualEnough(testInvoices.take(2).map(::toSummary), result)
    }

    @Test
    fun `search gives correct total and page composition when using filters`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val sent =
            testInvoices
                .filter { it.status == InvoiceStatus.DRAFT }
                .sortedBy { it.periodStart }
                .reversed()

        val result =
            invoiceController.searchInvoices(
                dbInstance(),
                testUser,
                RealEvakaClock(),
                SearchInvoicesRequest(
                    page = 1,
                    status = listOf(InvoiceStatus.DRAFT),
                    sortBy = InvoiceSortParam.START,
                    sortDirection = SortDirection.DESC,
                ),
            )

        assertEquals(2, result.total)
        assertEqualEnough(sent.map(::toSummary).take(2), result.data.map { it.data })
    }

    @Test
    fun `getInvoice works with existing invoice`() {
        val invoiceIds = db.transaction { tx -> tx.insert(testInvoices) }
        val invoice = testInvoices[0]

        val result = getInvoice(invoiceIds[0])
        assertDetailedEqualEnough(listOf(toDetailed(invoice)), listOf(result))
    }

    @Test
    fun `getInvoice works with existing invoice with related fee decisions`() {
        val invoice = testInvoices[0]
        val feeDecision =
            DevFeeDecision(
                validDuring = FiniteDateRange(invoice.periodStart, invoice.periodEnd),
                headOfFamilyId = invoice.headOfFamilyId,
                status = FeeDecisionStatus.SENT,
                decisionNumber = 123,
            )
        db.transaction { tx ->
            tx.insert(feeDecision)
            tx.insert(invoice)
            tx.insert(DevInvoicedFeeDecision(invoice.id, feeDecision.id))
        }

        val result = getInvoice(invoice.id)
        assertDetailedEqualEnough(
            expected =
                listOf(
                    toDetailed(invoice)
                        .copy(
                            relatedFeeDecisions =
                                listOf(
                                    RelatedFeeDecision(id = feeDecision.id, decisionNumber = 123)
                                )
                        )
                ),
            actual = listOf(result),
        )
    }

    @Test
    fun `getInvoice returns not found with non-existent invoice`() {
        db.transaction { tx -> tx.insert(testInvoices) }

        assertThrows<NotFound> {
            getInvoice(InvoiceId(UUID.fromString("00000000-0000-0000-0000-000000000000")))
        }
    }

    @Test
    fun `send works with draft invoice`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val draft = testInvoices.find { it.status == InvoiceStatus.DRAFT }!!

        sendInvoices(listOf(draft.id))
    }

    @Test
    fun `send returns bad request for sent status invoice`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val sent = testInvoices.find { it.status == InvoiceStatus.SENT }!!

        assertThrows<BadRequest> { sendInvoices(listOf(sent.id)) }
    }

    @Test
    fun `send updates invoice status and number and sent fields`() {
        db.transaction { tx -> tx.insert(testInvoices) }
        val draft = testInvoices.find { it.status == InvoiceStatus.DRAFT }!!

        sendInvoices(listOf(draft.id))

        val result = getInvoice(draft.id)
        val updated =
            draft.copy(
                status = InvoiceStatus.SENT,
                number = 5000000002L,
                sentBy = EvakaUserId(testDecisionMaker_1.id.raw),
            )

        assertDetailedEqualEnough(listOf(toDetailed(updated)), listOf(result))
        assertEquals(InvoiceStatus.SENT, result.status)
        assertEquals(testDecisionMaker_1.id.raw, result.sentBy?.raw)
        assertNotNull(result.sentAt)
    }

    @Test
    fun `send sets distinct numbers`() {
        val drafts =
            (1..5).map {
                DevInvoice(
                    status = InvoiceStatus.DRAFT,
                    headOfFamilyId = testAdult_1.id,
                    areaId = testArea.id,
                    rows = listOf(DevInvoiceRow(childId = testChild_1.id, unitId = testDaycare.id)),
                )
            }

        db.transaction { tx -> tx.insert(drafts) }

        sendInvoices(drafts.map { it.id })

        val sentInvoices = db.transaction { tx -> tx.getInvoicesByIds(drafts.map { it.id }) }

        assertThat(sentInvoices.all { it.status == InvoiceStatus.SENT }).isTrue

        val maxInvoiceNumber = db.transaction { tx -> tx.getMaxInvoiceNumber() }
        assertEquals(4999999999L + drafts.size, maxInvoiceNumber)
    }

    @Test
    fun `send sets numbers correctly when earlier rows exist`() {
        val sentInvoice = testInvoices[1]
        val drafts =
            (1..2).map {
                testInvoices[0].let {
                    it.copy(
                        id = InvoiceId(UUID.randomUUID()),
                        number = null,
                        rows = it.rows.map { row -> row.copy(id = InvoiceRowId(UUID.randomUUID())) },
                    )
                }
            }

        db.transaction { tx -> tx.insert(drafts + sentInvoice) }

        sendInvoices(drafts.map { it.id })

        val maxInvoiceNumber = db.transaction { tx -> tx.getMaxInvoiceNumber() }
        assertEquals(sentInvoice.number!! + drafts.size, maxInvoiceNumber)
    }

    @Test
    fun `send saves cost center information to invoice rows`() {
        fun Database.Read.readCostCenterFields(invoiceId: InvoiceId): Pair<String, String> =
            createQuery {
                    sql(
                        """
                SELECT saved_cost_center, saved_sub_cost_center FROM invoice_row WHERE invoice_id = ${bind(invoiceId)}
            """
                    )
                }
                .exactlyOne {
                    Pair(
                        column<String>("saved_cost_center"),
                        column<String>("saved_sub_cost_center"),
                    )
                }

        val draft = testInvoices[2]
        db.transaction { tx -> tx.insert(listOf(draft)) }

        sendInvoices(listOf(draft.id))

        val (costCenter, subCostCenter) = db.read { it.readCostCenterFields(draft.id) }
        assertEquals(testArea.subCostCenter, subCostCenter)
        assertEquals(testDaycare.costCenter, costCenter)
    }

    @Test
    fun `mark as sent updates invoice status and sent fields`() {
        val invoice = testInvoices.first().copy(status = InvoiceStatus.WAITING_FOR_SENDING)
        db.transaction { tx ->
            tx.insert(listOf(invoice))
            tx.execute { sql("UPDATE invoice_row SET saved_cost_center = '31500'") }
        }

        markInvoicesAsSent(listOf(invoice.id))

        val result = getInvoice(invoice.id)

        val updated =
            invoice.copy(
                status = InvoiceStatus.SENT,
                sentBy = EvakaUserId(testDecisionMaker_1.id.raw),
            )

        assertDetailedEqualEnough(listOf(toDetailed(updated)), listOf(result))
        assertEquals(InvoiceStatus.SENT, result.status)
        assertEquals(testDecisionMaker_1.id.raw, result.sentBy?.raw)
        assertNotNull(result.sentAt)
    }

    @Test
    fun `mark as sent returns bad request if invoice status is wrong`() {
        val invoice = testInvoices.first().copy(status = InvoiceStatus.DRAFT)
        db.transaction { tx -> tx.insert(listOf(invoice)) }

        assertThrows<BadRequest> { markInvoicesAsSent(listOf(invoice.id)) }
    }

    @Test
    fun `mark as sent returns bad request if one of the ids is incorrect`() {
        val invoice = testInvoices.first().copy(status = InvoiceStatus.DRAFT)
        db.transaction { tx -> tx.insert(listOf(invoice)) }

        assertThrows<BadRequest> {
            markInvoicesAsSent(listOf(invoice.id, InvoiceId(UUID.randomUUID())))
        }
    }

    @Test
    fun `createAllDraftInvoices works with one decision`() {
        val decision = decision1
        insertDecisions(listOf(decision))

        createDraftInvoices()

        val drafts =
            db.read { tx ->
                tx.paginatedSearch(
                    1,
                    50,
                    InvoiceSortParam.STATUS,
                    SortDirection.DESC,
                    listOf(InvoiceStatus.DRAFT),
                    listOf(),
                    null,
                    listOf(),
                )
            }

        assertEquals(1, drafts.data.size)
    }

    @Test
    fun `createAllDraftInvoices works with two decisions`() {
        val testDecisions2 = listOf(decision1, decision2)
        insertDecisions(testDecisions2)

        createDraftInvoices()

        val drafts =
            db.read { tx ->
                tx.paginatedSearch(
                    1,
                    50,
                    InvoiceSortParam.STATUS,
                    SortDirection.DESC,
                    listOf(InvoiceStatus.DRAFT),
                    listOf(),
                    null,
                    listOf(),
                )
            }

        assertEquals(2, drafts.data.size)
    }

    @Test
    fun `createAllDraftInvoices is idempotent`() {
        val decisions = listOf(decision1)
        insertDecisions(decisions)

        for (i in 1..4) {
            createDraftInvoices()
        }

        val drafts =
            db.read { tx ->
                tx.paginatedSearch(
                    1,
                    50,
                    InvoiceSortParam.STATUS,
                    SortDirection.DESC,
                    listOf(InvoiceStatus.DRAFT),
                    listOf(),
                    null,
                    listOf(),
                )
            }

        assertEquals(1, drafts.data.size)
    }

    @Test
    fun `createAllDraftInvoices generates no drafts from already invoiced decisions`() {
        val decisions = listOf(decision1)
        insertDecisions(decisions)

        createDraftInvoices()

        val draftIds = getInvoicesWithStatus(InvoiceStatus.DRAFT).map { it.id }
        assertThat(draftIds).isNotEmpty

        sendInvoices(draftIds)

        val sent = getInvoicesWithStatus(InvoiceStatus.SENT)
        assertThat(sent).isNotEmpty

        createDraftInvoices()

        val drafts = getInvoicesWithStatus(InvoiceStatus.DRAFT)
        assertThat(drafts).isEmpty()
    }

    @Test
    fun `createAllDraftInvoices overrides drafts`() {
        val decisions = listOf(decision1)
        insertDecisions(decisions)

        createDraftInvoices()

        val originalDrafts = getInvoicesWithStatus(InvoiceStatus.DRAFT)
        assertEquals(1, originalDrafts.size)

        createDraftInvoices()

        val originalDraft = db.transaction { tx -> tx.getInvoice(originalDrafts.first().id) }
        assertEquals(null, originalDraft)

        val newDrafts = getInvoicesWithStatus(InvoiceStatus.DRAFT)
        assertEquals(1, newDrafts.size)
    }

    @Test
    fun `createAllDraftInvoices does not create overrides for WAITING_FOR_SENDING invoices`() {
        val decisions = listOf(decision1, decisionNoSsn)
        insertDecisions(decisions)

        createDraftInvoices()

        val originalDrafts = getInvoicesWithStatus(InvoiceStatus.DRAFT)
        assertEquals(2, originalDrafts.size)

        sendInvoices(originalDrafts.map { it.id })

        val sent = getInvoicesWithStatus(InvoiceStatus.SENT)
        assertEquals(1, sent.size)

        val waitingForSending = getInvoicesWithStatus(InvoiceStatus.WAITING_FOR_SENDING)
        assertEquals(1, waitingForSending.size)

        val draftsLeft = getInvoicesWithStatus(InvoiceStatus.DRAFT)
        assertEquals(0, draftsLeft.size)

        createDraftInvoices()

        val newDrafts = getInvoicesWithStatus(InvoiceStatus.DRAFT)
        assertEquals(0, newDrafts.size)
    }

    private fun insertDecisions(decisions: List<FeeDecision>) =
        db.transaction { tx ->
            tx.upsertFeeDecisions(decisions)
            decisions.forEach { decision ->
                decision.children.forEach { part ->
                    tx.insert(
                        DevPlacement(
                            childId = part.child.id,
                            unitId = part.placement.unitId,
                            startDate = decision.validFrom,
                            endDate = decision.validTo,
                        )
                    )
                    tx.insert(
                        DevParentship(
                            childId = part.child.id,
                            headOfChildId = decision.headOfFamilyId,
                            startDate = decision.validFrom,
                            endDate = decision.validTo,
                        )
                    )
                }
            }
        }

    private fun createDraftInvoices() {
        invoiceController.createDraftInvoices(dbInstance(), testUser, RealEvakaClock())
    }

    private fun sendInvoices(invoiceIds: List<InvoiceId>) {
        invoiceController.sendInvoices(
            dbInstance(),
            testUser,
            RealEvakaClock(),
            invoiceDate = null,
            dueDate = null,
            invoiceIds = invoiceIds,
        )
    }

    private fun searchInvoices(request: SearchInvoicesRequest): List<InvoiceSummary> {
        return invoiceController
            .searchInvoices(dbInstance(), testUser, RealEvakaClock(), request)
            .data
            .map { it.data }
    }

    private fun getInvoice(id: InvoiceId): InvoiceDetailed {
        return invoiceController.getInvoice(dbInstance(), testUser, RealEvakaClock(), id).data
    }

    private fun markInvoicesAsSent(ids: List<InvoiceId>) {
        invoiceController.markInvoicesSent(dbInstance(), testUser, RealEvakaClock(), ids)
    }

    private fun getInvoicesWithStatus(status: InvoiceStatus): List<InvoiceDetailed> =
        db.transaction { tx -> tx.searchInvoices(status) }

    private fun toDetailed(invoice: DevInvoice): InvoiceDetailed =
        InvoiceDetailed(
            id = invoice.id,
            status = invoice.status,
            revisionNumber = invoice.revisionNumber,
            replacedInvoiceId = invoice.replacedInvoiceId,
            periodStart = invoice.periodStart,
            periodEnd = invoice.periodEnd,
            dueDate = invoice.dueDate,
            invoiceDate = invoice.invoiceDate,
            agreementType = allAreas.find { it.id == invoice.areaId }?.areaCode!!,
            areaId = invoice.areaId,
            headOfFamily = allAdults.find { it.id == invoice.headOfFamilyId }!!.toPersonDetailed(),
            codebtor = allAdults.find { it.id == invoice.codebtor }?.toPersonDetailed(),
            rows =
                invoice.rows.map { row ->
                    val unit = allDaycares.find { it.id == row.unitId }!!
                    InvoiceRowDetailed(
                        id = row.id,
                        child = allChildren.find { it.id == row.childId }!!.toPersonDetailed(),
                        amount = row.amount,
                        unitPrice = row.unitPrice,
                        periodStart = row.periodStart,
                        periodEnd = row.periodEnd,
                        product = row.product,
                        unitId = row.unitId,
                        unitName = unit.name,
                        unitProviderType = unit.providerType,
                        daycareType = unit.type,
                        costCenter = unit.costCenter!!,
                        subCostCenter = allAreas.find { it.id == invoice.areaId }?.subCostCenter!!,
                        savedCostCenter =
                            if (
                                invoice.status == InvoiceStatus.SENT ||
                                    invoice.status == InvoiceStatus.WAITING_FOR_SENDING
                            )
                                unit.costCenter
                            else null,
                        description = row.description,
                        correctionId = row.correctionId,
                        note = null,
                    )
                },
            number = invoice.number,
            sentBy = invoice.sentBy,
            sentAt = invoice.sentAt,
            relatedFeeDecisions = emptyList(),
        )

    private fun toSummary(invoice: DevInvoice): InvoiceSummary =
        InvoiceSummary(
            id = invoice.id,
            status = invoice.status,
            revisionNumber = invoice.revisionNumber,
            periodStart = invoice.periodStart,
            periodEnd = invoice.periodEnd,
            headOfFamily = allAdults.find { it.id == invoice.headOfFamilyId }!!.toPersonDetailed(),
            children =
                invoice.rows
                    .map { row -> allChildren.find { it.id == row.childId }!!.toPersonBasic() }
                    .distinctBy { it.id },
            totalPrice = invoice.rows.sumOf { it.amount * it.unitPrice },
            sentBy = invoice.sentBy,
            sentAt = invoice.sentAt,
            createdAt = invoice.createdAt,
        )
}
