// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.DateColumnType.DATE
import evaka.core.dataremoval.DateColumnType.TIMESTAMP_WITH_TIME_ZONE
import evaka.core.dataremoval.DateSource.GraphTableColumn
import evaka.core.dataremoval.DateSource.OwnColumn
import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.AllOf
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.AnyOf
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.ExpirationRule.Never
import evaka.core.dataremoval.ExpirationRule.SafeForIntegrations
import evaka.core.dataremoval.ExpirationRule.SafeForIntegrations.Companion.safeFor
import evaka.core.dataremoval.Handler.ADULT
import evaka.core.dataremoval.Handler.CHILD
import evaka.core.dataremoval.Integration.KOSKI
import evaka.core.dataremoval.Integration.VARDA
import evaka.core.koski.KOSKI_INPUT_TABLES
import evaka.core.varda.VARDA_INPUT_TABLES
import java.time.Period
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SchemaDefinitionTest {
    private fun invalid(message: String, f: () -> Any) {
        val error = assertFailsWith<IllegalArgumentException> { f() }
        assertContains(error.message!!, message)
    }

    /** Construction is the validation, so a valid definition simply constructs */
    private fun valid(f: () -> Any) {
        val _ = f()
    }

    @Test
    fun `the primary references form a root-first tree`() {
        val schema =
            schema(
                table("placement", CHILD, "child_id" to "child"),
                table("service_need", CHILD, "placement_id" to "placement"),
                table("application", CHILD, "child_id" to "child", "guardian_id" to "person"),
                table(
                    "fee_decision",
                    ADULT,
                    "head_of_family_id" to "person",
                    "partner_id" to "person",
                ),
                table(
                    "fee_decision_child",
                    ADULT,
                    "fee_decision_id" to "fee_decision",
                    "child_id" to "child",
                ),
                externalTable("message_account", "person_id" to "person"),
            )
        val primary = { name: String ->
            schema.primaryReference(schema.handled(name))?.referenceColumn
        }
        assertNull(primary("person"))
        assertEquals("id", primary("child"))
        assertEquals("child_id", primary("placement"))
        assertEquals("placement_id", primary("service_need"))
        assertEquals("child_id", primary("application"))
        assertEquals("head_of_family_id", primary("fee_decision"))
        assertEquals("fee_decision_id", primary("fee_decision_child"))

        val secondary = { name: String ->
            schema.tablesByName.getValue(name).secondaryReferences.map { it.referenceColumn }
        }
        assertEquals(listOf("guardian_id"), secondary("application"))
        assertEquals(listOf("partner_id"), secondary("fee_decision"))
        assertEquals(listOf("child_id"), secondary("fee_decision_child"))
        assertEquals(listOf("person_id"), secondary("message_account"))

        val order = schema.rootFirstOrder.map { it.name }
        assertEquals(
            setOf(
                "person",
                "child",
                "placement",
                "service_need",
                "application",
                "fee_decision",
                "fee_decision_child",
            ),
            order.toSet(),
        )
        for (table in order) {
            val parent =
                primary(table)?.let {
                    schema
                        .handled(table)
                        .blockingReferences
                        .first { r -> r.referenceColumn == it }
                        .referencedTable
                }
            if (parent != null)
                assert(order.indexOf(parent) < order.indexOf(table)) {
                    "$parent must come before $table"
                }
        }
        assertEquals(
            listOf("fee_decision_child", "fee_decision", "person"),
            schema.getPrimaryPath(schema.handled("fee_decision_child")).map { it.name },
        )
    }

    @Test
    fun `person and child are required and fixed`() {
        invalid("person is not declared") { SchemaDefinition(listOf(childTable())) }
        invalid("person is not declared as a handled table") {
            SchemaDefinition(listOf(ExternalTable("person", emptyList()), childTable()))
        }
        invalid("child is not declared") { SchemaDefinition(listOf(personTable)) }
        invalid("handled by adult, references nothing") {
            schema(person = HandledTable("person", CHILD, emptyList(), expirationRule = Always))
        }
        invalid("only reference is id to person") {
            schema(child = table("child", CHILD, "person_id" to "person"))
        }
    }

    @Test
    fun `duplicate tables, unknown targets, self-references, duplicate columns, composite targets and missing id columns are rejected`() {
        invalid("Duplicate tables") {
            schema(
                table("a", CHILD, "child_id" to "child"),
                table("a", CHILD, "child_id" to "child"),
            )
        }
        invalid("references unknown table b") { schema(table("a", CHILD, "b_id" to "b")) }
        invalid("self-reference") {
            schema(table("a", CHILD, "child_id" to "child", "parent_id" to "a"))
        }
        invalid("several references on the same column") {
            schema(
                HandledTable(
                    "a",
                    CHILD,
                    references =
                        listOf(
                            primaryReference("child_id", referencedTable = "child"),
                            optionalReference("child_id", referencedTable = "child"),
                        ),
                    expirationRule = Always,
                )
            )
        }
        invalid("is identified by several columns") {
            schema(
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    identifiedByCols = listOf("child_id", "n"),
                ),
                table("b", CHILD, "a_id" to "a"),
            )
        }
        invalid("has no id columns") {
            schema(table("a", CHILD, "child_id" to "child", identifiedByCols = emptyList()))
        }
    }

    @Test
    fun `every table reaches person and the references form no cycle`() {
        invalid("references nothing") { schema(table("a", CHILD)) }
        invalid("cycle") {
            schema(
                table("a", CHILD, "child_id" to "child", "b_id" to "b"),
                table("b", CHILD, "a_id" to "a"),
            )
        }
    }

    @Test
    fun `every handled table other than person declares exactly one primary reference, whose target has the table's handler`() {
        invalid("a declares no primary reference") {
            schema(
                HandledTable(
                    "a",
                    CHILD,
                    references = listOf(secondaryReference("child_id", referencedTable = "child")),
                    expirationRule = Always,
                )
            )
        }
        invalid("a declares 2 primary references, exactly one is required") {
            schema(
                HandledTable(
                    "a",
                    ADULT,
                    references =
                        listOf(
                            primaryReference("head_id", referencedTable = "person"),
                            primaryReference("partner_id", referencedTable = "person"),
                        ),
                    expirationRule = Always,
                )
            )
        }
        invalid("a is handled by CHILD, but its primary reference person_id leads to ADULT rows") {
            schema(table("a", CHILD, "person_id" to "person"))
        }
        invalid("a is handled by ADULT, but its primary reference child_id leads to CHILD rows") {
            schema(table("a", ADULT, "child_id" to "child"))
        }
        invalid("x is an external table and should not have a primary reference") {
            schema(
                ExternalTable(
                    "x",
                    listOf(primaryReference("person_id", referencedTable = "person")),
                )
            )
        }
        val schema =
            schema(
                table("a", CHILD, "child_id" to "child"),
                HandledTable(
                    "b",
                    CHILD,
                    references =
                        listOf(
                            secondaryReference("a_id", referencedTable = "a"),
                            primaryReference("other_id", referencedTable = "a"),
                        ),
                    expirationRule = Always,
                ),
            )
        assertEquals("other_id", schema.primaryReference(schema.handled("b"))?.referenceColumn)
        assertEquals(
            listOf("a_id"),
            schema.handled("b").secondaryReferences.map { it.referenceColumn },
        )
    }

    @Test
    fun `an external table is never loaded as own rows, and no reference leads to it`() {
        val schema = schema(externalTable("message_account", "person_id" to "person"))
        assertEquals(listOf("child"), schema.rootFirstOrder.drop(1).map { it.name })
        assertEquals(
            listOf("person_id"),
            schema.tablesByName.getValue("message_account").secondaryReferences.map {
                it.referenceColumn
            },
        )
        invalid("a: column account_id references external table message_account") {
            schema(
                externalTable("message_account", "person_id" to "person"),
                table("a", CHILD, "child_id" to "child", "account_id" to "message_account"),
            )
        }
        invalid("a: column account_id references external table message_account") {
            schema(
                externalTable("message_account", "person_id" to "person"),
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    optional =
                        listOf(
                            optionalReference("account_id", referencedTable = "message_account")
                        ),
                ),
            )
        }
    }

    @Test
    fun `an outside graph reference points outside the schema definition from a column that is not a reference`() {
        invalid("is a declared table, not one outside the graph") {
            schema(
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    orphansToDelete = listOf(OutsideGraphReference("b_id", "b")),
                ),
                table("b", CHILD, "child_id" to "child"),
            )
        }
        invalid("is a declared reference, not one outside the graph") {
            schema(
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    orphansToDelete = listOf(OutsideGraphReference("child_id", "x")),
                )
            )
        }
        invalid("several outside graph references on the same column") {
            schema(
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    orphansToDelete =
                        listOf(
                            OutsideGraphReference("x_id", "x"),
                            OutsideGraphReference("x_id", "y"),
                        ),
                )
            )
        }
        valid {
            schema(
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    orphansToDelete = listOf(OutsideGraphReference("x_id", "x")),
                )
            )
        }
    }

    @Test
    fun `a table is bundled by an ancestor on its primary path`() {
        invalid("is bundled by unknown table") {
            schema(table("a", CHILD, "child_id" to "child", bundledBy = "b"))
        }
        invalid("is bundled by a, which is not on its primary path") {
            schema(table("a", CHILD, "child_id" to "child", bundledBy = "a"))
        }
        invalid("is bundled by a, which is not on its primary path") {
            schema(
                table("a", CHILD, "child_id" to "child"),
                table("b", CHILD, "child_id" to "child", bundledBy = "a"),
            )
        }
        invalid("is bundled by b, which is not on its primary path") {
            // c reaches b only through a secondary reference
            schema(
                table("a", CHILD, "child_id" to "child"),
                table("b", CHILD, "child_id" to "child"),
                table("c", CHILD, "a_id" to "a", "b_id" to "b", bundledBy = "b"),
            )
        }
        val schema =
            schema(
                table("a", CHILD, "child_id" to "child"),
                table("b", CHILD, "a_id" to "a"),
                table("c", CHILD, "b_id" to "b", bundledBy = "a"),
            )
        assertEquals("a", schema.bundlerOf(schema.handled("c"))?.name)
        assertNull(schema.bundlerOf(schema.handled("b")))
    }

    @Test
    fun `a deletion bundle has independent rows in every table or in none`() {
        invalid("mixes tables") {
            schema(
                table("a", CHILD, "child_id" to "child", independentRows = true),
                table("b", CHILD, "a_id" to "a", bundledBy = "a"),
            )
        }
        invalid("mixes tables") {
            // The table between a and c belongs to the bundle too
            schema(
                table("a", CHILD, "child_id" to "child", independentRows = true),
                table("b", CHILD, "a_id" to "a"),
                table("c", CHILD, "b_id" to "b", independentRows = true, bundledBy = "a"),
            )
        }
        val schema =
            schema(
                table("a", CHILD, "child_id" to "child", independentRows = true),
                table("b", CHILD, "a_id" to "a", independentRows = true),
                table("c", CHILD, "b_id" to "b", independentRows = true, bundledBy = "a"),
                table("d", CHILD, "a_id" to "a"),
            )
        assertEquals(6, schema.tablesByName.size)
    }

    @Test
    fun `optional references are found from their target`() {
        val schema =
            schema(
                table("application", CHILD, "child_id" to "child"),
                HandledTable(
                    "placement",
                    CHILD,
                    references =
                        listOf(
                            primaryReference("child_id", referencedTable = "child"),
                            optionalReference(
                                "source_application_id",
                                referencedTable = "application",
                                alsoNull = listOf("source"),
                            ),
                        ),
                    expirationRule = Always,
                ),
            )
        val application = schema.tablesByName.getValue("application")
        assertEquals(
            listOf(
                "placement" to
                    Reference.Optional("source_application_id", "application", listOf("source"))
            ),
            schema.optionalReferencesInto(application).map { it.referencingTable to it.reference },
        )
        assertEquals(emptyList(), schema.handled("placement").secondaryReferences)
    }

    private val oneYear = Period.ofYears(1)

    private fun withRule(rule: ExpirationRule, vararg others: Table) =
        schema(table("a", CHILD, "child_id" to "child", expirationRule = rule), *others)

    @Test
    fun `a date source reads a column of the table itself or of a handled table of the same handler, and one column is read in one way`() {
        invalid("reads a table that is not declared as a handled table") {
            withRule(After(oneYear, GraphTableColumn("x", "end_date")))
        }
        invalid("reads a table that is not declared as a handled table") {
            withRule(
                After(oneYear, GraphTableColumn("x", "end_date")),
                externalTable("x", "person_id" to "person"),
            )
        }
        invalid("reads a table with another handler") {
            withRule(
                After(oneYear, GraphTableColumn("b", "end_date")),
                table("b", ADULT, "person_id" to "person"),
            )
        }
        invalid("reads b.end_date as DATE, but it is read as TIMESTAMP_WITH_TIME_ZONE elsewhere") {
            withRule(
                After(oneYear, GraphTableColumn("b", "end_date", TIMESTAMP_WITH_TIME_ZONE)),
                table(
                    "b",
                    CHILD,
                    "child_id" to "child",
                    expirationRule = After(oneYear, OwnColumn("end_date")),
                ),
            )
        }
        val schema =
            withRule(
                AllOf(
                    After(oneYear, GraphTableColumn("person", "date_of_birth")),
                    After(oneYear, GraphTableColumn("b", "end_date")),
                    After(oneYear, OwnColumn("created", TIMESTAMP_WITH_TIME_ZONE)),
                ),
                table("b", CHILD, "child_id" to "child"),
            )
        assertEquals(
            mapOf("created" to TIMESTAMP_WITH_TIME_ZONE),
            schema.dateColumnsToRead(schema.handled("a")),
        )
        assertEquals(mapOf("end_date" to DATE), schema.dateColumnsToRead(schema.handled("b")))
        assertEquals(mapOf("date_of_birth" to DATE), schema.dateColumnsToRead(schema.person))
    }

    @Test
    fun `a custom date source and an archived rule need rows identified by a single column`() {
        invalid("a custom date source needs rows identified by a single column") {
            schema(
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    identifiedByCols = listOf("child_id", "n"),
                    expirationRule = Coalesce(After(oneYear, customSource()), Never),
                )
            )
        }
        invalid("an archived if required rule needs rows identified by a single column") {
            schema(
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    identifiedByCols = listOf("child_id", "n"),
                    expirationRule = archivedIfRequiredRule(),
                )
            )
        }
        valid {
            schema(
                table(
                    "a",
                    CHILD,
                    "child_id" to "child",
                    expirationRule =
                        AllOf(
                            Coalesce(After(oneYear, customSource()), Never),
                            archivedIfRequiredRule(),
                        ),
                )
            )
        }
    }

    private val endDate = OwnColumn("end_date")
    private val nullableOwn = After(oneYear, endDate)
    private val ownDate = After(oneYear, OwnColumn("created"))

    /** Only the end date may be missing */
    private fun validated(rule: ExpirationRule) = rule.validate("a") { it == endDate }

    @Test
    fun `a rule that may find no date is wrapped directly in a coalesce with a fallback`() {
        invalid("add a fallback with Coalesce") { validated(nullableOwn) }
        invalid("no rule in Coalesce") { validated(Coalesce(nullableOwn, nullableOwn)) }
        invalid("never reached") { validated(Coalesce(nullableOwn, Always, Never)) }
        invalid("wrap it in Coalesce") { validated(AllOf(nullableOwn, Always)) }
        invalid("wrap it in Coalesce") { validated(AnyOf(nullableOwn, Always)) }
        invalid("wrap it in Coalesce") { validated(Coalesce(AllOf(nullableOwn, Always), Always)) }
        valid { validated(Coalesce(nullableOwn, Always)) }
        valid { validated(AllOf(Coalesce(nullableOwn, Always), ownDate)) }
        valid { validated(Coalesce(nullableOwn, ownDate)) }
        valid { validated(Coalesce(nullableOwn, Coalesce(nullableOwn, ownDate))) }
    }

    @Test
    fun `a rule safe for integrations is validated, and its sources and archived rules found, through the wrapper`() {
        invalid("add a fallback with Coalesce") { validated(nullableOwn.safeFor(VARDA)) }
        invalid("wrap it in Coalesce") { validated(AllOf(nullableOwn, Always).safeFor(VARDA)) }
        valid { validated(Coalesce(nullableOwn, ownDate).safeFor(VARDA)) }
        valid { validated(AllOf(Coalesce(nullableOwn, ownDate).safeFor(VARDA), Always)) }
        invalid("safe for no integration") { Always.safeFor() }

        val source = customSource()
        val archived = archivedIfRequiredRule()
        val rule = AllOf(Coalesce(After(oneYear, source), ownDate), archived).safeFor(KOSKI, VARDA)
        assertEquals(setOf(source, OwnColumn("created")), rule.usedDateSources())
        assertEquals(setOf(archived), rule.usedArchivedIfRequiredRules())
        assertEquals(setOf(KOSKI, VARDA), rule.integrations)
        assertEquals(
            mapOf("created" to DATE),
            withRule(rule).dateColumnsToRead(withRule(rule).handled("a")),
        )
    }

    private val declaredSchemas = listOf(false, true).map { buildDataRetentionSchema(it) }

    @Test
    fun `every declared table an integration reads is safe for that integration directly, and no table claims an integration that does not read it`() {
        val inputTablesByIntegration =
            mapOf(KOSKI to KOSKI_INPUT_TABLES, VARDA to VARDA_INPUT_TABLES)
        for (schema in declaredSchemas) {
            val invalid =
                schema.handledTables.flatMap { table ->
                    val declared =
                        (table.expirationRule as? SafeForIntegrations)?.integrations.orEmpty()
                    inputTablesByIntegration.mapNotNull { (integration, inputTables) ->
                        when {
                            table.name in inputTables && integration !in declared ->
                                "$table is read by $integration but is not declared safe for it"
                            table.name !in inputTables && integration in declared ->
                                "$table is declared safe for $integration, which does not read it"
                            else -> null
                        }
                    }
                }
            assertEquals(emptyList(), invalid)
        }
    }

    /** Always on its own would delete the rows the first time a run reaches them */
    @Test
    fun `every declared table whose rule is Always, wrapped or not, is bundled`() {
        for (schema in declaredSchemas) {
            val invalid =
                schema.handledTables.filter { table ->
                    val rule = table.expirationRule
                    val always =
                        rule == Always || (rule is SafeForIntegrations && rule.rule == Always)
                    always && table.bundledBy == null
                }
            assertEquals(emptyList(), invalid)
        }
    }

    @Test
    fun `the declared schema definition is valid and loads every table after the one its primary reference points to`() {
        for (schema in declaredSchemas) {
            val order = schema.rootFirstOrder
            assertEquals(schema.handledTables.toSet(), order.toSet())
            for (table in order) {
                val parent = schema.primaryReference(table)?.referencedTable ?: continue
                assert(order.indexOf(schema.handled(parent)) < order.indexOf(table)) {
                    "$parent must be loaded before $table"
                }
            }
        }
    }
}
