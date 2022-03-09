// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.VasuDocumentFollowupEntryId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetGroupRoles<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, targets: Set<T>) -> Iterable<IdAndRole>

data class HasGroupRole(val oneOf: EnumSet<UserRole>) : ActionRuleParams<HasGroupRole> {
    init {
        oneOf.forEach { check(it.isGroupScopedRole()) { "Expected a group-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun merge(other: HasGroupRole): HasGroupRole = HasGroupRole(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )

    private data class Query<T : Id<*>>(private val getGroupRoles: GetGroupRoles<T>) : DatabaseActionRule.Query<T, HasGroupRole> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<HasGroupRole>> = when (user) {
            is AuthenticatedUser.Employee -> getGroupRoles(tx, user, targets)
                .fold(targets.associateTo(linkedMapOf()) { (it to emptyEnumSet<UserRole>()) }) { acc, (target, role) ->
                    acc[target]?.plusAssign(role)
                    acc
                }
                .mapValues { (_, rolesInGroup) -> Deferred(rolesInGroup) }
            else -> emptyMap()
        }
    }
    private data class Deferred(private val rolesInGroup: Set<UserRole>) : DatabaseActionRule.Deferred<HasGroupRole> {
        override fun evaluate(params: HasGroupRole): AccessControlDecision = if (rolesInGroup.any { params.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(params)
        } else {
            AccessControlDecision.None
        }
    }

    fun inPlacementGroupOfChild() = DatabaseActionRule(
        this,
        Query<ChildId> { tx, user, ids ->
            tx.createQuery(
                """
    SELECT child_id AS id, role
    FROM child_acl_view
    WHERE employee_id = :userId
    AND child_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementGroupOfChildOfPedagogicalDocument() = DatabaseActionRule(
        this,
        Query<PedagogicalDocumentId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT pd.id, role
FROM pedagogical_document pd
JOIN child_acl_view ON pd.child_id = child_acl_view.child_id
WHERE employee_id = :userId
AND pd.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementGroupOfChildOfVasuDocument() = DatabaseActionRule(
        this,
        Query<VasuDocumentId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT curriculum_document.id AS id, role
FROM curriculum_document
JOIN child_acl_view ON curriculum_document.child_id = child_acl_view.child_id
WHERE employee_id = :userId
AND curriculum_document.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementGroupOfChildOfVasuDocumentFollowupEntry() = DatabaseActionRule(
        this,
        object : DatabaseActionRule.Query<VasuDocumentFollowupEntryId, HasGroupRole> {
            override fun execute(
                tx: Database.Read,
                user: AuthenticatedUser,
                targets: Set<VasuDocumentFollowupEntryId>
            ): Map<VasuDocumentFollowupEntryId, DatabaseActionRule.Deferred<HasGroupRole>> {
                val vasuDocuments =
                    inPlacementGroupOfChildOfVasuDocument().query.execute(tx, user, targets.map { it.first }.toSet())
                return targets.mapNotNull { target -> vasuDocuments[target.first]?.let { target to it } }.toMap()
            }
        }
    )
}
