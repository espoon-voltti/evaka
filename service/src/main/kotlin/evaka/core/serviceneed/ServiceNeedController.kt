// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.serviceneed

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.AuditId
import evaka.core.absence.ChildServiceNeedInfo
import evaka.core.placement.PlacementType
import evaka.core.shared.ChildId
import evaka.core.shared.PlacementId
import evaka.core.shared.ServiceNeedId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceNeedController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {

    data class ServiceNeedCreateRequest(
        val placementId: PlacementId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val optionId: ServiceNeedOptionId,
        val shiftCare: ShiftCareType,
        val partWeek: Boolean,
    )

    @PostMapping("/employee/service-needs")
    fun postServiceNeed(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ServiceNeedCreateRequest,
    ) {
        val audit =
            AuditContext().add(body.placementId).add(body.optionId).observeDate(body.startDate)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Placement.CREATE_SERVICE_NEED,
                        body.placementId,
                    )

                    createServiceNeed(
                            tx = tx,
                            user = user,
                            placementId = body.placementId,
                            startDate = body.startDate,
                            endDate = body.endDate,
                            optionId = body.optionId,
                            shiftCare = body.shiftCare,
                            partWeek = body.partWeek,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                        .also { id ->
                            val range = tx.getServiceNeedChildRange(id)
                            audit.add(id).add(range.childId)
                            notifyServiceNeedUpdated(tx, clock, asyncJobRunner, range)
                        }
                }
            }
            .also { audit.log(Audit.PlacementServiceNeedCreate, clock) }
    }

    data class ServiceNeedUpdateRequest(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val optionId: ServiceNeedOptionId,
        val shiftCare: ShiftCareType,
        val partWeek: Boolean,
    )

    @PutMapping("/employee/service-needs/{id}")
    fun putServiceNeed(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ServiceNeedId,
        @RequestBody body: ServiceNeedUpdateRequest,
    ) {
        val audit = AuditContext().add(id).add(body.optionId).observeDate(body.startDate)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ServiceNeed.UPDATE,
                        id,
                    )

                    val oldRange = tx.getServiceNeedChildRange(id)
                    audit.add(oldRange.childId)
                    updateServiceNeed(
                        tx = tx,
                        user = user,
                        id = id,
                        startDate = body.startDate,
                        endDate = body.endDate,
                        optionId = body.optionId,
                        shiftCare = body.shiftCare,
                        partWeek = body.partWeek,
                        confirmedAt = HelsinkiDateTime.now(),
                        audit = audit,
                    )
                    notifyServiceNeedUpdated(
                        tx,
                        clock,
                        asyncJobRunner,
                        ServiceNeedChildRange(
                            childId = oldRange.childId,
                            dateRange =
                                FiniteDateRange(
                                    minOf(oldRange.dateRange.start, body.startDate),
                                    maxOf(oldRange.dateRange.end, body.endDate),
                                ),
                        ),
                    )
                }
            }
            .also { audit.log(Audit.PlacementServiceNeedUpdate, clock) }
    }

    @DeleteMapping("/employee/service-needs/{id}")
    fun deleteServiceNeed(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ServiceNeedId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.ServiceNeed.DELETE, id)

                val childRange = tx.getServiceNeedChildRange(id)
                tx.deleteServiceNeed(id)
                notifyServiceNeedUpdated(tx, clock, asyncJobRunner, childRange)
            }
        }
        Audit.PlacementServiceNeedDelete.log(targetId = AuditId(id))
    }

    @GetMapping("/employee/service-needs/options")
    fun getServiceNeedOptions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<ServiceNeedOption> {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_SERVICE_NEED_OPTIONS,
                    )
                    tx.getServiceNeedOptions().also { audit.addMeta("count", it.size) }
                }
            }
            .also { audit.log(Audit.ServiceNeedOptionsRead, clock) }
    }

    @GetMapping(
        path = ["/citizen/public/service-needs/options", "/employee/public/service-needs/options"]
    )
    fun getServiceNeedOptionPublicInfos(
        db: Database,
        @RequestParam placementTypes: List<PlacementType> = emptyList(),
    ): List<ServiceNeedOptionPublicInfo> {
        return db.connect { dbc -> dbc.read { it.getServiceNeedOptionPublicInfos(placementTypes) } }
    }

    @GetMapping("/employee/children/{childId}/service-needs")
    fun getChildServiceNeeds(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestParam from: LocalDate,
    ): List<ChildServiceNeedInfo> {
        val audit = AuditContext().add(childId).observeDate(from)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_SERVICE_NEEDS,
                        childId,
                    )
                    tx.getChildServiceNeedInfos(childId, from)
                }
            }
            .also { audit.log(Audit.ChildServiceNeedsRead, clock) }
    }
}
