// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleCacheTest : AccessControlTest() {
    private val clock = MockEvakaClock(2023, 1, 1, 12, 0)

    private fun <P : Any> createUnscopedRule(
        params: P,
        cacheKey: Any,
        deferred: () -> DatabaseActionRule.Deferred<P>
    ) =
        DatabaseActionRule.Unscoped(
            params,
            object : DatabaseActionRule.Unscoped.Query<P> {
                override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
                    Triple(user, now, cacheKey)

                override fun execute(
                    ctx: DatabaseActionRule.QueryContext
                ): DatabaseActionRule.Deferred<P> = deferred()
            }
        )

    @Test
    fun `unscoped rules with same type and same cache keys use cache in a single permission check`() {
        var executionCount = 0
        val actions =
            listOf(
                Action.Global.CREATE_UNIT,
                Action.Global.CREATE_PERSON,
                Action.Global.CREATE_PAPER_APPLICATION
            )

        // Multiple different instances of the same rule that take different params (= the index
        // number).
        // When *executed*, they return the same deferred, but when the deferred is *evaluated*, it
        // only gives permission if we're evaluating the first rule
        val deferred =
            object : DatabaseActionRule.Deferred<Int> {
                override fun evaluate(params: Int): AccessControlDecision =
                    if (params == 0) AccessControlDecision.Permitted(params)
                    else AccessControlDecision.None
            }
        actions.withIndex().forEach { (idx, action) ->
            rules.add(
                action,
                createUnscopedRule(idx, cacheKey = Unit) { deferred.also { executionCount += 1 } }
            )
        }
        val permittedActions =
            db.read { tx ->
                accessControl.getPermittedActions<Action.Global>(
                    tx,
                    AuthenticatedUser.SystemInternalUser,
                    clock
                )
            }
        // Without a cache all rules would be executed, but with the cache it should keep the
        // deferred data of the first execution, which is evaluated multiple times (with different
        // results)
        assertEquals(1, executionCount)
        assertEquals(setOf(actions.first()), permittedActions)
    }

    @Test
    fun `unscoped rules with same type and different cache keys don't use cache in a single permission check`() {
        val executed = mutableSetOf<Action.Global>()
        val actions =
            setOf(
                Action.Global.CREATE_UNIT,
                Action.Global.CREATE_PERSON,
                Action.Global.CREATE_PAPER_APPLICATION
            )

        // Multiple different instances of the same rule that take same params (= Unit) but use
        // different cache keys.
        actions.withIndex().forEach { (idx, action) ->
            rules.add(
                action,
                createUnscopedRule(Unit, cacheKey = idx) {
                    DatabaseActionRule.Deferred.Permitted.also { executed += action }
                }
            )
        }
        val permittedActions =
            db.read { tx ->
                accessControl.getPermittedActions<Action.Global>(
                    tx,
                    AuthenticatedUser.SystemInternalUser,
                    clock
                )
            }
        assertEquals(actions, executed)
        assertEquals(actions, permittedActions)
    }
}
