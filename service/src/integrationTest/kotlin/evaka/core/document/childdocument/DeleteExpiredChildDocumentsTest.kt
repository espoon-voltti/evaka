// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.childdocument

import evaka.core.PureJdbiTest
import evaka.core.caseprocess.CaseProcessState
import evaka.core.caseprocess.insertCaseProcess
import evaka.core.caseprocess.insertCaseProcessHistoryRow
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentDeletionBasis
import evaka.core.document.DocumentTemplateContent
import evaka.core.placement.PlacementType
import evaka.core.sficlient.SentSfiMessage
import evaka.core.sficlient.rest.EventType
import evaka.core.sficlient.storeSentSfiMessage
import evaka.core.shared.CaseProcessId
import evaka.core.shared.ChildDocumentDecisionId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.EmployeeId
import evaka.core.shared.SfiMessageId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentDecision
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevSfiMessageEvent
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteExpiredChildDocumentsTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2026, 5, 7)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(2, 0))

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val careArea = DevCareArea()
    private val daycare = DevDaycare(areaId = careArea.id)
    private val child = DevPerson()
    private val citizen = DevPerson()

    @BeforeEach
    fun basicSetup() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(careArea)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(citizen, DevPersonType.ADULT)
        }
    }

    @Test
    fun `STATUS_TRANSITION - deleted exactly when retention has elapsed`() {
        val template =
            insertTemplate(retentionDays = 10, basis = DocumentDeletionBasis.STATUS_TRANSITION)
        val docId = insertDocument(template, statusModifiedAt = now.minusDays(10))

        val resultBefore = db.transaction { tx ->
            tx.deleteExpiredChildDocuments(now.minusMinutes(1))
        }
        assertEquals(0, resultBefore.size)
        assertDocumentExists(docId)

        val resultAt = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(1, resultAt.size)
        assertDocumentDoesNotExist(docId)
    }

    @Test
    fun `STATUS_TRANSITION - non-terminal statuses are also deleted once retention has elapsed`() {
        // status_modified_at is set on creation, so abandoned non-terminal documents are
        // swept by the same retention clock. This is intentional: long-abandoned
        // workspace documents are cleaned up too.
        // Each status only validates against specific document types per the
        // valid_status check constraint; pair each with a permissible type.
        val cases =
            listOf(
                DocumentStatus.DRAFT to ChildDocumentType.PEDAGOGICAL_REPORT,
                DocumentStatus.PREPARED to ChildDocumentType.HOJKS,
                DocumentStatus.CITIZEN_DRAFT to ChildDocumentType.CITIZEN_BASIC,
                DocumentStatus.DECISION_PROPOSAL to ChildDocumentType.OTHER_DECISION,
            )
        val docIds = cases.map { (status, type) ->
            val template =
                insertTemplate(
                    retentionDays = 10,
                    basis = DocumentDeletionBasis.STATUS_TRANSITION,
                    type = type,
                )
            insertDocument(
                template,
                statusModifiedAt = now.minusDays(10),
                status = status,
                decisionMaker = if (type == ChildDocumentType.OTHER_DECISION) admin.id else null,
            )
        }

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(cases.size, result.size)
        docIds.forEach { assertDocumentDoesNotExist(it) }
    }

    @Test
    fun `PLACEMENT_END - deleted when last placement end + retention has elapsed`() {
        val template =
            insertTemplate(retentionDays = 365, basis = DocumentDeletionBasis.PLACEMENT_END)
        val docId = insertDocument(template)

        // Last placement ended exactly retention days ago
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = today.minusYears(2),
                    endDate = today.minusDays(365),
                )
            )
        }

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(1, result.size)
        assertDocumentDoesNotExist(docId)
    }

    @Test
    fun `PLACEMENT_END - not deleted before retention has elapsed`() {
        val template =
            insertTemplate(retentionDays = 365, basis = DocumentDeletionBasis.PLACEMENT_END)
        val docId = insertDocument(template)

        // Placement ended one day too recently to be eligible
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = today.minusYears(2),
                    endDate = today.minusDays(364),
                )
            )
        }

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(0, result.size)
        assertDocumentExists(docId)
    }

    @Test
    fun `PLACEMENT_END - never deleted when child has no placements`() {
        val template =
            insertTemplate(retentionDays = 1, basis = DocumentDeletionBasis.PLACEMENT_END)
        val docId = insertDocument(template)

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(0, result.size)
        assertDocumentExists(docId)
    }

    @Test
    fun `PLACEMENT_END - not deleted when child has only future placements`() {
        val template =
            insertTemplate(retentionDays = 30, basis = DocumentDeletionBasis.PLACEMENT_END)
        val docId = insertDocument(template)

        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = today.plusDays(30),
                    endDate = today.plusDays(60),
                )
            )
        }

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(0, result.size)
        assertDocumentExists(docId)
    }

    @Test
    fun `archive externally and not yet archived - skipped, never deleted`() {
        val template =
            insertTemplate(
                retentionDays = 10,
                basis = DocumentDeletionBasis.STATUS_TRANSITION,
                archiveExternally = true,
            )
        val docId = insertDocument(template, statusModifiedAt = now.minusYears(1))

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(0, result.size)
        assertDocumentExists(docId)
    }

    @Test
    fun `archive externally and already archived - deleted`() {
        val template =
            insertTemplate(
                retentionDays = 10,
                basis = DocumentDeletionBasis.STATUS_TRANSITION,
                archiveExternally = true,
            )
        val docId = insertDocument(template, statusModifiedAt = now.minusYears(1))
        db.transaction { tx -> tx.markDocumentAsArchived(docId, now.minusMonths(6)) }

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(1, result.size)
        assertDocumentDoesNotExist(docId)
    }

    @Test
    fun `limit caps how many documents are deleted in a single run`() {
        val template =
            insertTemplate(retentionDays = 10, basis = DocumentDeletionBasis.STATUS_TRANSITION)
        val docs =
            (1..5).map { i ->
                insertDocument(
                    template,
                    statusModifiedAt = now.minusYears(1),
                    createdAt = now.minusDays(i.toLong()),
                )
            }

        val first = db.transaction { tx -> tx.deleteExpiredChildDocuments(now, limit = 2) }
        assertEquals(2, first.size)

        val second = db.transaction { tx -> tx.deleteExpiredChildDocuments(now, limit = 2) }
        assertEquals(2, second.size)

        val third = db.transaction { tx -> tx.deleteExpiredChildDocuments(now, limit = 2) }
        assertEquals(1, third.size)

        docs.forEach { assertDocumentDoesNotExist(it) }
    }

    @Test
    fun `pending-archival rows are filtered out and do not consume LIMIT slots`() {
        val archiveTemplate =
            insertTemplate(
                retentionDays = 10,
                basis = DocumentDeletionBasis.STATUS_TRANSITION,
                archiveExternally = true,
            )
        val plainTemplate =
            insertTemplate(retentionDays = 10, basis = DocumentDeletionBasis.STATUS_TRANSITION)

        val pendingArchival =
            (1..3).map { insertDocument(archiveTemplate, statusModifiedAt = now.minusYears(1)) }
        val deletable =
            (1..3).map { insertDocument(plainTemplate, statusModifiedAt = now.minusYears(1)) }

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now, limit = 2) }
        assertEquals(2, result.size)
        pendingArchival.forEach { assertDocumentExists(it) }
        assertEquals(1, deletable.count { db.read { tx -> tx.getChildDocument(it) } != null })
    }

    @Test
    fun `deletion cascades to related rows`() {
        val template =
            insertTemplate(
                retentionDays = 10,
                basis = DocumentDeletionBasis.STATUS_TRANSITION,
                type = ChildDocumentType.OTHER_DECISION,
            )
        val decision =
            DevChildDocumentDecision(
                createdBy = admin.id,
                modifiedBy = admin.id,
                status = ChildDocumentDecisionStatus.ACCEPTED,
                validity = DateRange(today.minusYears(1), today.minusMonths(6)),
                daycareId = daycare.id,
            )
        val processId = db.transaction { tx ->
            val process =
                tx.insertCaseProcess(
                    processDefinitionNumber = "12.06.04",
                    year = today.year - 1,
                    organization = "test",
                    archiveDurationMonths = 120,
                )
            tx.insertCaseProcessHistoryRow(
                processId = process.id,
                state = CaseProcessState.INITIAL,
                now = now.minusYears(1),
                userId = admin.evakaUserId,
            )
            process.id
        }
        val docId =
            insertDocument(
                template,
                statusModifiedAt = now.minusYears(1),
                decisionMaker = admin.id,
                decision = decision,
                processId = processId,
                publishedVersions =
                    listOf(
                        DevChildDocumentPublishedVersion(
                            versionNumber = 1,
                            createdAt = now.minusYears(1),
                            createdBy = admin.evakaUserId,
                            publishedContent = DocumentContent(emptyList()),
                        )
                    ),
            )
        val sfiMessageId = db.transaction { tx ->
            tx.markChildDocumentAsRead(
                AuthenticatedUser.Citizen(citizen.id, CitizenAuthLevel.STRONG),
                docId,
                now.minusYears(1),
            )
            val messageId =
                tx.storeSentSfiMessage(SentSfiMessage(guardianId = citizen.id, documentId = docId))
            tx.insert(
                DevSfiMessageEvent(
                    messageId = messageId,
                    eventType = EventType.ELECTRONIC_MESSAGE_CREATED,
                )
            )
            tx.insert(
                DevSfiMessageEvent(messageId = messageId, eventType = EventType.RECEIPT_CONFIRMED)
            )
            messageId
        }

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(1, result.size)

        db.read { tx ->
            assertNull(tx.getChildDocument(docId))
            assertEquals(0, tx.countPublishedVersions(docId))
            assertEquals(0, tx.countReadMarkers(docId))
            assertNull(tx.findDecision(decision.id))
            assertNull(tx.findCaseProcess(processId))
            assertEquals(0, tx.countCaseProcessHistory(processId))
            assertNull(tx.findSfiMessage(sfiMessageId))
            assertEquals(0, tx.countSfiMessageEvents(sfiMessageId))
        }
    }

    @Test
    fun `PLACEMENT_END - retention is measured from the latest of multiple placements`() {
        val template =
            insertTemplate(retentionDays = 30, basis = DocumentDeletionBasis.PLACEMENT_END)
        val docId = insertDocument(template)

        // Earlier placement is well past retention; the later placement is not yet eligible.
        // Retention must be computed from MAX(end_date), so the document must NOT be deleted.
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = today.minusYears(3),
                    endDate = today.minusYears(2),
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = today.minusYears(1),
                    endDate = today.minusDays(29),
                )
            )
        }

        val result = db.transaction { tx -> tx.deleteExpiredChildDocuments(now) }
        assertEquals(0, result.size)
        assertDocumentExists(docId)
    }

    private fun insertTemplate(
        retentionDays: Int,
        basis: DocumentDeletionBasis,
        archiveExternally: Boolean = false,
        type: ChildDocumentType = ChildDocumentType.PEDAGOGICAL_REPORT,
    ): DevDocumentTemplate {
        val template =
            DevDocumentTemplate(
                type = type,
                placementTypes = setOf(PlacementType.DAYCARE),
                validity = DateRange(today.minusYears(5), null),
                content = DocumentTemplateContent(emptyList()),
                archiveExternally = archiveExternally,
                processDefinitionNumber = if (archiveExternally) "12.06.04" else null,
                archiveDurationMonths = if (archiveExternally) 120 else null,
                endDecisionWhenUnitChanges =
                    if (type == ChildDocumentType.OTHER_DECISION) true else null,
                deletionRetentionDays = retentionDays,
                deletionRetentionBasis = basis,
            )
        db.transaction { tx -> tx.insert(template) }
        return template
    }

    private fun insertDocument(
        template: DevDocumentTemplate,
        statusModifiedAt: HelsinkiDateTime = now,
        createdAt: HelsinkiDateTime? = null,
        status: DocumentStatus = DocumentStatus.COMPLETED,
        decision: DevChildDocumentDecision? = null,
        decisionMaker: EmployeeId? = null,
        processId: CaseProcessId? = null,
        publishedVersions: List<DevChildDocumentPublishedVersion> = emptyList(),
    ): ChildDocumentId {
        val doc =
            DevChildDocument(
                status = status,
                childId = child.id,
                templateId = template.id,
                content = DocumentContent(emptyList()),
                createdAt = createdAt,
                modifiedAt = statusModifiedAt,
                modifiedBy = admin.evakaUserId,
                statusModifiedAt = statusModifiedAt,
                contentLockedAt = statusModifiedAt,
                contentLockedBy = admin.id,
                decisionMaker = decisionMaker,
                decision = decision,
                processId = processId,
                publishedVersions = publishedVersions,
            )
        return db.transaction { tx -> tx.insert(doc) }
    }

    private fun assertDocumentExists(id: ChildDocumentId) {
        assertNotNull(db.read { tx -> tx.getChildDocument(id) })
    }

    private fun assertDocumentDoesNotExist(id: ChildDocumentId) {
        assertNull(db.read { tx -> tx.getChildDocument(id) })
    }
}

private fun Database.Read.countPublishedVersions(id: ChildDocumentId): Int =
    createQuery {
            sql(
                "SELECT count(*) FROM child_document_published_version WHERE child_document_id = ${bind(id)}"
            )
        }
        .exactlyOne<Int>()

private fun Database.Read.countReadMarkers(id: ChildDocumentId): Int =
    createQuery { sql("SELECT count(*) FROM child_document_read WHERE document_id = ${bind(id)}") }
        .exactlyOne<Int>()

private fun Database.Read.findDecision(id: ChildDocumentDecisionId): ChildDocumentDecisionId? =
    createQuery { sql("SELECT id FROM child_document_decision WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<ChildDocumentDecisionId>()

private fun Database.Read.findCaseProcess(id: CaseProcessId): CaseProcessId? =
    createQuery { sql("SELECT id FROM case_process WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<CaseProcessId>()

private fun Database.Read.countCaseProcessHistory(id: CaseProcessId): Int =
    createQuery { sql("SELECT count(*) FROM case_process_history WHERE process_id = ${bind(id)}") }
        .exactlyOne<Int>()

private fun Database.Read.findSfiMessage(id: SfiMessageId): SfiMessageId? =
    createQuery { sql("SELECT id FROM sfi_message WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<SfiMessageId>()

private fun Database.Read.countSfiMessageEvents(id: SfiMessageId): Int =
    createQuery { sql("SELECT count(*) FROM sfi_message_event WHERE message_id = ${bind(id)}") }
        .exactlyOne<Int>()
