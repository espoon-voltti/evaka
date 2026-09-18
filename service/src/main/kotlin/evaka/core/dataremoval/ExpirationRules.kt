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

/** How a date is read from a column. */
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
        val mayReturnNoDate: Boolean,
        val query: (tx: Database.Read, ids: List<UUID>) -> Map<UUID, LocalDate>,
    ) : DateSource {
        override fun toString(): String = "custom date source"
    }
}

enum class Integration {
    KOSKI,
    VARDA,
}

/**
 * The rows of a table that have expired based on its expiration rule are deleted if nothing else
 * depends on them
 */
sealed interface ExpirationRule {
    /** Always expired */
    data object Always : ExpirationRule

    /** Never expired */
    data object Never : ExpirationRule

    /** Multiple rules combined with AND */
    data class AllOf(val rules: List<ExpirationRule>) : ExpirationRule {
        constructor(vararg rules: ExpirationRule) : this(rules.toList())
    }

    /** Multiple rules combined with OR */
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
     * Wrapping a rule with this adds an additional condition that the node cannot expire before it
     * is safe from an external data sync's point of view.
     *
     * If data has already been sent to the integration, it must not be removed before the child has
     * reached [SAFE_DATA_REMOVAL_AGE] and the sync can be frozen permanently.
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

fun ExpirationRule.usedDateSources(): Set<DateSource> =
    when (this) {
        is AllOf -> rules.flatMapTo(mutableSetOf()) { it.usedDateSources() }
        is AnyOf -> rules.flatMapTo(mutableSetOf()) { it.usedDateSources() }
        is Coalesce -> rules.flatMapTo(mutableSetOf()) { it.usedDateSources() }
        is SafeForIntegrations -> rule.usedDateSources()
        is After -> setOf(dateSource)
        Always,
        Never,
        is ArchivedIfRequired -> emptySet()
    }

fun ExpirationRule.usedCustomDateSources(): Set<DateSource.Custom> =
    usedDateSources().filterIsInstance<DateSource.Custom>().toSet()

fun ExpirationRule.usedArchivedIfRequiredRules(): Set<ArchivedIfRequired> =
    when (this) {
        is ArchivedIfRequired -> setOf(this)
        is AllOf -> rules.flatMapTo(mutableSetOf()) { it.usedArchivedIfRequiredRules() }
        is AnyOf -> rules.flatMapTo(mutableSetOf()) { it.usedArchivedIfRequiredRules() }
        is Coalesce -> rules.flatMapTo(mutableSetOf()) { it.usedArchivedIfRequiredRules() }
        is SafeForIntegrations -> rule.usedArchivedIfRequiredRules()
        Always,
        Never,
        is After -> emptySet()
    }
