// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.DateSource.GraphTableColumn
import evaka.core.dataremoval.DateSource.OwnColumn
import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.AllOf
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.AnyOf
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.ExpirationRule.Never
import evaka.core.dataremoval.ExpirationRule.SafeForIntegrations.Companion.safeFor
import evaka.core.dataremoval.Handler.ADULT
import evaka.core.dataremoval.Handler.CHILD
import evaka.core.dataremoval.Integration.KOSKI
import evaka.core.dataremoval.Integration.VARDA
import evaka.core.shared.ChildId
import evaka.core.shared.async.AsyncJob
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PersonGraphEvaluationTest {
    private val oneYear = Period.ofYears(1)
    private val endDate = OwnColumn("end_date")
    private val target = targetId.raw
    private val person = OwnNodeId.WholeTable("person")
    private val child = OwnNodeId.WholeTable("child")

    private fun whole(table: String) = OwnNodeId.WholeTable(table)

    private fun single(table: String, id: UUID) =
        OwnNodeId.SingleRow(table, RowIdentity(mapOf("id" to id)))

    /** A schema with person, child and a child-handled table "a" with the rule */
    private fun schemaWith(rule: ExpirationRule, vararg others: Table) =
        schema(table("a", CHILD, "child_id" to "child", expirationRule = rule), *others)

    /**
     * Whether the node of table "a" with the given rows is deleted in a graph of person, child and
     * "a"
     */
    private fun expired(
        rule: ExpirationRule,
        vararg rows: OwnRow = arrayOf(row("child_id" to target)),
    ): Boolean {
        val schema = schemaWith(rule)
        val plan = schema.graph(schema.ownNode("a", *rows)).evaluate(today)
        return whole("a") in plan.deleted()
    }

    private fun endedAt(date: LocalDate?) =
        row("child_id" to target, dateByColumn = mapOf("end_date" to date))

    @Test
    fun `Always expires and Never does not`() {
        assert(expired(Always))
        assert(!expired(Never))
    }

    @Test
    fun `After expires the day after the period has passed since the date`() {
        val rule = After(oneYear, endDate)
        assert(expired(rule, endedAt(today.minusYears(1).minusDays(1))))
        assert(!expired(rule, endedAt(today.minusYears(1))))
        assert(!expired(rule, endedAt(today)))
    }

    @Test
    fun `a node evaluated as a whole takes the latest date of its rows, or none when a row has none`() {
        val rule = Coalesce(After(oneYear, endDate), Always)
        assert(!expired(rule, endedAt(today.minusYears(5)), endedAt(today)))
        assert(expired(rule, endedAt(today.minusYears(5)), endedAt(today.minusYears(2))))
        assert(expired(rule, endedAt(today), endedAt(null)))
        assert(!expired(Coalesce(After(oneYear, endDate), Never), endedAt(today), endedAt(null)))
    }

    @Test
    fun `a column of another table gives the latest date over all of that table's rows, or none without rows`() {
        val rule = Coalesce(After(oneYear, GraphTableColumn("placement", "end_date")), Never)
        val schema = schemaWith(rule, table("placement", CHILD, "child_id" to "child"))
        fun placed(vararg ends: LocalDate) =
            schema.ownNode("placement", *ends.map { endedAt(it) }.toTypedArray())
        fun deleted(vararg placements: OwnNode) =
            whole("a") in
                schema
                    .graph(schema.ownNode("a", row("child_id" to target)), *placements)
                    .evaluate(today)
                    .deleted()
        assert(deleted(placed(today.minusYears(5), today.minusYears(2))))
        assert(!deleted(placed(today.minusYears(5), today.minusMonths(6))))
        assert(!deleted())
    }

    @Test
    fun `a custom date source gives the latest of the dates of the rows, or none when a row has none`() {
        val source = customSource()
        val rule = Coalesce(After(oneYear, source), Never)
        fun dated(date: LocalDate?) =
            row("child_id" to target, dateByCustomSource = mapOf(source to date))
        assert(expired(rule, dated(today.minusYears(5)), dated(today.minusYears(2))))
        assert(!expired(rule, dated(today.minusYears(5)), dated(today)))
        assert(!expired(rule, dated(today.minusYears(5)), dated(null)))
    }

    @Test
    fun `AllOf needs every rule, AnyOf needs one and Coalesce takes the first result`() {
        assert(!expired(AllOf(Always, Never)))
        assert(expired(AllOf(Always, Always)))
        assert(expired(AnyOf(Always, Never)))
        assert(!expired(AnyOf(Never, Never)))
        assert(expired(Coalesce(After(oneYear, endDate), Always), endedAt(null)))
        assert(!expired(Coalesce(After(oneYear, endDate), Always), endedAt(today)))
    }

    @Test
    fun `a date the loader did not read is an error naming the node`() {
        val error = assertFailsWith<IllegalStateException> { expired(After(oneYear, endDate)) }
        assertContains(error.message!!, "a: the loader did not read column end_date")
        val source = customSource()
        val custom =
            assertFailsWith<IllegalStateException> {
                expired(Coalesce(After(oneYear, source), Never))
            }
        assertContains(custom.message!!, "a: the loader did not read custom date source")
    }

    @Test
    fun `ArchivedIfRequired holds the node until every row that must be archived is`() {
        val archived = archivedIfRequiredRule()
        fun row(mayExpire: Boolean) =
            row("child_id" to target, mayExpireByArchivedRule = mapOf(archived to mayExpire))
        assert(!expired(archived, row(true), row(false)))
        assert(expired(archived, row(true), row(true)))
    }

    @Test
    fun `the person node is part of every graph`() {
        val schema = schema()
        val error =
            assertFailsWith<IllegalArgumentException> {
                PersonGraph(
                    schema,
                    targetId,
                    listOf(schema.ownNode("child")),
                    emptyList(),
                    mapOf(targetChildId to neverSent),
                )
            }
        assertContains(error.message!!, "person node")
    }

    private val blockingSchema =
        schema(
            table("a", CHILD, "child_id" to "child"),
            table("b", CHILD, "a_id" to "a", expirationRule = Never),
            table("c", CHILD, "child_id" to "child"),
        )

    @Test
    fun `a node that is not expired blocks the nodes it references, transitively, but not unrelated nodes`() {
        val a = UUID.randomUUID()
        val plan =
            blockingSchema
                .graph(
                    blockingSchema.ownNode("a", row("child_id" to target, id = a)),
                    blockingSchema.ownNode("b", row("a_id" to a)),
                    blockingSchema.ownNode("c", row("child_id" to target)),
                )
                .evaluate(today)
        assertEquals(listOf(whole("c")), plan.deleted())
    }

    @Test
    fun `a foreign node blocks the own nodes its rows reference`() {
        // Another person's fridge_child rows reference the target's child row
        val schema =
            schema(
                table("c", CHILD, "child_id" to "child"),
                table("fridge_child", ADULT, "head_of_child" to "person", "child_id" to "child"),
            )
        val plan =
            schema
                .graph(
                    schema.ownNode("c", row("child_id" to target)),
                    foreignNodes = listOf(schema.foreign("fridge_child", "child_id", target)),
                )
                .evaluate(today)
        assertEquals(listOf(whole("c")), plan.deleted())
    }

    @Test
    fun `a foreign node blocks only the rows it references of a table with independent rows`() {
        val schema =
            schema(
                table("a", CHILD, "child_id" to "child", independentRows = true),
                externalTable("b", "a_id" to "a"),
            )
        val referenced = UUID.randomUUID()
        val other = UUID.randomUUID()
        val plan =
            schema
                .graph(
                    schema.ownRowNode("a", row("child_id" to target, id = referenced)),
                    schema.ownRowNode("a", row("child_id" to target, id = other)),
                    foreignNodes = listOf(schema.foreign("b", "a_id", referenced)),
                )
                .evaluate(today)
        assertEquals(listOf(single("a", other)), plan.deleted())
    }

    @Test
    fun `a missing child row counts as expired and blocks nothing`() {
        val plan =
            blockingSchema
                .graph(blockingSchema.ownNode("c", row("child_id" to target)), childRow = false)
                .evaluate(today)
        assertEquals(listOf(whole("c"), person), plan.deleted())
    }

    @Test
    fun `a reference to a row of another person is not an edge`() {
        val schema =
            schema(table("application", CHILD, "child_id" to "child", "guardian_id" to "person"))
        val plan =
            schema
                .graph(
                    schema.ownNode(
                        "application",
                        row("child_id" to target, "guardian_id" to UUID.randomUUID()),
                    )
                )
                .evaluate(today)
        assertEquals(listOf(whole("application"), child, person), plan.deleted())
    }

    @Test
    fun `bundled nodes go together or not at all`() {
        val schema =
            schema(
                table("a", CHILD, "child_id" to "child", expirationRule = Never),
                table("b", CHILD, "a_id" to "a", bundledBy = "a"),
                table("c", CHILD, "b_id" to "b"),
            )
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val plan =
            schema
                .graph(
                    schema.ownNode("a", row("child_id" to target, id = a)),
                    schema.ownNode("b", row("a_id" to a, id = b)),
                    schema.ownNode("c", row("b_id" to b)),
                )
                .evaluate(today)
        // a is not expired, so it holds b through the bundle; c only references b and goes
        assertEquals(listOf(whole("c")), plan.deleted())
    }

    @Test
    fun `a row bundles only the rows whose primary path leads to it`() {
        val schema =
            schema(
                table("a", CHILD, "child_id" to "child", independentRows = true),
                table("b", CHILD, "a_id" to "a", independentRows = true),
                table(
                    "c",
                    CHILD,
                    "b_id" to "b",
                    independentRows = true,
                    bundledBy = "a",
                    expirationRule = After(oneYear, endDate),
                ),
            )
        val a1 = UUID.randomUUID()
        val a2 = UUID.randomUUID()
        val b1 = UUID.randomUUID()
        val b2 = UUID.randomUUID()
        val c1 = row("b_id" to b1, dateByColumn = mapOf("end_date" to today.minusYears(2)))
        val c2 = row("b_id" to b2, dateByColumn = mapOf("end_date" to today))
        val graph =
            schema.graph(
                schema.ownRowNode("a", row("child_id" to target, id = a1)),
                schema.ownRowNode("a", row("child_id" to target, id = a2)),
                schema.ownRowNode("b", row("a_id" to a1, id = b1)),
                schema.ownRowNode("b", row("a_id" to a2, id = b2)),
                schema.ownRowNode("c", c1),
                schema.ownRowNode("c", c2),
            )
        assertEquals(
            mapOf<OwnNodeId, List<OwnNodeId>>(
                single("a", a1) to listOf(OwnNodeId.SingleRow("c", c1.id)),
                single("a", a2) to listOf(OwnNodeId.SingleRow("c", c2.id)),
            ),
            graph.ownNodeIdsByBundler,
        )
        // The second c row is not expired: it holds b2, which holds a2, which bundles nothing else
        assertEquals(
            listOf(OwnNodeId.SingleRow("c", c1.id), single("b", b1), single("a", a1)),
            graph.evaluate(today).deleted(),
        )
    }

    @Test
    fun `nodes are deleted leaf-first`() {
        val schema =
            schema(
                table("a", CHILD, "child_id" to "child"),
                table("b", CHILD, "a_id" to "a"),
                table("c", CHILD, "child_id" to "child", "b_id" to "b"),
            )
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val plan =
            schema
                .graph(
                    schema.ownNode("a", row("child_id" to target, id = a)),
                    schema.ownNode("b", row("a_id" to a, id = b)),
                    schema.ownNode("c", row("child_id" to target, "b_id" to b)),
                )
                .evaluate(today)
        assertEquals(listOf(whole("c"), whole("b"), whole("a"), child, person), plan.deleted())
    }

    @Test
    fun `the plan clears the optional references into a deleted node, with the ids of its rows`() {
        val schema =
            schema(
                table("application", CHILD, "child_id" to "child"),
                table(
                    "placement",
                    CHILD,
                    "child_id" to "child",
                    optional =
                        listOf(
                            optionalReference(
                                "source_application_id",
                                referencedTable = "application",
                                alsoNull = listOf("source"),
                            )
                        ),
                    expirationRule = Never,
                ),
            )
        val application = UUID.randomUUID()
        val plan =
            schema
                .graph(
                    schema.ownNode("application", row("child_id" to target, id = application)),
                    schema.ownNode("placement", row("child_id" to target)),
                )
                .evaluate(today)
        assertEquals(listOf(whole("application")), plan.deleted())
        assertEquals(
            listOf(
                ReferenceClearing(
                    "placement",
                    "source_application_id",
                    listOf("source"),
                    listOf(application),
                )
            ),
            plan.ownNodeDeletions.single().referenceClearings,
        )
    }

    /** The tables Koski and Varda read, with a fee decision naming other children */
    private fun integrationSchema(childRule: ExpirationRule = Always) =
        schema(
            table("placement", CHILD, "child_id" to "child"),
            table("fee_decision", ADULT, "head_of_family_id" to "person"),
            table(
                "fee_decision_child",
                ADULT,
                "fee_decision_id" to "fee_decision",
                "child_id" to "child",
                bundledBy = "fee_decision",
            ),
            child = childTable(childRule),
        )

    private val otherChild = ChildId(UUID.randomUUID())
    private val neverSentChild = ChildId(UUID.randomUUID())
    private val sentToBoth =
        ChildIntegrationFacts(today.minusYears(5), sentToKoski = true, sentToVarda = true)
    private val sentToVarda =
        ChildIntegrationFacts(today.minusYears(5), sentToKoski = false, sentToVarda = true)

    private fun SchemaDefinition.integrationGraph(
        integrationFactsByChild: Map<ChildId, ChildIntegrationFacts>
    ): PersonGraph {
        val decision = UUID.randomUUID()
        return graph(
            ownNode("placement", row("child_id" to target)),
            ownNode("fee_decision", row("head_of_family_id" to target, id = decision)),
            ownNode(
                "fee_decision_child",
                row("fee_decision_id" to decision, "child_id" to otherChild.raw),
                row("fee_decision_id" to decision, "child_id" to neverSentChild.raw),
            ),
            integrationFactsByChild = integrationFactsByChild,
        )
    }

    @Test
    fun `an integration is frozen for the target of a child-handled table and for the children an adult-handled table names, when sent to it`() {
        val integrationFactsByChild =
            mapOf(
                targetChildId to sentToBoth,
                otherChild to sentToVarda,
                neverSentChild to neverSent,
            )
        val childStays =
            integrationSchema(childRule = Never)
                .integrationGraph(integrationFactsByChild)
                .evaluate(today)
        assertEquals(setOf(targetChildId), childStays.childrenToFreezeForKoski)
        assertEquals(setOf(targetChildId, otherChild), childStays.childrenToFreezeForVarda)

        // The child row goes with the plan, so there is nothing to freeze for the target
        val schema = integrationSchema()
        val childGoes = schema.integrationGraph(integrationFactsByChild).evaluate(today)
        assertEquals(emptySet(), childGoes.childrenToFreezeForKoski)
        assertEquals(setOf(otherChild), childGoes.childrenToFreezeForVarda)
        assertEquals(schema.handledTables.size, childGoes.ownNodeDeletions.size)
    }

    @Test
    fun `a rule safe for an integration expires only when every child concerned is never sent to it or past the safe age`() {
        val past = today.minusYears(SAFE_DATA_REMOVAL_AGE).minusDays(1)
        val sentToKoskiOld = ChildIntegrationFacts(past, sentToKoski = true, sentToVarda = false)
        val sentToKoskiYoung =
            ChildIntegrationFacts(past.plusDays(1), sentToKoski = true, sentToVarda = false)
        fun expired(
            rule: ExpirationRule,
            facts: ChildIntegrationFacts,
            row: OwnRow = row("child_id" to target),
        ): Boolean {
            val schema = schemaWith(rule)
            val graph =
                schema.graph(
                    schema.ownNode("a", row),
                    integrationFactsByChild = mapOf(targetChildId to facts),
                )
            return whole("a") in graph.evaluate(today).deleted()
        }
        assert(!expired(Never.safeFor(KOSKI), neverSent))
        assert(expired(Always.safeFor(KOSKI), neverSent))
        assert(!expired(Always.safeFor(KOSKI), sentToKoskiYoung))
        assert(expired(Always.safeFor(VARDA), sentToKoskiYoung))
        assert(!expired(Always.safeFor(KOSKI, VARDA), sentToKoskiYoung))
        assert(expired(Always.safeFor(KOSKI), sentToKoskiOld))
        // No result from the wrapped rule stays no result, so the fallback decides
        val noDate = Coalesce(After(oneYear, endDate).safeFor(KOSKI), Always)
        assert(expired(noDate, sentToKoskiYoung, endedAt(null)))
        assert(!expired(noDate, sentToKoskiYoung, endedAt(today.minusYears(2))))
    }

    @Test
    fun `a rule safe for an integration concerns the children an adult-handled table names directly and through the nodes it bundles`() {
        val schema =
            schema(
                table(
                    "fee_decision",
                    ADULT,
                    "head_of_family_id" to "person",
                    expirationRule = Always.safeFor(VARDA),
                ),
                table(
                    "fee_decision_child",
                    ADULT,
                    "fee_decision_id" to "fee_decision",
                    "child_id" to "child",
                    bundledBy = "fee_decision",
                ),
            )
        val decision = UUID.randomUUID()
        fun deleted(facts: Map<ChildId, ChildIntegrationFacts>) =
            schema
                .graph(
                    schema.ownNode(
                        "fee_decision",
                        row("head_of_family_id" to target, id = decision),
                    ),
                    schema.ownNode(
                        "fee_decision_child",
                        row("fee_decision_id" to decision, "child_id" to otherChild.raw),
                    ),
                    childRow = false,
                    integrationFactsByChild = facts,
                )
                .evaluate(today)
                .deleted()
        assert(
            whole("fee_decision") in
                deleted(mapOf(targetChildId to neverSent, otherChild to neverSent))
        )
        assert(
            whole("fee_decision") !in
                deleted(mapOf(targetChildId to neverSent, otherChild to sentToVarda))
        )
        val error =
            assertFailsWith<IllegalStateException> { deleted(mapOf(targetChildId to neverSent)) }
        assertContains(
            error.message!!,
            "fee_decision: the loader did not read the integration facts of child $otherChild",
        )
    }

    @Test
    fun `the plan queues the async jobs of the deleted rows`() {
        val schema =
            schema(
                table(
                    "decision",
                    CHILD,
                    "child_id" to "child",
                    asyncJobsPlannedOnDelete =
                        AsyncJobsOnDelete(listOf("document_key")) { row ->
                            listOfNotNull(row.valuesByColumn["document_key"]).map {
                                AsyncJob.DeleteDecisionPdf(it)
                            }
                        },
                )
            )
        val plan =
            schema
                .graph(
                    schema.ownNode(
                        "decision",
                        row(
                            "child_id" to target,
                            valueForJobByColumn = mapOf("document_key" to "key-1"),
                        ),
                        row(
                            "child_id" to target,
                            valueForJobByColumn = mapOf("document_key" to null),
                        ),
                    )
                )
                .evaluate(today)
        assertEquals(listOf(AsyncJob.DeleteDecisionPdf("key-1")), plan.asyncJobs)
    }

    @Test
    fun `the plan deletes the orphans the deleted rows reference by the declared column, and counts them as deleted rows`() {
        val schema =
            schema(
                table(
                    "document",
                    CHILD,
                    "child_id" to "child",
                    orphansToDelete =
                        listOf(
                            OutsideGraphReference("decision_id", "decision"),
                            OutsideGraphReference("process_id", "process", "key"),
                        ),
                )
            )
        val decisionId = UUID.randomUUID()
        val processId = UUID.randomUUID()
        val plan =
            schema
                .graph(
                    schema.ownNode(
                        "document",
                        row(
                            "child_id" to target,
                            outsideGraphReferences =
                                mapOf("decision_id" to decisionId, "process_id" to processId),
                        ),
                        row(
                            "child_id" to target,
                            outsideGraphReferences =
                                mapOf("decision_id" to null, "process_id" to null),
                        ),
                    )
                )
                .evaluate(today)
        val deletion = plan.ownNodeDeletions.single { it.node.id == whole("document") }
        assertEquals(
            listOf(
                OrphanDeletion("decision", "id", listOf(decisionId)),
                OrphanDeletion("process", "key", listOf(processId)),
            ),
            deletion.orphanDeletions,
        )
        assertEquals(
            mapOf("document" to 2, "decision" to 1, "process" to 1, "child" to 1, "person" to 1),
            plan.rowCountsByTable(),
        )
    }

    @Test
    fun `independent rows expire and are deleted on their own`() {
        val schema =
            schema(
                table(
                    "income",
                    ADULT,
                    "person_id" to "person",
                    independentRows = true,
                    expirationRule = After(oneYear, endDate),
                )
            )
        val old =
            row("person_id" to target, dateByColumn = mapOf("end_date" to today.minusYears(2)))
        val recent = row("person_id" to target, dateByColumn = mapOf("end_date" to today))
        val plan =
            schema
                .graph(
                    schema.ownRowNode("income", old),
                    schema.ownRowNode("income", recent),
                    childRow = false,
                )
                .evaluate(today)
        assertEquals(listOf(OwnNodeId.SingleRow("income", old.id)), plan.deleted())
        assertEquals(mapOf("income" to 1), plan.rowCountsByTable())
    }
}
