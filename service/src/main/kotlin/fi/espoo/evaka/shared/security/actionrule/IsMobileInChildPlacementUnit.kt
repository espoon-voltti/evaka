// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.MobileAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import org.jdbi.v3.core.kotlin.mapTo

private typealias FilterByMobile<T> = (tx: Database.Read, mobileId: MobileDeviceId, targets: Set<T>) -> Iterable<T>

data class IsMobileInChildPlacementUnit(val requirePinLogin: Boolean) : ActionRuleParams<IsMobileInChildPlacementUnit> {
    override fun merge(other: IsMobileInChildPlacementUnit): IsMobileInChildPlacementUnit =
        IsMobileInChildPlacementUnit(this.requirePinLogin && other.requirePinLogin)

    private data class Query<T>(private val filterByMobile: FilterByMobile<T>) : DatabaseActionRule.Query<T, IsMobileInChildPlacementUnit> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<IsMobileInChildPlacementUnit>> = when (user) {
            is AuthenticatedUser.MobileDevice -> filterByMobile(tx, MobileDeviceId(user.id), targets).associateWith { Deferred(user.authLevel) }
            else -> emptyMap()
        }
    }
    private data class Deferred(private val authLevel: MobileAuthLevel) : DatabaseActionRule.Deferred<IsMobileInChildPlacementUnit> {
        override fun evaluate(params: IsMobileInChildPlacementUnit): AccessControlDecision =
            if (params.requirePinLogin && authLevel != MobileAuthLevel.PIN_LOGIN) {
                AccessControlDecision.Denied(params, "PIN login required", "PIN_LOGIN_REQUIRED")
            } else {
                AccessControlDecision.Permitted(params)
            }
    }

    val child = DatabaseActionRule(
        this,
        Query<ChildId> { tx, mobileId, ids ->
            tx.createQuery(
                """
SELECT child_id
FROM child_acl_view
WHERE employee_id = :userId
AND role = 'MOBILE'
AND child_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )
    val childDailyNote = DatabaseActionRule(
        this,
        Query<ChildDailyNoteId> { tx, mobileId, ids ->
            tx.createQuery(
                """
SELECT cdn.id
FROM child_acl_view
JOIN child_daily_note cdn ON child_acl_view.child_id = cdn.child_id
WHERE employee_id = :userId
AND role = 'MOBILE'
AND cdn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )
    val childImage = DatabaseActionRule(
        this,
        Query<ChildImageId> { tx, mobileId, ids ->
            tx.createQuery(
                """
SELECT img.id
FROM child_acl_view
JOIN child_images img ON child_acl_view.child_id = img.child_id
WHERE employee_id = :userId
AND role = 'MOBILE'
AND img.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )
}
