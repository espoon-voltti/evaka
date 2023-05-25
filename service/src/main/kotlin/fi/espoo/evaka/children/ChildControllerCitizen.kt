// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.Audit
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.getChildDailyServiceTimes
import fi.espoo.evaka.placement.getPlacementSummary
import fi.espoo.evaka.serviceneed.ServiceNeedOptionPublicInfo
import fi.espoo.evaka.serviceneed.ServiceNeedSummary
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.serviceneed.getServiceNeedSummary
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/children")
class ChildControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping
    fun getChildren(db: Database, user: AuthenticatedUser.Citizen, clock: EvakaClock): List<Child> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Person.READ_CHILDREN,
                        user.id
                    )
                    it.getChildrenByParent(user.id, clock.today())
                }
            }
            .also {
                Audit.CitizenChildrenRead.log(targetId = user.id, meta = mapOf("count" to it.size))
            }
    }

    @GetMapping("/{childId}/service-needs")
    fun getChildServiceNeeds(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<ServiceNeedSummary> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_SERVICE_NEEDS,
                        childId
                    )
                    val serviceNeeds = tx.getServiceNeedSummary(childId)
                    val missingServiceNeeds = getMissingServiceNeeds(tx, childId, serviceNeeds)
                    serviceNeeds + missingServiceNeeds
                }
            }
            .also { Audit.CitizenChildServiceNeedRead.log(targetId = childId) }
    }

    private fun getMissingServiceNeeds(
        tx: Database.Read,
        childId: ChildId,
        serviceNeeds: List<ServiceNeedSummary>
    ): List<ServiceNeedSummary> {
        val defaultServiceNeedOptions =
            tx.getServiceNeedOptions()
                .filter { it.defaultOption }
                .associateBy({ it.validPlacementType }, { ServiceNeedOptionPublicInfo.of(it) })
        val serviceNeedDateRanges = serviceNeeds.map { FiniteDateRange(it.startDate, it.endDate) }
        return tx.getPlacementSummary(childId).flatMap { placement ->
            val placementRange = FiniteDateRange(placement.startDate, placement.endDate)
            placementRange.complement(serviceNeedDateRanges).map {
                ServiceNeedSummary(
                    it.start,
                    it.end,
                    defaultServiceNeedOptions[placement.type],
                    placement.unit.name
                )
            }
        }
    }

    @GetMapping("/{childId}/daily-service-times")
    fun getChildDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<DailyServiceTimes> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_DAILY_SERVICE_TIMES,
                        childId
                    )
                    tx.getChildDailyServiceTimes(childId)
                }
            }
            .also { Audit.CitizenChildDailyServiceTimeRead.log(targetId = childId) }
    }
}
