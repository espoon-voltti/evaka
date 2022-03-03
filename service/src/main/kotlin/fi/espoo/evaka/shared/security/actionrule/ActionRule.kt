// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision

/**
 * A rule that grants permission based on an `AuthenticatedUser`, without needing any additional information
 */
interface StaticActionRule : ScopedActionRule<Any> {
    fun isPermitted(user: AuthenticatedUser): Boolean
}

/**
 * A rule that grants permission based on an `AuthenticatedUser` and a "target", which is something
 * that can be used in a database query (e.g. an ID of some sort).
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
 * Instead of passing just one target, we pass a set, and the function returns a map so we can associate each target
 * with a separate result. If we do both the query/pure split *and* support multiple targets, we get this function:
 *
 * `(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, (params: P) -> AccessControlDecision>`
 *
 * Now, if we have N targets and M parameters, we can call `query()` once with all the targets and then each deferred
 * function once per parameter (M times). As a result, we end up doing N*M cheap deferred function evaluations and only
 * one expensive database query. This is much better than the original naive version which would do N*M expensive
 * database queries in this scenario.
 */
data class DatabaseActionRule<T, P : Any>(val params: P, val query: Query<T, P>) : ScopedActionRule<T> {
    interface Query<T, P> {
        fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, Deferred<P>>
    }
    interface Deferred<P> {
        fun evaluate(params: P): AccessControlDecision
    }
}

interface ActionRuleParams<This> {
    fun merge(other: This): This
}
