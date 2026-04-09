// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.archival

import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.CareDetails
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.ChildDocumentDecisionStatus
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.ChildId
import evaka.core.shared.DecisionId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentDecision
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFeeDecision
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevVoucherValueDecision
import evaka.core.shared.dev.TestDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.dev.insertTestDecision
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.UiLanguage
import evaka.instance.tampere.AbstractTampereIntegrationTest
import evaka.instance.tampere.TampereProperties
import evaka.trevaka.archival.planDocumentArchival
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ArchivalSchedulingIntegrationTest : AbstractTampereIntegrationTest() {

    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Autowired private lateinit var tampereProperties: TampereProperties

    private val now = HelsinkiDateTime.of(LocalDateTime.of(2025, 12, 5, 12, 0))
    private val clock = MockEvakaClock(now)
    private val today = clock.today()
    private val user = AuthenticatedUser.SystemInternalUser
    private val employee = DevEmployee()

    private val guardian =
        DevPerson(
            dateOfBirth = LocalDate.of(2007, 1, 1),
            ssn = "010107A995B",
            firstName = "Harrier",
            lastName = "Huoltaja",
        )
    private val childId = ChildId(UUID.randomUUID())
    private val emptyContent = DocumentContent(answers = emptyList())
    private val daycare =
        DevDaycare(
            name = "Test unit",
            areaId = AreaId(UUID.fromString("6529f5a2-9777-11eb-ba89-cfcda122ed3b")),
        )

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(daycare)
            val childId =
                tx.insert(
                    DevPerson(id = childId, dateOfBirth = LocalDate.of(2020, 1, 1)),
                    DevPersonType.CHILD,
                )

            tx.insert(guardian, DevPersonType.ADULT)

            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = childId,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = childId,
                    unitId = daycare.id,
                    startDate = today.plusYears(1).plusDays(1),
                    endDate = today.plusYears(2),
                )
            )
        }
    }

    @Test
    fun `does not schedule jobs for child document decisions if child still placed`() {
        val schedule = tampereProperties.archival?.schedule!!

        // first placement end + schedule delay
        val earlyArchivalClock =
            MockEvakaClock(now.plusYears(1).plusDays(schedule.documentDecisionDelayDays))
        val validityEnd = today.plusYears(1)

        insertTemplateAndDocument(
            validityEnd,
            archiveExternally = true,
            ChildDocumentType.OTHER_DECISION,
        )
        insertTemplateAndDocument(
            validityEnd,
            archiveExternally = true,
            ChildDocumentType.OTHER_DECISION,
            ChildDocumentDecisionStatus.REJECTED,
        )
        val earlyAnnulledDocId =
            insertTemplateAndDocument(
                validityEnd,
                archiveExternally = true,
                ChildDocumentType.OTHER_DECISION,
            )
        annulDocumentDecision(earlyAnnulledDocId)

        planDocumentArchival(db, earlyArchivalClock, asyncJobRunner, schedule)

        val earlyJobs = getScheduledChildDocumentArchivalJobs()
        assertEquals(0, earlyJobs.size)
    }

    @Test
    fun `schedules archival job for accepted, annulled and rejected child documents decisions after last placement end`() {
        val schedule = tampereProperties.archival?.schedule!!

        // last placement end + schedule delay
        val lateArchivalClock =
            MockEvakaClock(now.plusYears(2).plusDays(schedule.documentDecisionDelayDays))
        val validityEnd = today.plusYears(2)

        val lateDocId =
            insertTemplateAndDocument(
                validityEnd,
                archiveExternally = true,
                ChildDocumentType.OTHER_DECISION,
            )
        val lateAnnulledDocId =
            insertTemplateAndDocument(
                validityEnd,
                archiveExternally = true,
                ChildDocumentType.OTHER_DECISION,
            )
        val lateRejectedDocId =
            insertTemplateAndDocument(
                validityEnd,
                archiveExternally = true,
                ChildDocumentType.OTHER_DECISION,
                ChildDocumentDecisionStatus.REJECTED,
            )
        annulDocumentDecision(lateAnnulledDocId)

        planDocumentArchival(db, lateArchivalClock, asyncJobRunner, schedule)

        val lateJobs = getScheduledChildDocumentArchivalJobs().map { it.documentId }
        assertEquals(3, lateJobs.size)
        assertThat(lateJobs)
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    lateDocId.toString(),
                    lateAnnulledDocId.toString(),
                    lateRejectedDocId.toString(),
                )
            )
    }

    @Test
    fun `schedules archival job for a sent fee decision well after approval`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        val feeDecisionId = insertFeeDecision()

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledFeeDecisionArchivalJobs()
        assertEquals(1, jobs.size)
        assertEquals(feeDecisionId.toString(), jobs.first().documentId)
    }

    @Test
    fun `schedules archival job for a sent voucher value decision well after approval`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        val voucherValueDecisionId = insertVoucherValueDecision()

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledVoucherValueDecisionArchivalJobs()
        assertEquals(1, jobs.size)
        assertEquals(voucherValueDecisionId.toString(), jobs.first().documentId)
    }

    @Test
    fun `schedules archival job for a placement decision well after resolution`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.decisionDelayDays))

        val decisionId = insertDecisionData()

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledDecisionArchivalJobs()
        assertEquals(1, jobs.size)
        assertEquals(decisionId.toString(), jobs.first().documentId)
    }

    @Test
    fun `does not schedule job for child document decision when placement end too recent`() {
        val schedule = tampereProperties.archival?.schedule!!

        val validityEnd = today.plusYears(1)

        insertTemplateAndDocument(
            validityEnd,
            archiveExternally = true,
            ChildDocumentType.OTHER_DECISION,
        )

        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobs = getScheduledChildDocumentArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule job for fee decision when approval too recent`() {
        val schedule = tampereProperties.archival?.schedule!!

        insertFeeDecision()

        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobs = getScheduledFeeDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule job for voucher value decision when approval too recent`() {
        val schedule = tampereProperties.archival?.schedule!!

        insertVoucherValueDecision()

        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobs = getScheduledVoucherValueDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule archival job for a placement decision too soon after resolution`() {
        val schedule = tampereProperties.archival?.schedule!!

        insertDecisionData()

        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobs = getScheduledDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule job for child document decision when not marked for archival or document already archived`() {
        val schedule = tampereProperties.archival?.schedule!!

        // placement end + schedule delay
        val eligibleArchivalClock =
            MockEvakaClock(now.plusYears(1).plusDays(schedule.documentDecisionDelayDays))
        val validityEnd = today.plusYears(1)

        insertTemplateAndDocument(
            validityEnd,
            archiveExternally = false,
            ChildDocumentType.OTHER_DECISION,
        )

        val docId = insertTemplateAndDocument(validityEnd, archiveExternally = true)
        markDocumentArchived(docId)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledChildDocumentArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule archival job for a sent fee decision it it's already archived`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        val feeDecisionId = insertFeeDecision()
        markFeeDecisionArchived(feeDecisionId)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledFeeDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule archival job for a sent voucher value decision if it's already archived`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        val voucherValueDecisionId = insertVoucherValueDecision()
        markVoucherValueDecisionArchived(voucherValueDecisionId)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledVoucherValueDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule archival job for a placement decision that's already archived`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.decisionDelayDays))

        val decisionId = insertDecisionData()
        markDecisionArchived(decisionId)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule archival job for a draft fee decision`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        insertFeeDecision(status = FeeDecisionStatus.DRAFT)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledFeeDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `schedules archival job for an annulled fee decision`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        val decisionId = insertFeeDecision(status = FeeDecisionStatus.ANNULLED)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledFeeDecisionArchivalJobs()
        assertEquals(1, jobs.size)
        assertEquals(decisionId.toString(), jobs.first().documentId)
    }

    @Test
    fun `schedules archival job for an annulled voucher value decision`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        val decisionId = insertVoucherValueDecision(status = VoucherValueDecisionStatus.ANNULLED)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledVoucherValueDecisionArchivalJobs()
        assertEquals(1, jobs.size)
        assertEquals(decisionId.toString(), jobs.first().documentId)
    }

    @Test
    fun `does not schedule archival job for a draft voucher value decision`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        insertVoucherValueDecision(status = VoucherValueDecisionStatus.DRAFT)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledVoucherValueDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `schedules archival job for a placement decision that is rejected`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.decisionDelayDays))

        val decisionId = insertDecisionData(status = DecisionStatus.REJECTED)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledDecisionArchivalJobs()
        assertEquals(1, jobs.size)
        assertEquals(decisionId.toString(), jobs.first().documentId)
    }

    @Test
    fun `does not schedule archival job for a placement decision that is pending`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.decisionDelayDays))

        insertDecisionData(status = DecisionStatus.PENDING)

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `schedules archival job for all decision types except CLUB`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.decisionDelayDays))

        val archivableDecisionIds =
            listOf(
                    DecisionType.DAYCARE,
                    DecisionType.PRESCHOOL,
                    DecisionType.PRESCHOOL_DAYCARE,
                    DecisionType.PRESCHOOL_CLUB,
                    DecisionType.PREPARATORY_EDUCATION,
                    DecisionType.DAYCARE_PART_TIME,
                )
                .map { insertDecisionData(type = it) }

        listOf(DecisionType.CLUB).forEach { insertDecisionData(type = it) }

        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobDocs = getScheduledDecisionArchivalJobs().map { it.documentId }
        assertEquals(archivableDecisionIds.size, jobDocs.size)
        assertThat(jobDocs)
            .containsExactlyInAnyOrderElementsOf(archivableDecisionIds.map { it.toString() })
    }

    @Test
    fun `schedules archival job for vasu when template old enough`() {
        // Template validity ended long enough ago for archival
        val schedule = tampereProperties.archival?.schedule!!

        val validityEnd = today.minusDays(schedule.documentPlanDelayDays)
        val docId = insertTemplateAndDocument(validityEnd, archiveExternally = true)

        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobs = getScheduledChildDocumentArchivalJobs()
        assertEquals(1, jobs.size)
        assertEquals(docId.toString(), jobs.first().documentId)
    }

    @Test
    fun `does not schedule job for vasu when template too recent, not marked for archival or document already archived`() {
        val schedule = tampereProperties.archival?.schedule!!

        val tooRecent = today.minusDays(schedule.documentPlanDelayDays - 1)
        val oldEnough = today.minusDays(schedule.documentPlanDelayDays)

        insertTemplateAndDocument(tooRecent, archiveExternally = true)
        insertTemplateAndDocument(oldEnough, archiveExternally = false)

        val docId = insertTemplateAndDocument(oldEnough, archiveExternally = true)
        markDocumentArchived(docId)

        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobs = getScheduledChildDocumentArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule job for vasu without document key`() {
        val schedule = tampereProperties.archival?.schedule!!

        val oldEnough = today.minusDays(schedule.documentPlanDelayDays)

        insertTemplateAndDocument(oldEnough, archiveExternally = true, documentKey = null)
        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobs = getScheduledChildDocumentArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule job for child document decision without document key`() {
        val schedule = tampereProperties.archival?.schedule!!

        // last placement end + schedule delay
        val lateArchivalClock =
            MockEvakaClock(now.plusYears(2).plusDays(schedule.documentDecisionDelayDays))
        val validityEnd = today.plusYears(2)

        insertTemplateAndDocument(
            validityEnd,
            archiveExternally = true,
            ChildDocumentType.OTHER_DECISION,
            documentKey = null,
        )

        planDocumentArchival(db, lateArchivalClock, asyncJobRunner, schedule)

        val lateJobs = getScheduledChildDocumentArchivalJobs().map { it.documentId }
        assertEquals(0, lateJobs.size)
    }

    @Test
    fun `does not schedule job for decision without document key`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.decisionDelayDays))

        insertDecisionData(documentKey = null)
        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule job for fee decision without document key`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.feeDecisionDelayDays))

        insertFeeDecision(documentKey = null)
        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledFeeDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule job for voucher value decision without document key`() {
        val schedule = tampereProperties.archival?.schedule!!

        // add schedule delay to simulated 'now'
        val eligibleArchivalClock = MockEvakaClock(now.plusDays(schedule.voucherDecisionDelayDays))

        insertVoucherValueDecision(documentKey = null)
        planDocumentArchival(db, eligibleArchivalClock, asyncJobRunner, schedule)

        val jobs = getScheduledVoucherValueDecisionArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `respects archival limit when specified`() {
        val schedule = tampereProperties.archival?.schedule!!.copy(dailyDocumentLimit = 5)
        val oldEnough = today.minusDays(schedule.documentPlanDelayDays)

        // Insert in excess of the limit
        val docIds =
            List((schedule.dailyDocumentLimit * 3).toInt()) {
                insertTemplateAndDocument(oldEnough, archiveExternally = true).toString()
            }
        // Plan with limit
        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobDocIds = getScheduledChildDocumentArchivalJobs().map { it.documentId }
        assertEquals(schedule.dailyDocumentLimit.toInt(), jobDocIds.size)
        assertThat(docIds).containsAll(jobDocIds)
    }

    @Test
    fun `archival limit 0 prevents archival jobs`() {
        val schedule = tampereProperties.archival?.schedule!!.copy(dailyDocumentLimit = 0)
        val oldEnough = today.minusDays(schedule.documentPlanDelayDays)

        // Insert in excess of the limit
        repeat(3) { insertTemplateAndDocument(oldEnough, archiveExternally = true).toString() }
        // Plan with limit
        planDocumentArchival(db, clock, asyncJobRunner, schedule)

        val jobDocIds = getScheduledChildDocumentArchivalJobs().map { it.documentId }
        assertEquals(schedule.dailyDocumentLimit.toInt(), jobDocIds.size)
        assertThat(jobDocIds).isEmpty()
    }

    private fun markDocumentArchived(documentId: ChildDocumentId) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    UPDATE child_document
                    SET archived_at = ${bind(now)}
                    WHERE id = ${bind(documentId)}
                    """
                )
            }
        }
    }

    private fun markFeeDecisionArchived(feeDecisionId: FeeDecisionId) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    UPDATE fee_decision
                    SET archived_at = ${bind(now)}
                    WHERE id = ${bind(feeDecisionId)}
                    """
                )
            }
        }
    }

    private fun markVoucherValueDecisionArchived(voucherDecisionId: VoucherValueDecisionId) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    UPDATE voucher_value_decision
                    SET archived_at = ${bind(now)}
                    WHERE id = ${bind(voucherDecisionId)}
                    """
                )
            }
        }
    }

    private fun markDecisionArchived(decisionId: DecisionId) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    UPDATE decision
                    SET archived_at = ${bind(now)}
                    WHERE id = ${bind(decisionId)}
                    """
                )
            }
        }
    }

    private fun annulDocumentDecision(documentId: ChildDocumentId) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    UPDATE child_document_decision
                    SET status = 'ANNULLED', annulment_reason = 'TEST REASON'
                    WHERE id = (SELECT decision_id from child_document
                        WHERE id = ${bind(documentId)}
                        LIMIT 1)
                    """
                )
            }
        }
    }

    private fun insertTemplateAndDocument(
        validityEnd: LocalDate,
        archiveExternally: Boolean,
        type: ChildDocumentType = ChildDocumentType.VASU,
        status: ChildDocumentDecisionStatus = ChildDocumentDecisionStatus.ACCEPTED,
        documentKey: String? = "test-key",
    ): ChildDocumentId {
        return db.transaction { tx ->
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = "Test Template",
                        type = type,
                        language = UiLanguage.FI,
                        validity = DateRange(LocalDate.of(2020, 1, 1), validityEnd),
                        content = DocumentTemplateContent(sections = emptyList()),
                        archiveExternally = archiveExternally,
                        processDefinitionNumber = if (archiveExternally) "12.06.01" else null,
                        archiveDurationMonths = if (archiveExternally) 120 else null,
                        endDecisionWhenUnitChanges =
                            if (type == ChildDocumentType.OTHER_DECISION) true else null,
                    )
                )

            var docId: ChildDocumentId
            if (type != ChildDocumentType.OTHER_DECISION) {
                docId =
                    tx.insert(
                        DevChildDocument(
                            childId = childId,
                            templateId = templateId,
                            status = DocumentStatus.COMPLETED,
                            content = emptyContent,
                            modifiedAt = now,
                            modifiedBy = user.evakaUserId,
                            contentLockedAt = now,
                            contentLockedBy = null,
                            publishedVersions =
                                listOf(
                                    DevChildDocumentPublishedVersion(
                                        versionNumber = 1,
                                        createdAt = now,
                                        createdBy = user.evakaUserId,
                                        publishedContent = emptyContent,
                                        documentKey = documentKey,
                                    )
                                ),
                        )
                    )
            } else {
                docId =
                    tx.insert(
                        DevChildDocument(
                            childId = childId,
                            templateId = templateId,
                            status = DocumentStatus.COMPLETED,
                            content = emptyContent,
                            modifiedAt = now,
                            modifiedBy = user.evakaUserId,
                            contentLockedAt = now,
                            contentLockedBy = null,
                            decision =
                                DevChildDocumentDecision(
                                    daycareId = daycare.id,
                                    validity =
                                        if (status != ChildDocumentDecisionStatus.REJECTED)
                                            DateRange(LocalDate.of(2020, 1, 1), validityEnd)
                                        else null,
                                    modifiedBy = employee.id,
                                    createdBy = employee.id,
                                    status = status,
                                ),
                            decisionMaker = employee.id,
                            publishedVersions =
                                listOf(
                                    DevChildDocumentPublishedVersion(
                                        versionNumber = 1,
                                        createdAt = now,
                                        createdBy = user.evakaUserId,
                                        publishedContent = emptyContent,
                                        documentKey = documentKey,
                                    )
                                ),
                        )
                    )
            }
            return@transaction docId
        }
    }

    private fun insertFeeDecision(
        status: FeeDecisionStatus = FeeDecisionStatus.SENT,
        approvedAt: HelsinkiDateTime = HelsinkiDateTime.of(today, LocalTime.of(0, 0)),
        documentKey: String? = "test-key",
    ): FeeDecisionId =
        db.transaction { tx ->
            tx.insert(
                DevFeeDecision(
                    headOfFamilyId = guardian.id,
                    status = status,
                    validDuring = FiniteDateRange(today, today.plusMonths(1)),
                    approvedAt = approvedAt,
                    documentKey = documentKey,
                )
            )
        }

    private fun insertVoucherValueDecision(
        status: VoucherValueDecisionStatus = VoucherValueDecisionStatus.SENT,
        approvedAt: HelsinkiDateTime = HelsinkiDateTime.of(today, LocalTime.of(0, 0)),
        documentKey: String? = "test-key",
    ): VoucherValueDecisionId =
        db.transaction { tx ->
            tx.insert(
                DevVoucherValueDecision(
                    headOfFamilyId = guardian.id,
                    status = status,
                    validFrom = today,
                    validTo = today.plusMonths(1),
                    approvedAt = approvedAt,
                    childId = childId,
                    placementUnitId = daycare.id,
                    documentKey = documentKey,
                )
            )
        }

    private fun insertDecisionData(
        status: DecisionStatus = DecisionStatus.ACCEPTED,
        type: DecisionType = DecisionType.DAYCARE,
        resolvedAt: HelsinkiDateTime = HelsinkiDateTime.of(today, LocalTime.of(0, 0)),
        documentKey: String? = "test-key",
    ): DecisionId =
        db.transaction { tx ->
            val appId =
                tx.insertTestApplication(
                    type =
                        when (type) {
                            DecisionType.DAYCARE -> ApplicationType.DAYCARE
                            DecisionType.PRESCHOOL -> ApplicationType.PRESCHOOL
                            DecisionType.CLUB -> ApplicationType.CLUB
                            DecisionType.PRESCHOOL_DAYCARE -> ApplicationType.DAYCARE
                            DecisionType.PRESCHOOL_CLUB -> ApplicationType.CLUB
                            DecisionType.PREPARATORY_EDUCATION -> ApplicationType.PRESCHOOL
                            DecisionType.DAYCARE_PART_TIME -> ApplicationType.DAYCARE
                        },
                    status = ApplicationStatus.ACTIVE,
                    guardianId = guardian.id,
                    childId = childId,
                    confidential = true,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            connectedDaycare = false,
                            urgent = true,
                            careDetails = CareDetails(assistanceNeeded = true),
                            extendedCare = true,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycare.id)),
                            preferredStartDate = LocalDate.of(2026, 1, 1),
                        ),
                )
            tx.insertTestDecision(
                TestDecision(
                    applicationId = appId,
                    unitId = daycare.id,
                    status = status,
                    createdBy = user.evakaUserId,
                    type = type,
                    startDate = today,
                    endDate = today.plusMonths(1),
                    resolved = resolvedAt,
                    resolvedBy = user.evakaUserId.raw,
                    documentKey = documentKey,
                )
            )
        }

    private fun getScheduledDecisionArchivalJobs(): List<AsyncJobInfo> =
        db.read { tx ->
            tx.createQuery {
                    sql(
                        """
                        SELECT payload->>'decisionId' as document_id
                        FROM async_job
                        WHERE type = 'ArchiveDecision'
                        ORDER BY id
                        """
                    )
                }
                .toList<AsyncJobInfo>()
        }

    private fun getScheduledFeeDecisionArchivalJobs(): List<AsyncJobInfo> =
        db.read { tx ->
            tx.createQuery {
                    sql(
                        """
                        SELECT payload->>'feeDecisionId' as document_id
                        FROM async_job
                        WHERE type = 'ArchiveFeeDecision'
                        ORDER BY id
                        """
                    )
                }
                .toList<AsyncJobInfo>()
        }

    private fun getScheduledVoucherValueDecisionArchivalJobs(): List<AsyncJobInfo> =
        db.read { tx ->
            tx.createQuery {
                    sql(
                        """
                        SELECT payload->>'voucherValueDecisionId' as document_id
                        FROM async_job
                        WHERE type = 'ArchiveVoucherValueDecision'
                        ORDER BY id
                        """
                    )
                }
                .toList<AsyncJobInfo>()
        }

    private fun getScheduledChildDocumentArchivalJobs(): List<AsyncJobInfo> =
        db.read { tx ->
            tx.createQuery {
                    sql(
                        """
                        SELECT payload->>'documentId' as document_id
                        FROM async_job
                        WHERE type = 'ArchiveChildDocument'
                        ORDER BY id
                        """
                    )
                }
                .toList<AsyncJobInfo>()
        }

    data class AsyncJobInfo(val documentId: String)
}
