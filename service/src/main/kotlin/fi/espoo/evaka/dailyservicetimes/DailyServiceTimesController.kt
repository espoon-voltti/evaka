// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DailyServiceTimesController(
    private val accessControl: AccessControl
) {

    data class DailyServiceTimesResponse(
        val dailyServiceTimes: DailyServiceTimesWithId,
        val permittedActions: Set<Action.DailyServiceTime>
    )

    @GetMapping("/children/{childId}/daily-service-times")
    fun getDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): List<DailyServiceTimesResponse> {
        Audit.ChildDailyServiceTimesRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_DAILY_SERVICE_TIMES, childId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getChildDailyServiceTimes(childId).map {
                    DailyServiceTimesResponse(
                        it,
                        permittedActions = accessControl.getPermittedActions(
                            tx, user, it.id
                        )
                    )
                }
            }
        }
    }

    @PostMapping("/children/{childId}/daily-service-times")
    fun postDailyServiceTimes(
        db: Database,
        evakaClock: EvakaClock,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestBody body: DailyServiceTimes
    ) {
        Audit.ChildDailyServiceTimesEdit.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_DAILY_SERVICE_TIME, childId)

        if (body.validityPeriod.start.isBefore(evakaClock.today())) {
            throw BadRequest("New daily service times cannot be added in the past")
        }

        db.connect { dbc ->
            dbc.transaction { tx ->
                this.checkOverlappingDailyServiceTimes(tx, childId, body.validityPeriod)
                tx.createChildDailyServiceTimes(childId, body)
            }
        }
    }

    @PutMapping("/daily-service-times/{id}")
    fun putDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: DailyServiceTimesId,
        @RequestBody body: DailyServiceTimes
    ) {
        Audit.ChildDailyServiceTimesEdit.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.DailyServiceTime.UPDATE, id)

        db.connect { dbc -> dbc.transaction { it.updateChildDailyServiceTimes(id, body) } }
    }

    @DeleteMapping("/daily-service-times/{id}")
    fun deleteDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: DailyServiceTimesId
    ) {
        Audit.ChildDailyServiceTimesDelete.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.DailyServiceTime.DELETE, id)

        db.connect { dbc -> dbc.transaction { it.deleteChildDailyServiceTimes(id) } }
    }

    private fun checkOverlappingDailyServiceTimes(
        tx: Database.Transaction,
        childId: ChildId,
        range: DateRange
    ) {
        val overlapping = tx.getOverlappingChildDailyServiceTimes(childId, range)

        if (overlapping.isNotEmpty()) {
            overlapping.forEach {
                if (range.start <= it.validityPeriod.start && (range.end == null || (it.validityPeriod.end != null && range.end >= it.validityPeriod.end))) {
                    tx.deleteChildDailyServiceTimes(it.id)
                } else {
                    tx.updateChildDailyServiceTimesValidity(
                        id = it.id,
                        if (it.validityPeriod.end == null || it.validityPeriod.end >= range.start)
                            DateRange(it.validityPeriod.start, range.start.minusDays(1))
                        else if (range.end != null && it.validityPeriod.start <= range.end)
                            DateRange(range.end.plusDays(1), it.validityPeriod.end)
                        else
                            throw IllegalStateException("Unsupported overlap")
                    )
                }
            }
        }
    }
}
