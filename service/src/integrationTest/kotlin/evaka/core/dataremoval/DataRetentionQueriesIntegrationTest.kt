// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.PureJdbiTest
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child as ApplicationFormChild
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.childimages.insertChildImage
import evaka.core.decision.DecisionType
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentDeletionBasis
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.Question
import evaka.core.document.Section
import evaka.core.document.childdocument.AnsweredQuestion
import evaka.core.document.childdocument.ChildDocumentDecisionStatus
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.incomestatement.IncomeStatementBody
import evaka.core.incomestatement.IncomeStatementStatus
import evaka.core.insertServiceNeedOptions
import evaka.core.messaging.createPersonMessageAccount
import evaka.core.placement.PlacementSource
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.ChildImageId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.IncomeStatementId
import evaka.core.shared.PartnershipId
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevBackupCare
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentDecision
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFamilyContact
import evaka.core.shared.dev.DevFeeDecision
import evaka.core.shared.dev.DevFeeDecisionChild
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevFridgePartnership
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevIncome
import evaka.core.shared.dev.DevIncomeStatement
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevPlacementDraft
import evaka.core.shared.dev.DevPlacementPlan
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.DevVoucherValueDecision
import evaka.core.shared.dev.TestDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.dev.insertTestDecision
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.snDefaultDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.BeforeEach

class DataRetentionQueriesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2026, 9, 12)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(2, 0))
    private val longAgo = HelsinkiDateTime.of(LocalDate.of(2011, 1, 1), LocalTime.NOON)
    private val schema = buildDataRetentionSchema(financeFreezeImplemented = true)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val employee = DevEmployee()
    private val child = DevPerson(dateOfBirth = LocalDate.of(2011, 3, 4))
    private val guardian = DevPerson(dateOfBirth = LocalDate.of(1981, 5, 6))
    private val otherGuardian = DevPerson(dateOfBirth = LocalDate.of(1982, 7, 8))

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insertServiceNeedOptions()
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(guardian, DevPersonType.RAW_ROW)
            tx.insert(otherGuardian, DevPersonType.RAW_ROW)
            tx.setCreatedLongAgo(child.id, guardian.id, otherGuardian.id)
        }
    }

    private fun load(id: PersonId) = db.transaction { checkNotNull(it.loadPersonGraph(schema, id)) }

    private fun plan(id: PersonId) = load(id).evaluate(today)

    private fun execute(id: PersonId) = db.transaction { tx ->
        tx.executeDeletionPlan(
            checkNotNull(tx.loadPersonGraph(schema, id)).evaluate(today),
            now,
        )
    }

    /** The loaded rows by table */
    private fun PersonGraph.rows(): Map<String, Int> =
        ownNodes.groupingBy { it.table.name }.fold(0) { count, node -> count + node.rows.size }

    private fun PersonGraph.foreign(): Set<String> =
        foreignNodes.map { "${it.table.name}.${it.referenceColumn}" }.toSet()

    private fun remaining(id: PersonId): Set<String> =
        load(id).ownNodes.filter { it.rows.isNotEmpty() }.map { it.table.name }.toSet()

    private fun count(sql: String): Int = db.read {
        it.createQuery { sql("SELECT count(*) FROM $sql") }.exactlyOne<Int>()
    }

    private fun personExists(id: PersonId) = count("person WHERE id = '${id.raw}'") == 1

    /** The Koski and Varda freeze timestamps of the child row */
    private fun freezes(id: PersonId): Map<String, HelsinkiDateTime?> = db.read { tx ->
        tx.createQuery {
                sql(
                    "SELECT koski_data_first_removed_at AS koski, varda_data_first_removed_at AS varda FROM child WHERE id = ${bind(id)}"
                )
            }
            .exactlyOne {
                mapOf(
                    "koski" to column<HelsinkiDateTime?>("koski"),
                    "varda" to column<HelsinkiDateTime?>("varda"),
                )
            }
    }

    private val noFreezes = mapOf<String, HelsinkiDateTime?>("koski" to null, "varda" to null)

    @Test
    fun `the child's graph holds the child's own rows, and the adults' rows that name the child as foreign rows`() {
        val family = db.transaction { it.insertFamily() }

        val graph = load(child.id)

        assertEquals(child.id, graph.targetPersonId)
        assertEquals(
            mapOf(
                "person" to 1,
                "child" to 1,
                "placement" to 2,
                "service_need" to 1,
                "daycare_group_placement" to 1,
                "backup_care" to 1,
                "application" to 1,
                "application_note" to 1,
                "application_other_guardian" to 1,
                "placement_plan" to 1,
                "placement_draft" to 1,
                "decision" to 1,
                "koski_study_right" to 1,
                "koski_upload_error" to 1,
                "varda_state" to 1,
                "child_images" to 1,
                "child_document" to 1,
                "child_document_read" to 1,
                "child_document_published_version" to 1,
                "guardian" to 2,
                "family_contact" to 1,
                "voucher_value_decision" to 1,
            ),
            graph.rows(),
        )
        assertEquals(setOf("fee_decision_child.child_id", "fridge_child.child_id"), graph.foreign())

        val placement = graph.ownNode(OwnNodeId.WholeTable("placement"))
        assertEquals(
            family.placementIds.map { it.raw }.toSet(),
            placement.rows.map { it.id.single() }.toSet(),
        )
        assertEquals(
            setOf(LocalDate.of(2013, 7, 31), family.lastPlacementEnd),
            placement.rows.map { it.dateByColumn.getValue("end_date") }.toSet(),
        )
        assertEquals(
            setOf(child.id.raw),
            placement.rows.map { it.referencedIdByColumn.getValue("child_id") }.toSet(),
        )

        val document =
            graph.ownNode(
                OwnNodeId.SingleRow("child_document", RowId(mapOf("id" to family.documentId.raw)))
            )
        assertEquals(
            family.lastPlacementEnd.plusDays(retentionDays.toLong()),
            document.rows.single().dateByCustomSource.getValue(childDocumentRetentionEnd),
        )
        assertEquals(
            true,
            document.rows
                .single()
                .mayExpireByArchivedRule
                .getValue(childDocumentArchivedIfRequired),
        )

        val read =
            graph.ownNode(
                OwnNodeId.SingleRow(
                    "child_document_read",
                    RowId(
                        mapOf(
                            "document_id" to family.documentId.raw,
                            "person_id" to guardian.id.raw,
                        )
                    ),
                )
            )
        assertEquals(guardian.id.raw, read.rows.single().referencedIdByColumn.getValue("person_id"))

        val decision = graph.ownNode(OwnNodeId.WholeTable("decision"))
        assertEquals(
            mapOf(
                "document_key" to "decision.pdf",
                "other_guardian_document_key" to "decision-other-guardian.pdf",
            ),
            decision.rows.single().valueForJobByColumn,
        )
    }

    @Test
    fun `an adult's graph holds the adult's own rows, one node per row where the rows are independent, and the family's rows that name the adult as foreign rows`() {
        val family = db.transaction { it.insertFamily() }

        val graph = load(guardian.id)

        assertEquals(guardian.id, graph.targetPersonId)
        assertEquals(
            mapOf(
                "person" to 1,
                "child" to 0,
                "fridge_child" to 1,
                "fridge_partner" to 1,
                "income" to 2,
                "income_statement" to 1,
                "fee_decision" to 1,
                "fee_decision_child" to 1,
            ),
            graph.rows(),
        )
        assertEquals(2, graph.ownNodes.count { it.table.name == "income" })
        assertEquals(
            OwnNodeId.SingleRow(
                "fridge_partner",
                RowId(mapOf("partnership_id" to family.partnershipId.raw)),
            ),
            graph.ownNodes.single { it.table.name == "fridge_partner" }.id,
        )
        assertEquals(
            setOf(
                "application.guardian_id",
                "guardian.guardian_id",
                "child_document_read.person_id",
                "message_account.person_id",
                "voucher_value_decision.head_of_family_id",
            ),
            graph.foreign(),
        )

        val other = load(otherGuardian.id)
        assertEquals(mapOf("person" to 1, "child" to 0, "fridge_partner" to 1), other.rows())
        assertEquals(
            setOf(
                "application_other_guardian.guardian_id",
                "fee_decision.partner_id",
                "guardian.guardian_id",
                "family_contact.contact_person_id",
                "voucher_value_decision.partner_id",
            ),
            other.foreign(),
        )
    }

    @Test
    fun `the child's run deletes what the child's rules govern, clears the optional references and leaves what the adults' rows still name`() {
        val family = db.transaction { it.insertFamily() }
        // The same guardian's row for a sibling shares one column of the composite key
        val sibling = DevPerson(dateOfBirth = LocalDate.of(2013, 1, 1))
        db.transaction { tx ->
            tx.insert(sibling, DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = sibling.id))
        }

        val result = execute(child.id)

        // The order of deletion is leaf-first; the database's foreign key checks enforce it here
        assertEquals(
            mapOf(
                "application_note" to 1,
                "application_other_guardian" to 1,
                "placement_plan" to 1,
                "placement_draft" to 1,
                "decision" to 1,
                "application" to 1,
                "backup_care" to 1,
                "child_images" to 1,
                "child_document_read" to 1,
                "child_document_published_version" to 1,
                "child_document" to 1,
                "guardian" to 2,
                "family_contact" to 1,
                "voucher_value_decision" to 1,
            ),
            result.deletedRowCountsByTable,
        )
        assertEquals(
            mapOf(
                ("placement" to "source_application_id") to 1,
                ("fridge_child" to "created_by_application") to 1,
                ("income" to "application_id") to 1,
                ("fridge_partner" to "created_from_application") to 2,
                ("message_thread" to "application_id") to 1,
            ),
            result.clearedRowCountsByColumn,
        )
        assertEquals(emptyList(), result.childrenFrozenForKoski)
        assertEquals(listOf(child.id), result.childrenFrozenForVarda)

        // The fee decision child and parentship rows of the adult's data hold the child row, and
        // with it the placements the child row bundles
        assertEquals(
            setOf(
                "person",
                "child",
                "placement",
                "service_need",
                "daycare_group_placement",
                "koski_study_right",
                "koski_upload_error",
                "varda_state",
            ),
            remaining(child.id),
        )
        assertEquals(1, count("guardian WHERE child_id = '${sibling.id.raw}'"))
        assertEquals(
            1,
            count(
                "placement WHERE id = '${family.placementIds.last().raw}' AND source IS NULL AND source_application_id IS NULL"
            ),
        )
        assertEquals(
            1,
            count(
                "income WHERE person_id = '${guardian.id.raw}' AND application_id IS NULL AND valid_to IS NOT NULL"
            ),
        )
        assertEquals(
            2,
            count(
                "fridge_partner WHERE partnership_id = '${family.partnershipId.raw}' AND created_from_application IS NULL AND create_source IS NULL"
            ),
        )
        assertEquals(
            1,
            count(
                "fridge_child WHERE child_id = '${child.id.raw}' AND created_by_application IS NULL AND create_source IS NULL"
            ),
        )
        assertEquals(1, count("message_thread WHERE application_id IS NULL"))
        assertEquals(mapOf("koski" to null, "varda" to now), freezes(child.id))
    }

    @Test
    fun `the plan queues the file deletions of the deleted rows`() {
        val family = db.transaction { it.insertFamily() }

        val plan = plan(child.id)

        assertEquals(
            setOf(
                AsyncJob.DeleteDecisionPdf("decision.pdf"),
                AsyncJob.DeleteDecisionPdf("decision-other-guardian.pdf"),
                AsyncJob.DeleteChildImage(family.imageId),
                AsyncJob.DeleteChildDocumentPdf("document.pdf"),
                AsyncJob.DeleteVoucherValueDecisionPdf("value-decision.pdf"),
            ),
            plan.asyncJobs.toSet(),
        )
        assertEquals(5, plan.asyncJobs.size)
    }

    @Test
    fun `an adult's run deletes the expired fee decision with its child rows, and the person row waits for the child's data, the messaging job, the adult's own rows and a recent login`() {
        val family = db.transaction { it.insertFamily() }

        assertEquals(
            setOf(AsyncJob.DeleteFeeDecisionPdf("fee-decision.pdf")),
            plan(guardian.id).asyncJobs.toSet(),
        )
        val adult = execute(guardian.id)

        assertEquals(
            mapOf(
                "fee_decision_child" to 1,
                "fee_decision" to 1,
                "fridge_child" to 1,
                "income" to 1,
                "income_statement" to 1,
                "fridge_partner" to 2,
            ),
            adult.deletedRowCountsByTable,
        )
        assertEquals(emptyList(), adult.childrenFrozenForKoski)
        assertEquals(listOf(child.id), adult.childrenFrozenForVarda)
        // The partnership is deleted by its id, so the rows of both partners go
        assertEquals(
            0,
            count("fridge_partner WHERE partnership_id = '${family.partnershipId.raw}'"),
        )
        assertEquals(setOf("person", "income"), remaining(guardian.id))

        val childRun = execute(child.id)

        assertEquals(
            mapOf(
                "application_note" to 1,
                "application_other_guardian" to 1,
                "placement_plan" to 1,
                "placement_draft" to 1,
                "decision" to 1,
                "application" to 1,
                "service_need" to 1,
                "daycare_group_placement" to 1,
                "placement" to 2,
                "backup_care" to 1,
                "koski_study_right" to 1,
                "koski_upload_error" to 1,
                "varda_state" to 1,
                "child_images" to 1,
                "child_document_read" to 1,
                "child_document_published_version" to 1,
                "child_document" to 1,
                "guardian" to 2,
                "family_contact" to 1,
                "voucher_value_decision" to 1,
                "child" to 1,
                "person" to 1,
            ),
            childRun.deletedRowCountsByTable,
        )
        // The child row goes with the plan, so there is nothing to freeze
        assertEquals(emptyList(), childRun.childrenFrozenForKoski)
        assertEquals(emptyList(), childRun.childrenFrozenForVarda)
        assertEquals(false, personExists(child.id))

        // The message account is handled elsewhere and holds the person row, and so does the
        // income that has not expired
        assertEquals(emptyMap(), execute(guardian.id).deletedRowCountsByTable)
        db.transaction {
            it.execute { sql("DELETE FROM message_account WHERE person_id = ${bind(guardian.id)}") }
        }
        assertEquals(emptyMap(), execute(guardian.id).deletedRowCountsByTable)
        db.transaction {
            it.execute {
                sql(
                    "UPDATE income SET valid_from = '2013-01-01', valid_to = '2014-12-31' WHERE person_id = ${bind(guardian.id)}"
                )
            }
            it.execute {
                sql("UPDATE person SET last_login = ${bind(now)} WHERE id = ${bind(guardian.id)}")
            }
        }
        // A recent login keeps the person row on its own
        assertEquals(mapOf("income" to 1), execute(guardian.id).deletedRowCountsByTable)
        db.transaction {
            it.execute {
                sql("UPDATE person SET last_login = NULL WHERE id = ${bind(guardian.id)}")
            }
        }
        assertEquals(mapOf("person" to 1), execute(guardian.id).deletedRowCountsByTable)
        assertEquals(false, personExists(guardian.id))
    }

    @Test
    fun `the rows deleted are exactly the rows evaluated`() {
        db.transaction { it.insertFamily() }
        val plan = plan(guardian.id)
        val lateIncome = db.transaction { tx ->
            tx.insert(
                DevIncome(
                    personId = guardian.id,
                    validFrom = LocalDate.of(2010, 1, 1),
                    validTo = LocalDate.of(2010, 12, 31),
                    modifiedBy = employee.evakaUserId,
                )
            )
        }

        val result = db.transaction { it.executeDeletionPlan(plan, now) }

        assertEquals(1, result.deletedRowCountsByTable["income"])
        assertEquals(1, count("income WHERE id = '${lateIncome.raw}'"))
    }

    @Test
    fun `a recent placement keeps everything the child's rules govern`() {
        db.transaction { it.insertFamily(lastPlacementEnd = today.minusYears(1)) }

        assertEquals(emptyMap(), execute(child.id).deletedRowCountsByTable)
        assertEquals(noFreezes, freezes(child.id))
    }

    @Test
    fun `a child never placed expires through the fallbacks of the rules`() {
        val byStatus = db.transaction { tx ->
            tx.insertApplicationTree()
            tx.insertChildImage(child.id)
            tx.insertDocument(tx.insert(devDocumentTemplate()), documentKey = "placement-end.pdf")
            val byStatus =
                tx.insertDocument(
                    tx.insert(
                        devDocumentTemplate(
                            basis = DocumentDeletionBasis.STATUS_TRANSITION,
                            retentionDays = 30,
                        )
                    ),
                    documentKey = "status.pdf",
                )
            tx.insert(DevGuardian(guardianId = guardian.id, childId = child.id))
            tx.insert(
                DevFamilyContact(
                    id = UUID.randomUUID(),
                    childId = child.id,
                    contactPersonId = otherGuardian.id,
                    priority = 1,
                )
            )
            byStatus
        }

        val graph = load(child.id)
        assert(graph.ownNodes.none { it.table.name == "placement" })
        val document =
            graph.ownNode(OwnNodeId.SingleRow("child_document", RowId(mapOf("id" to byStatus.raw))))
        assertEquals(
            longAgo.toLocalDate().plusDays(30),
            document.rows.single().dateByCustomSource.getValue(childDocumentRetentionEnd),
        )

        val result = execute(child.id)

        // The application expires five years after it was made and the documents ten years after
        // their last status change or the template's days after it. The image waits a year after
        // its update, the guardianship ten years after its creation and the family contact forever.
        assertEquals(
            mapOf(
                "application_note" to 1,
                "application_other_guardian" to 1,
                "placement_plan" to 1,
                "placement_draft" to 1,
                "decision" to 1,
                "application" to 1,
                "child_document_read" to 2,
                "child_document_published_version" to 2,
                "child_document" to 2,
            ),
            result.deletedRowCountsByTable,
        )
        assertEquals(
            setOf("person", "child", "child_images", "guardian", "family_contact"),
            remaining(child.id),
        )
        // The application was read by Varda, but the child was never sent there
        assertEquals(emptyList(), result.childrenFrozenForVarda)
        assertEquals(noFreezes, freezes(child.id))
    }

    @Test
    fun `a person without a child row is deleted once the parentship that names them as a child is gone`() {
        val sibling = DevPerson(dateOfBirth = LocalDate.of(2013, 1, 1))
        db.transaction { tx ->
            tx.insert(sibling, DevPersonType.RAW_ROW)
            tx.setCreatedLongAgo(sibling.id)
            tx.insert(
                DevFridgeChild(
                    childId = sibling.id,
                    headOfChild = guardian.id,
                    startDate = sibling.dateOfBirth,
                    endDate = LocalDate.of(2015, 12, 31),
                )
            )
        }
        val graph = load(sibling.id)
        assertEquals(mapOf("person" to 1, "child" to 0), graph.rows())
        assertEquals(setOf("fridge_child.child_id"), graph.foreign())

        // The parentship is the guardian's row, and it holds the sibling's person row
        assertEquals(emptyMap(), execute(sibling.id).deletedRowCountsByTable)
        // It ended long enough ago for the finance freeze, and the guardian has nothing else
        assertEquals(
            mapOf("fridge_child" to 1, "person" to 1),
            execute(guardian.id).deletedRowCountsByTable,
        )

        val result = execute(sibling.id)

        assertEquals(mapOf("person" to 1), result.deletedRowCountsByTable)
        assertEquals(emptyList(), result.childrenFrozenForKoski)
        assertEquals(emptyList(), result.childrenFrozenForVarda)
        assertEquals(false, personExists(sibling.id))
    }

    @Test
    fun `a parentship waits for the placements of the partner's children during the partnership`() {
        // The parentship is too recent for the finance freeze, and its child was never placed
        val stepchild = DevPerson(dateOfBirth = LocalDate.of(2018, 1, 1))
        val partnershipId = db.transaction { tx ->
            tx.insert(stepchild, DevPersonType.CHILD)
            tx.insert(
                DevFridgeChild(
                    childId = child.id,
                    headOfChild = guardian.id,
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2024, 12, 31),
                )
            )
            tx.execute {
                sql(
                    "UPDATE fridge_child SET created_at = ${bind(longAgo)} WHERE head_of_child = ${bind(guardian.id)}"
                )
            }
            tx.insert(
                DevFridgeChild(
                    childId = stepchild.id,
                    headOfChild = otherGuardian.id,
                    startDate = stepchild.dateOfBirth,
                    endDate = stepchild.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            tx.insert(
                DevPlacement(
                    childId = stepchild.id,
                    unitId = daycare.id,
                    startDate = LocalDate.of(2020, 8, 1),
                    endDate = LocalDate.of(2022, 6, 30),
                )
            )
            tx.insert(
                DevFridgePartnership(
                    first = guardian.id,
                    second = otherGuardian.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    createdAt = longAgo,
                )
            )
        }

        // The partner's child was placed until 2022 while the partnership and both parentships
        // overlapped
        assertEquals(null, execute(guardian.id).deletedRowCountsByTable["fridge_child"])

        // A partnership that ended before the parentship began makes the partner's child
        // irrelevant, and the parentship then expires a year after it was created
        db.transaction {
            it.execute {
                sql(
                    "UPDATE fridge_partner SET start_date = '2018-01-01', end_date = '2019-06-30' WHERE partnership_id = ${bind(partnershipId)}"
                )
            }
        }
        assertEquals(1, execute(guardian.id).deletedRowCountsByTable["fridge_child"])
    }

    @Test
    fun `a deleted decision document takes its decision row with it, and a kept one keeps its decision`() {
        db.transaction { it.insertFamily() }
        val expiredDecision = acceptedDecision()
        val keptDecision = acceptedDecision()
        db.transaction { tx ->
            tx.insertDocument(
                tx.insert(devDocumentTemplate(type = ChildDocumentType.OTHER_DECISION)),
                documentKey = "expired-decision.pdf",
                decision = expiredDecision,
            )
            val keptTemplateId =
                tx.insert(
                    devDocumentTemplate(
                        type = ChildDocumentType.OTHER_DECISION,
                        basis = DocumentDeletionBasis.STATUS_TRANSITION,
                        retentionDays = 100 * 365,
                    )
                )
            tx.insertDocument(
                keptTemplateId,
                documentKey = "kept-decision.pdf",
                decision = keptDecision,
            )
        }

        val result = execute(child.id)

        // The family's document and the expired decision document
        assertEquals(2, result.deletedRowCountsByTable["child_document"])
        assertEquals(1, result.deletedRowCountsByTable["child_document_decision"])
        val order = result.deletedRowCountsByTable.keys.toList()
        assertEquals(order.indexOf("child_document") + 1, order.indexOf("child_document_decision"))
        assertEquals(0, count("child_document_decision WHERE id = '${expiredDecision.id.raw}'"))
        assertEquals(1, count("child_document_decision WHERE id = '${keptDecision.id.raw}'"))
    }

    @Test
    fun `a document that must be archived holds its rows until it has been archived`() {
        val family = db.transaction { it.insertFamily() }
        val archivedDocumentId = db.transaction { tx ->
            val templateId = tx.insert(devDocumentTemplate(archiveExternally = true))
            tx.insertDocument(templateId, documentKey = "archived.pdf")
        }

        val first = execute(child.id)

        assertEquals(1, first.deletedRowCountsByTable["child_document"])
        assertEquals(1, first.deletedRowCountsByTable["child_document_read"])
        assertEquals(1, first.deletedRowCountsByTable["child_document_published_version"])
        assertEquals(0, count("child_document WHERE id = '${family.documentId.raw}'"))
        assertEquals(1, count("child_document WHERE id = '${archivedDocumentId.raw}'"))

        db.transaction {
            it.execute {
                sql(
                    "UPDATE child_document SET archived_at = ${bind(now)} WHERE id = ${bind(archivedDocumentId)}"
                )
            }
        }
        val second = execute(child.id)

        assertEquals(
            mapOf(
                "child_document_read" to 1,
                "child_document_published_version" to 1,
                "child_document" to 1,
            ),
            second.deletedRowCountsByTable,
        )
    }

    @Test
    fun `a voucher value decision waits for the report rows that name it, and goes once the snapshot is gone`() {
        val family = db.transaction { it.insertFamily() }
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
WITH snapshot AS (
    INSERT INTO voucher_value_report_snapshot (month, year, taken_at)
    VALUES (6, 2015, ${bind(longAgo)})
    RETURNING id
)
INSERT INTO voucher_value_report_decision (voucher_value_report_snapshot_id, decision_id, realized_amount, realized_period)
SELECT id, ${bind(family.voucherValueDecisionId)}, 100, daterange('2015-06-01', '2015-07-01', '[)')
FROM snapshot
"""
                )
            }
        }

        assertContains(load(child.id).foreign(), "voucher_value_report_decision.decision_id")
        assertEquals(null, execute(child.id).deletedRowCountsByTable["voucher_value_decision"])
        assertEquals(
            1,
            count("voucher_value_decision WHERE id = '${family.voucherValueDecisionId.raw}'"),
        )

        db.transaction { tx ->
            tx.execute { sql("DELETE FROM voucher_value_report_decision") }
            tx.execute { sql("DELETE FROM voucher_value_report_snapshot") }
        }

        assertEquals(
            mapOf("voucher_value_decision" to 1),
            execute(child.id).deletedRowCountsByTable,
        )
    }

    @Test
    fun `a fee decision waits for the placements of the children on it`() {
        db.transaction { it.insertFamily() }
        val latePlacement = db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = LocalDate.of(2019, 8, 1),
                    endDate = LocalDate.of(2020, 6, 30),
                )
            )
        }

        val kept = execute(guardian.id).deletedRowCountsByTable
        assertEquals(null, kept["fee_decision"])
        assertEquals(null, kept["fee_decision_child"])

        db.transaction { tx ->
            tx.execute { sql("DELETE FROM placement WHERE id = ${bind(latePlacement)}") }
        }

        assertEquals(
            mapOf("fee_decision_child" to 1, "fee_decision" to 1),
            execute(guardian.id).deletedRowCountsByTable,
        )
    }

    @Test
    fun `a person that is a duplicate or has duplicates is refused`() {
        val duplicate = DevPerson(duplicateOf = child.id)
        db.transaction { tx -> tx.insert(duplicate, DevPersonType.CHILD) }
        for (id in listOf(child.id, duplicate.id)) {
            val error = assertFailsWith<IllegalStateException> { load(id) }
            assertContains(error.message!!, "is a duplicate")
        }
    }

    @Test
    fun `the loaded graph carries the integration facts of the target and of every child the own rows name`() {
        db.transaction { it.insertFamily() }
        val childFacts =
            ChildIntegrationFacts(child.dateOfBirth, sentToKoski = true, sentToVarda = true)
        assertEquals(
            mapOf(
                guardian.id to
                    ChildIntegrationFacts(
                        guardian.dateOfBirth,
                        sentToKoski = false,
                        sentToVarda = false,
                    ),
                child.id to childFacts,
            ),
            load(guardian.id).integrationFactsByChild,
        )
        assertEquals(mapOf(child.id to childFacts), load(child.id).integrationFactsByChild)
    }

    @Test
    fun `a child sent to Varda keeps the rows Varda read until the safe age, and a child never sent loses them without a freeze`() {
        val young = DevPerson(dateOfBirth = today.minusYears(7))
        db.transaction { tx ->
            tx.insert(young, DevPersonType.CHILD)
            val applicationId =
                tx.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = guardian.id,
                    childId = young.id,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child = ApplicationFormChild(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycare.id)),
                        ),
                )
            tx.execute {
                sql(
                    "UPDATE application SET created_at = ${bind(longAgo)} WHERE id = ${bind(applicationId)}"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO varda_state (child_id, state, last_success_at) VALUES (${bind(young.id)}, NULL, now())"
                )
            }
        }
        assertEquals(true, load(young.id).integrationFactsByChild.getValue(young.id).sentToVarda)
        // The application expired five years after it was made, but Varda has read it
        assertEquals(emptyMap(), execute(young.id).deletedRowCountsByTable)
        assertEquals(noFreezes, freezes(young.id))

        db.transaction { tx ->
            tx.execute { sql("DELETE FROM varda_state WHERE child_id = ${bind(young.id)}") }
        }
        val result = execute(young.id)
        assertEquals(mapOf("application" to 1), result.deletedRowCountsByTable)
        assertEquals(emptyList(), result.childrenFrozenForVarda)
        assertEquals(noFreezes, freezes(young.id))
    }

    @Test
    fun `an income statement draft expires a year after it was created, and a sent one ten years after its period, which lasts a year when it has no end`() {
        fun statement(
            status: IncomeStatementStatus,
            createdAt: HelsinkiDateTime,
            start: LocalDate,
        ) =
            DevIncomeStatement(
                personId = guardian.id,
                data = IncomeStatementBody.HighestFee(start, null),
                status = status,
                sentAt = createdAt.takeIf { status != IncomeStatementStatus.DRAFT },
                createdAt = createdAt,
                modifiedAt = createdAt,
            )
        val expiredDraft =
            statement(
                IncomeStatementStatus.DRAFT,
                HelsinkiDateTime.of(today.minusYears(1).minusDays(1), LocalTime.NOON),
                start = today,
            )
        val recentDraft =
            statement(
                IncomeStatementStatus.DRAFT,
                HelsinkiDateTime.of(today.minusYears(1), LocalTime.NOON),
                start = today,
            )
        val expiredSent =
            statement(
                IncomeStatementStatus.SENT,
                longAgo,
                start = today.minusYears(11).minusDays(1),
            )
        val recentSent =
            statement(IncomeStatementStatus.SENT, longAgo, start = today.minusYears(11))
        db.transaction { tx ->
            for (statement in listOf(expiredDraft, recentDraft, expiredSent, recentSent)) {
                tx.insert(statement)
            }
        }

        assertEquals(mapOf("income_statement" to 2), execute(guardian.id).deletedRowCountsByTable)
        assertEquals(
            setOf(recentDraft.id, recentSent.id),
            db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT id FROM income_statement WHERE person_id = ${bind(guardian.id)}"
                        )
                    }
                    .toSet<IncomeStatementId>()
            },
        )
    }

    private fun Database.Transaction.setCreatedLongAgo(vararg ids: PersonId) = execute {
        sql("UPDATE person SET created = ${bind(longAgo)} WHERE id = ANY(${bind(ids.toList())})")
    }

    private data class Family(
        val lastPlacementEnd: LocalDate,
        val applicationId: ApplicationId,
        val placementIds: List<PlacementId>,
        val documentId: ChildDocumentId,
        val imageId: ChildImageId,
        val partnershipId: PartnershipId,
        val voucherValueDecisionId: VoucherValueDecisionId,
    )

    private val retentionDays = 3650

    private val templateContent =
        DocumentTemplateContent(
            listOf(
                Section(
                    id = "s1",
                    label = "s1",
                    questions = listOf(Question.TextQuestion(id = "q1", label = "q1")),
                )
            )
        )
    private val documentContent =
        DocumentContent(
            answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "answer"))
        )

    private fun devDocumentTemplate(
        type: ChildDocumentType = ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
        archiveExternally: Boolean = false,
        basis: DocumentDeletionBasis = DocumentDeletionBasis.PLACEMENT_END,
        retentionDays: Int = this.retentionDays,
    ) =
        DevDocumentTemplate(
            type = type,
            validity = DateRange(LocalDate.of(2012, 1, 1), null),
            content = templateContent,
            archiveExternally = archiveExternally,
            processDefinitionNumber = "12.06.01".takeIf { archiveExternally },
            archiveDurationMonths = 120.takeIf { archiveExternally },
            deletionRetentionBasis = basis,
            deletionRetentionDays = retentionDays,
            endDecisionWhenUnitChanges = false.takeIf { type == ChildDocumentType.OTHER_DECISION },
        )

    private fun acceptedDecision() =
        DevChildDocumentDecision(
            createdBy = employee.id,
            modifiedBy = employee.id,
            status = ChildDocumentDecisionStatus.ACCEPTED,
            validity = DateRange(longAgo.toLocalDate(), null),
            daycareId = daycare.id,
        )

    /**
     * A completed document with a published PDF, read by the guardian, long ago. A decision
     * document has a decision made by the employee.
     */
    private fun Database.Transaction.insertDocument(
        templateId: DocumentTemplateId,
        documentKey: String,
        decision: DevChildDocumentDecision? = null,
    ): ChildDocumentId {
        val documentId =
            insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    childId = child.id,
                    templateId = templateId,
                    content = documentContent,
                    modifiedAt = longAgo,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = longAgo,
                    contentLockedBy = employee.id,
                    decisionMaker = employee.id.takeIf { decision != null },
                    decision = decision,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = longAgo,
                                createdBy = employee.evakaUserId,
                                publishedContent = documentContent,
                                documentKey = documentKey,
                            )
                        ),
                )
            )
        execute {
            sql(
                "INSERT INTO child_document_read (document_id, person_id) VALUES (${bind(documentId)}, ${bind(guardian.id)})"
            )
        }
        return documentId
    }

    /**
     * An application made by the guardian in 2012 with the other guardian on it, with a note, a
     * placement plan, a placement draft and a decision with PDFs for both guardians
     */
    private fun Database.Transaction.insertApplicationTree(): ApplicationId {
        val applicationId =
            insertTestApplication(
                type = ApplicationType.DAYCARE,
                guardianId = guardian.id,
                childId = child.id,
                otherGuardians = setOf(otherGuardian.id),
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = ApplicationFormChild(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                    ),
                modifiedAt = HelsinkiDateTime.of(LocalDate.of(2012, 5, 1), LocalTime.NOON),
            )
        execute {
            sql(
                """
INSERT INTO application_note (application_id, content, created_by, modified_by, modified_at)
VALUES (${bind(applicationId)}, 'note', ${bind(employee.evakaUserId)}, ${bind(employee.evakaUserId)}, ${bind(longAgo)})
"""
            )
        }
        insert(DevPlacementPlan(applicationId = applicationId, unitId = daycare.id))
        insert(
            DevPlacementDraft(
                applicationId = applicationId,
                unitId = daycare.id,
                startDate = LocalDate.of(2012, 8, 1),
                createdBy = employee.evakaUserId,
                modifiedBy = employee.evakaUserId,
            )
        )
        val decisionId =
            insertTestDecision(
                TestDecision(
                    createdBy = employee.evakaUserId,
                    unitId = daycare.id,
                    applicationId = applicationId,
                    type = DecisionType.DAYCARE,
                    startDate = LocalDate.of(2012, 8, 1),
                    endDate = LocalDate.of(2013, 7, 31),
                    documentKey = "decision.pdf",
                )
            )
        execute {
            sql(
                "UPDATE decision SET other_guardian_document_key = 'decision-other-guardian.pdf' WHERE id = ${bind(decisionId)}"
            )
        }
        return applicationId
    }

    /**
     * The child's two placements, the last of them from the application, and the family's data
     * around them: the integration state, an image, a document, guardianships, a family contact, a
     * parentship, a partnership, a fee decision, incomes, an income statement, and a message thread
     * about the application
     */
    private fun Database.Transaction.insertFamily(
        lastPlacementEnd: LocalDate = LocalDate.of(2015, 6, 30)
    ): Family {
        val applicationId = insertApplicationTree()
        val placementIds =
            listOf(
                insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = LocalDate.of(2012, 8, 1),
                        endDate = LocalDate.of(2013, 7, 31),
                    )
                ),
                insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = lastPlacementEnd.minusYears(1),
                        endDate = lastPlacementEnd,
                        source = PlacementSource.APPLICATION,
                        sourceApplicationId = applicationId,
                    )
                ),
            )
        insert(
            DevServiceNeed(
                placementId = placementIds.first(),
                startDate = LocalDate.of(2012, 8, 1),
                endDate = LocalDate.of(2013, 7, 31),
                optionId = snDefaultDaycare.id,
                confirmedBy = employee.evakaUserId,
            )
        )
        insert(
            DevDaycareGroupPlacement(
                daycarePlacementId = placementIds.last(),
                daycareGroupId = group.id,
                startDate = lastPlacementEnd.minusYears(1),
                endDate = lastPlacementEnd,
            )
        )
        insert(
            DevBackupCare(
                childId = child.id,
                unitId = daycare.id,
                period = FiniteDateRange(lastPlacementEnd.minusMonths(1), lastPlacementEnd),
            )
        )
        execute {
            sql(
                """
INSERT INTO koski_study_right (child_id, unit_id, type, payload, version, data_version)
VALUES (${bind(child.id)}, ${bind(daycare.id)}, 'PRESCHOOL', '{}', 0, 1)
"""
            )
        }
        execute {
            sql(
                """
INSERT INTO koski_upload_error (child_id, unit_id, type, error, status_code, errored_at, errored_since)
VALUES (${bind(child.id)}, ${bind(daycare.id)}, 'PRESCHOOL', 'failed', 500, now(), now())
"""
            )
        }
        execute {
            sql(
                "INSERT INTO varda_state (child_id, state, last_success_at) VALUES (${bind(child.id)}, NULL, now())"
            )
        }
        val imageId = insertChildImage(child.id)
        val documentId = insertDocument(insert(devDocumentTemplate()), documentKey = "document.pdf")
        insert(DevGuardian(guardianId = guardian.id, childId = child.id))
        insert(DevGuardian(guardianId = otherGuardian.id, childId = child.id))
        insert(
            DevFamilyContact(
                id = UUID.randomUUID(),
                childId = child.id,
                contactPersonId = otherGuardian.id,
                priority = 1,
            )
        )
        insert(
            DevFridgeChild(
                childId = child.id,
                headOfChild = guardian.id,
                startDate = child.dateOfBirth,
                endDate = child.dateOfBirth.plusYears(18).minusDays(1),
            )
        )
        execute {
            sql(
                "UPDATE fridge_child SET create_source = 'APPLICATION', created_by_application = ${bind(applicationId)} WHERE child_id = ${bind(child.id)}"
            )
        }
        val partnershipId =
            insert(
                DevFridgePartnership(
                    first = guardian.id,
                    second = otherGuardian.id,
                    startDate = LocalDate.of(2012, 1, 1),
                    endDate = LocalDate.of(2015, 12, 31),
                    createdAt = longAgo,
                )
            )
        execute {
            sql(
                "UPDATE fridge_partner SET create_source = 'APPLICATION', created_from_application = ${bind(applicationId)} WHERE partnership_id = ${bind(partnershipId)}"
            )
        }
        val feeDecisionId =
            insert(
                DevFeeDecision(
                    validDuring =
                        FiniteDateRange(LocalDate.of(2013, 1, 1), LocalDate.of(2013, 12, 31)),
                    headOfFamilyId = guardian.id,
                    partnerId = otherGuardian.id,
                    documentKey = "fee-decision.pdf",
                )
            )
        insert(
            DevFeeDecisionChild(
                feeDecisionId = feeDecisionId,
                childId = child.id,
                placementUnitId = daycare.id,
            )
        )
        val voucherValueDecisionId =
            insert(
                DevVoucherValueDecision(
                    validFrom = lastPlacementEnd.minusYears(1),
                    validTo = lastPlacementEnd,
                    headOfFamilyId = guardian.id,
                    partnerId = otherGuardian.id,
                    childId = child.id,
                    childDateOfBirth = child.dateOfBirth,
                    placementUnitId = daycare.id,
                    documentKey = "value-decision.pdf",
                )
            )
        val incomeId =
            insert(
                DevIncome(
                    personId = guardian.id,
                    validFrom = LocalDate.of(2013, 1, 1),
                    validTo = LocalDate.of(2013, 12, 31),
                    modifiedBy = employee.evakaUserId,
                )
            )
        execute {
            sql(
                "UPDATE income SET application_id = ${bind(applicationId)} WHERE id = ${bind(incomeId)}"
            )
        }
        insert(
            DevIncome(
                personId = guardian.id,
                validFrom = LocalDate.of(2024, 1, 1),
                validTo = null,
                modifiedBy = employee.evakaUserId,
            )
        )
        insert(
            DevIncomeStatement(
                personId = guardian.id,
                data =
                    IncomeStatementBody.HighestFee(
                        LocalDate.of(2013, 1, 1),
                        LocalDate.of(2013, 12, 31),
                    ),
                createdAt = longAgo,
                modifiedAt = longAgo,
            )
        )
        createPersonMessageAccount(guardian.id)
        execute {
            sql(
                """
INSERT INTO message_thread (message_type, title, urgent, is_copy, application_id, sensitive)
VALUES ('MESSAGE', 'About the application', false, false, ${bind(applicationId)}, false)
"""
            )
        }
        return Family(
            lastPlacementEnd,
            applicationId,
            placementIds,
            documentId,
            imageId,
            partnershipId,
            voucherValueDecisionId,
        )
    }
}
