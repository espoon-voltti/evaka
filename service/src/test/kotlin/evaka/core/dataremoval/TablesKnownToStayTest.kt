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
import evaka.core.shared.DatabaseTable
import java.time.LocalDate
import java.time.Period
import kotlin.test.Test
import kotlin.test.assertEquals

class TablesKnownToStayTest {
    private val oneYear = Period.ofYears(1)
    private val target = targetId.raw
    private val recently = today.minusMonths(1)
    private val longAgo = today.minusYears(2)

    private val lastPlacementEnd = GraphTableColumn("placement", "end_date")
    private val ownEndDate = OwnColumn("end_date")

    /** Cannot be met while the last placement ended within a year */
    private val placementRule = Coalesce(After(oneYear, lastPlacementEnd), Always)

    private fun placementTable(rule: ExpirationRule = Always, deletedWithParent: Boolean = false) =
        table(
            "placement",
            CHILD,
            "child_id" to "child",
            expirationRule = rule,
            deletedWithParent = deletedWithParent,
        )

    /** A child-handled table whose primary reference points to child */
    private fun underChild(name: String, rule: ExpirationRule, deletedWithParent: Boolean = false) =
        table(
            name,
            CHILD,
            "child_id" to "child",
            expirationRule = rule,
            deletedWithParent = deletedWithParent,
        )

    /** The placement rows are given by their end dates. A table without rows has no node. */
    private fun SchemaDefinition.knownToStay(
        vararg placementEnds: LocalDate,
        childRow: Boolean = true,
        integrationFacts: IntegrationFacts = neverSent,
    ): Set<String> {
        val placements = placementEnds.map {
            row("child_id" to target, dateByColumn = mapOf("end_date" to it))
        }
        val placementNodes =
            if (placements.isEmpty()) emptyList()
            else listOf(ownNode("placement", *placements.toTypedArray()))
        return tablesKnownToStay(
            withPersonAndChild(*placementNodes.toTypedArray(), childRow = childRow),
            integrationFacts,
            today,
        )
    }

    @Test
    fun `a rule that reads only the placements is decided from them, and a rule that reads other data might be met`() {
        val schema =
            schema(
                placementTable(),
                underChild("by_placements", placementRule),
                underChild("by_own_column", Coalesce(After(oneYear, ownEndDate), Never)),
                underChild("by_custom_source", Coalesce(After(oneYear, customSource()), Never)),
                underChild("by_not_while", notWhileRule()),
                underChild("never", Never),
            )
        assertEquals(setOf("by_placements", "never"), schema.knownToStay(longAgo, recently))
        assertEquals(setOf("never"), schema.knownToStay(longAgo))
    }

    @Test
    fun `all of stays when one of its rules cannot be met, and any of only when none of them can`() {
        val ownColumnRule = After(oneYear, ownEndDate)
        val schema =
            schema(
                placementTable(),
                underChild("all_of", AllOf(ownColumnRule, placementRule)),
                underChild("any_of", AnyOf(ownColumnRule, placementRule)),
                underChild("any_of_never", AnyOf(Never, placementRule)),
            )
        assertEquals(setOf("all_of", "any_of_never"), schema.knownToStay(recently))
    }

    @Test
    fun `coalesce moves on only from a rule that might return no verdict`() {
        val schema =
            schema(
                placementTable(),
                underChild("placements_then_always", placementRule),
                underChild(
                    "placements_then_never",
                    Coalesce(After(oneYear, lastPlacementEnd), Never),
                ),
                underChild("own_column_then_never", Coalesce(After(oneYear, ownEndDate), Never)),
            )
        // Without placements, the placement rule returns no verdict
        assertEquals(setOf("placements_then_never"), schema.knownToStay())
        assertEquals(
            setOf("placements_then_always", "placements_then_never"),
            schema.knownToStay(recently),
        )
    }

    @Test
    fun `a rule safe for an integration stays while the target is not safe to delete for it, if the table concerns the target, and otherwise the rule it wraps decides`() {
        val schema =
            schema(
                placementTable(),
                underChild("child_handled", Always.safeFor(KOSKI)),
                table(
                    "adult_handled",
                    ADULT,
                    "person_id" to "person",
                    expirationRule = Always.safeFor(KOSKI),
                ),
                underChild("placement_rule", placementRule.safeFor(KOSKI)),
                person =
                    HandledTable(
                        "person",
                        ADULT,
                        references = emptyList(),
                        auditIdType = DatabaseTable.Person::class,
                        expirationRule = Always.safeFor(KOSKI),
                    ),
            )
        val young = today.minusYears(5)
        val pastSafeAge = today.minusYears(SAFE_DATA_REMOVAL_AGE).minusDays(1)
        fun knownToStay(dateOfBirth: LocalDate, sentToKoski: Boolean, sentToVarda: Boolean) =
            schema.knownToStay(
                integrationFacts =
                    IntegrationFacts(
                        dateOfBirth,
                        sentToKoski = sentToKoski,
                        sentToVarda = sentToVarda,
                    )
            )
        assertEquals(
            setOf("person", "child_handled", "placement_rule"),
            knownToStay(young, sentToKoski = true, sentToVarda = false),
        )
        assertEquals(emptySet(), knownToStay(young, sentToKoski = false, sentToVarda = true))
        assertEquals(emptySet(), knownToStay(pastSafeAge, sentToKoski = true, sentToVarda = true))
        // When the check passes, the rule it wraps decides
        assertEquals(setOf("placement_rule"), schema.knownToStay(recently))
    }

    @Test
    fun `a preloaded table is decided from its own rows, and a child node without its row does not stay by its own rule`() {
        val ownColumnRule = After(oneYear, ownEndDate)
        val schema =
            schema(
                placementTable(ownColumnRule),
                underChild("reads_placements", placementRule),
                underChild("not_preloaded", ownColumnRule),
            )
        // The placements keep child and person, which lie on their primary path
        assertEquals(
            setOf("placement", "child", "person", "reads_placements"),
            schema.knownToStay(longAgo, recently),
        )
        assertEquals(emptySet(), schema.knownToStay(longAgo))

        val withChildRule = schema(child = childTable(Never))
        assertEquals(setOf("child", "person"), withChildRule.knownToStay())
        assertEquals(emptySet(), withChildRule.knownToStay(childRow = false))
    }

    @Test
    fun `a table that stays keeps the tables deleted with it, and preloaded rows keep the tables on their primary path`() {
        val schema =
            schema(
                placementTable(Never),
                underChild("deleted_with_child", Always, deletedWithParent = true),
                table(
                    "deleted_with_that",
                    CHILD,
                    "parent_id" to "deleted_with_child",
                    expirationRule = Always,
                    deletedWithParent = true,
                ),
                underChild("not_deleted_with_child", Always),
                table(
                    "deleted_with_person",
                    ADULT,
                    "person_id" to "person",
                    expirationRule = Always,
                    deletedWithParent = true,
                ),
                // Makes placement a preloaded table
                underChild("reads_placements", placementRule),
            )
        assertEquals(
            setOf(
                "placement",
                "child",
                "person",
                "deleted_with_child",
                "deleted_with_that",
                "deleted_with_person",
            ),
            schema.knownToStay(longAgo),
        )
        assertEquals(emptySet(), schema.knownToStay())
    }

    @Test
    fun `a table known to stay is skipped unless something needs its rows`() {
        val schema =
            schema(
                placementTable(),
                underChild("reads_placements", placementRule),
                underChild("plain", Always),
                underChild("deep_parent", Always),
                table("deep_read", CHILD, "parent_id" to "deep_parent"),
                underChild(
                    "reads_deep",
                    Coalesce(After(oneYear, GraphTableColumn("deep_read", "end_date")), Always),
                ),
                table(
                    "references_unknown",
                    CHILD,
                    "child_id" to "child",
                    "other_id" to "reads_placements",
                ),
                underChild("parent_of_loaded", Always),
                table("loaded_below", CHILD, "parent_id" to "parent_of_loaded"),
                underChild("referenced_otherwise", Always),
                table(
                    "optional_referrer",
                    CHILD,
                    "child_id" to "child",
                    optional =
                        listOf(
                            optionalReference("ref_id", referencedTable = "referenced_otherwise")
                        ),
                ),
                externalTable("external_referrer", "ref_id" to "referenced_otherwise"),
            )
        val knownToStay =
            setOf(
                "person",
                "child",
                "placement",
                "plain",
                "deep_parent",
                "deep_read",
                "references_unknown",
                "parent_of_loaded",
                "referenced_otherwise",
            )
        // The others known to stay are not skipped:
        // - placement is preloaded
        // - reads_deep is not known to stay, and it reads the dates of deep_read
        // - references_unknown references reads_placements, which is not known to stay
        // - deep_parent and parent_of_loaded are the parents of loaded tables
        assertEquals(setOf("plain", "referenced_otherwise"), schema.tablesToSkip(knownToStay))
    }
}
