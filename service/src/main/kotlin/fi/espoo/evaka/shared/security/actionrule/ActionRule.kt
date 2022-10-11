// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.QueryFunction
import fi.espoo.evaka.shared.db.predicate
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.security.PilotFeature
import org.jdbi.v3.core.mapper.Nested

/**
 * A rule that grants permission based on an `AuthenticatedUser`, without needing any additional information
 */
interface StaticActionRule : ScopedActionRule<Any>, UnscopedActionRule {
    fun evaluate(user: AuthenticatedUser): AccessControlDecision
}

/**
 * A rule that grants permission based on an `AuthenticatedUser`, and possibly some data from the database.
 */
sealed interface UnscopedActionRule

/**
 * A rule that grants permission based on an `AuthenticatedUser` and a "target", which is some data that can be
 * used to evaluate permissions by itself or as part of a database query.
 */
sealed interface ScopedActionRule<T>

/**
 * A rule that grants permission based on an `AuthenticatedUser` and some data that can be fetched using a "target" T.
 *
 * For performance reasons, this rule is split into two parts: "Query" and "Deferred". Here's why:
 *
 * In the naive simple case, a database rule could be modeled as a function like this:
 *
 * `(tx: Database.Read, user: AuthenticatedUser, target: T, params: P): AccessControlDecision`
 *
 * If we have one target but want to evaluate the rule multiple times with different parameters, this function would
 * need to be called multiple times, which would do one database query per call. However, we can split the function
 * into a separate "database query part" which uses targets and returns a "pure function part" which only needs
 * parameters. This "pure function part" internally uses whatever data was queried from the database earlier, but it
 * can be called multiple times with different parameters.
 *
 * `(tx: Database.Read, user: AuthenticatedUser, target: T): (params: P) -> AccessControlDecision`
 *
 * Now we can call the "query" function once, which returns a deferred function that can be called multiple times with
 * different parameters. Another important change for good performance is supporting multiple targets. The original
 * naive non-split function could be changed like this:
 *
 * `(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>, params: P): Map<T, AccessControlDecision>`
 *
 * Instead of passing just one target, we pass a set and the function returns a map, so we can associate each target
 * with a separate result. If we do both the query/pure split *and* support multiple targets, we get this function:
 *
 * `(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, (params: P) -> AccessControlDecision>`
 *
 * Now, if we have N targets and M parameters, we can call `query()` once with all the targets and then each deferred
 * function once per parameter (M times). As a result, we end up doing N*M cheap deferred function evaluations and only
 * one expensive database query. This is much better than the original naive version which would do N*M expensive
 * database queries in this scenario.
 */

object DatabaseActionRule {
    data class QueryContext(val tx: Database.Read, val user: AuthenticatedUser, val now: HelsinkiDateTime)
    interface Deferred<P> {
        fun evaluate(params: P): AccessControlDecision
    }

    interface Scoped<T, P : Any> : ScopedActionRule<T> {
        val params: P
        val query: Query<T, P>

        interface Query<T, P> {
            fun executeWithTargets(ctx: QueryContext, targets: Set<T>): Map<T, Deferred<P>>
            fun filterForParams(ctx: QueryContext, params: P): QueryFunction<T>?
            override fun hashCode(): Int
            override fun equals(other: Any?): Boolean
        }
        data class Simple<T, P : Any>(override val params: P, override val query: Query<T, P>) : Scoped<T, P>
    }

    data class Unscoped<P : Any>(val params: P, val query: Query<P>) : ScopedActionRule<Any>, UnscopedActionRule {
        interface Query<P> {
            fun execute(ctx: QueryContext): Deferred<P>?
            override fun hashCode(): Int
            override fun equals(other: Any?): Boolean
        }
    }
}
internal data class IdRoleFeatures(val id: Id<*>, @Nested val roleFeatures: RoleAndFeatures)
internal data class RoleAndFeatures(val role: UserRole, val unitFeatures: Set<PilotFeature>)

sealed interface AccessControlFilter<out T> {
    object PermitAll : AccessControlFilter<Nothing>
    data class Some<T>(val filter: QueryFunction<T>) : AccessControlFilter<T>
}

fun <T : DatabaseTable> AccessControlFilter<Id<T>>.toPredicate(): Predicate<T> = when (this) {
    AccessControlFilter.PermitAll -> Predicate.alwaysTrue()
    is AccessControlFilter.Some<Id<T>> -> predicate { prefix ->
        sql("($prefix.id IN (${subquery(filter)}))")
    }
}
