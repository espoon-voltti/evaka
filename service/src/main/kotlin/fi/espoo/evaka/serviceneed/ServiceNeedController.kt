// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
        val serviceNeedId =
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
                            notifyServiceNeedUpdated(tx, clock, asyncJobRunner, range)
                        }
                }
            }
        Audit.PlacementServiceNeedCreate.log(
            targetId = AuditId(body.placementId),
            objectId = AuditId(serviceNeedId),
        )
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
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.ServiceNeed.UPDATE, id)

                val oldRange = tx.getServiceNeedChildRange(id)
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
        Audit.PlacementServiceNeedUpdate.log(targetId = AuditId(id))
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
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_SERVICE_NEED_OPTIONS,
                    )
                    it.getServiceNeedOptions()
                }
            }
            .also { Audit.ServiceNeedOptionsRead.log(meta = mapOf("count" to it.size)) }
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
}
