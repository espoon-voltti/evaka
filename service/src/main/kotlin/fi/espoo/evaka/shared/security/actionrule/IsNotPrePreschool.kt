// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.assistanceaction.getAssistanceActionById
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import java.time.LocalDate

object IsNotPrePreschool {
    private data class Query<T>(private val f: (Database.Read, T) -> Pair<ChildId, LocalDate>) :
        DatabaseActionRule.Query<T, IsNotPrePreschool> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsNotPrePreschool>> = when (user) {
            is AuthenticatedUser.Employee -> {
                targets.filter { target ->
                    val (child, startDate) = f(tx, target)
                    val preschoolPlacements = tx.getPlacementsForChild(child).filter {
                        (it.type == PlacementType.PRESCHOOL || it.type == PlacementType.PRESCHOOL_DAYCARE)
                    }
                    preschoolPlacements.isEmpty() || preschoolPlacements.any { placement ->
                        placement.startDate.isBefore(startDate) || placement.startDate == startDate
                    }
                }.associateWith { Deferred }
            }
            else -> emptyMap()
        }
    }
    private object Deferred : DatabaseActionRule.Deferred<IsNotPrePreschool> {
        override fun evaluate(params: IsNotPrePreschool): AccessControlDecision = AccessControlDecision.Permitted(params)
    }
    val assistanceAction = DatabaseActionRule(
        this,
        Query<AssistanceActionId> { tx, id ->
            tx.getAssistanceActionById(id).let { Pair(it.childId, it.startDate) }
        }
    )
}
