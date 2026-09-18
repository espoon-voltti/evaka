// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.AllOf
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.AnyOf
import evaka.core.dataremoval.ExpirationRule.ArchivedIfRequired
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.ExpirationRule.Never
import evaka.core.dataremoval.ExpirationRule.SafeForIntegrations
import evaka.core.shared.db.Database
import java.time.LocalDate
import java.time.Period
import java.util.UUID

/**
 * How a date is read from a column: a timestamp as its date in Finnish time, a date range as its
 * inclusive end date, which an open-ended range does not have. A finite date range never has an
 * open end, which the schema test verifies from a check constraint on the column.
 */
enum class DateColumnType {
    DATE,
    TIMESTAMP_WITH_TIME_ZONE,
    DATE_RANGE_END,
    FINITE_DATE_RANGE_END,
}

/** Where a reference date rule gets its date */
sealed interface DateSource {
    /**
     * A column of the table itself: the date of the row, or the latest date over the rows of a node
     * evaluated as a whole
     */
    data class OwnColumn(
        val column: String,
        val columnType: DateColumnType = DateColumnType.DATE,
    ) : DateSource

    /** A column of another table, the latest date over all of that table's rows in the graph */
    data class GraphTableColumn(
        val table: String,
        val column: String,
        val columnType: DateColumnType = DateColumnType.DATE,
    ) : DateSource

    /**
     * A query written for the table, which gives a date for each of the ids it is given. A node
     * evaluated as a whole takes the latest date of its rows.
     */
    class Custom(
        /** Whether the query may give no date for a row, so that the rule needs a fallback */
        val mayHaveNoDate: Boolean,
        val query: (tx: Database.Read, ids: List<UUID>) -> Map<UUID, LocalDate>,
    ) : DateSource {
        override fun toString(): String = "custom date source"
    }
}

/**
 * The external systems that read some of the tables, and freeze their sync for a child once rows
 * the system read are deleted
 */
enum class Integration {
    KOSKI,
    VARDA,
}

/** When the rows of a table have expired; they are deleted if nothing depends on them */
sealed interface ExpirationRule {
    data object Always : ExpirationRule

    data object Never : ExpirationRule

    data class AllOf(val rules: List<ExpirationRule>) : ExpirationRule {
        constructor(vararg rules: ExpirationRule) : this(rules.toList())
    }

    data class AnyOf(val rules: List<ExpirationRule>) : ExpirationRule {
        constructor(vararg rules: ExpirationRule) : this(rules.toList())
    }

    /** The result of the first rule that returns one */
    data class Coalesce(val rules: List<ExpirationRule>) : ExpirationRule {
        constructor(vararg rules: ExpirationRule) : this(rules.toList())
    }

    /**
     * Expired once the period has passed since the date. Returns no result when the date is
     * missing.
     */
    data class After(val period: Period, val dateSource: DateSource) : ExpirationRule

    /**
     * Rows that must be archived keep the node until they have been: the function returns the ids
     * of the rows still waiting
     */
    class ArchivedIfRequired(
        val idsAwaitingArchival: (tx: Database.Read, ids: List<UUID>) -> Set<UUID>
    ) : ExpirationRule {
        override fun toString(): String = "archived if required rule"
    }

    /**
     * The wrapped rule, and every child the node concerns is either never sent to each of the
     * integrations or past [SAFE_DATA_REMOVAL_AGE], so that deleting the rows never freezes the
     * sync of a child who may still return. Declared on every table the integration reads.
     */
    class SafeForIntegrations
    private constructor(val integrations: Set<Integration>, val rule: ExpirationRule) :
        ExpirationRule {
        init {
            require(integrations.isNotEmpty()) { "$rule is safe for no integration" }
        }

        override fun toString(): String = "$rule.safeFor(${integrations.joinToString()})"

        companion object {
            fun ExpirationRule.safeFor(vararg integrations: Integration): SafeForIntegrations =
                SafeForIntegrations(integrations.toSet(), this)
        }
    }
}

fun ExpirationRule.dateSources(): Set<DateSource> =
    when (this) {
        is AllOf -> rules.flatMapTo(mutableSetOf()) { it.dateSources() }
        is AnyOf -> rules.flatMapTo(mutableSetOf()) { it.dateSources() }
        is Coalesce -> rules.flatMapTo(mutableSetOf()) { it.dateSources() }
        is SafeForIntegrations -> rule.dateSources()
        is After -> setOf(dateSource)
        Always,
        Never,
        is ArchivedIfRequired -> emptySet()
    }

fun ExpirationRule.customDateSources(): Set<DateSource.Custom> =
    dateSources().filterIsInstance<DateSource.Custom>().toSet()

fun ExpirationRule.archivedIfRequiredRules(): Set<ArchivedIfRequired> =
    when (this) {
        is AllOf -> rules.flatMapTo(mutableSetOf()) { it.archivedIfRequiredRules() }
        is AnyOf -> rules.flatMapTo(mutableSetOf()) { it.archivedIfRequiredRules() }
        is Coalesce -> rules.flatMapTo(mutableSetOf()) { it.archivedIfRequiredRules() }
        is SafeForIntegrations -> rule.archivedIfRequiredRules()
        is ArchivedIfRequired -> setOf(this)
        Always,
        Never,
        is After -> emptySet()
    }
