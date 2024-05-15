// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.generateAbsencesFromIrregularDailyServiceTimes
import fi.espoo.evaka.reservations.clearReservationsForRangeExceptInHolidayPeriod
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DailyServiceTimesController(private val accessControl: AccessControl) {

    data class DailyServiceTimesResponse(
        val dailyServiceTimes: DailyServiceTimes,
        val permittedActions: Set<Action.DailyServiceTime>
    )

    @GetMapping(
        "" + "/children/{childId}/daily-service-times", // deprecated
        "/employee/children/{childId}/daily-service-times"
    )
    fun getDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<DailyServiceTimesResponse> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_DAILY_SERVICE_TIMES,
                        childId
                    )
                    tx.getChildDailyServiceTimes(childId).map {
                        DailyServiceTimesResponse(
                            it,
                            permittedActions =
                                accessControl.getPermittedActions(tx, user, clock, it.id)
                        )
                    }
                }
            }
            .also {
                Audit.ChildDailyServiceTimesRead.log(
                    targetId = childId,
                    meta = mapOf("count" to it.size)
                )
            }
    }

    @PostMapping(
        "/children/{childId}/daily-service-times", // deprecated
        "/employee/children/{childId}/daily-service-times"
    )
    fun postDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: DailyServiceTimesValue
    ) {
        val now = clock.now()
        val today = now.toLocalDate()

        if (body.validityPeriod.start <= today) {
            throw BadRequest(
                "New daily service times cannot be added if their validity starts on or before today"
            )
        }

        val id =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_DAILY_SERVICE_TIME,
                        childId
                    )
                    updateOverlappingDailyServiceTimes(tx, childId, body.validityPeriod)
                    val id = tx.createChildDailyServiceTimes(childId, body)
                    deleteCollidingReservationsAndNotify(
                        tx,
                        today,
                        id,
                        childId,
                        body.validityPeriod
                    )
                    generateAbsencesFromIrregularDailyServiceTimes(tx, now, childId)
                    id
                }
            }
        Audit.ChildDailyServiceTimesEdit.log(targetId = childId, objectId = id)
    }

    @PutMapping(
        "/daily-service-times/{id}", // deprecated
        "/employee/daily-service-times/{id}"
    )
    fun putDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DailyServiceTimesId,
        @RequestBody body: DailyServiceTimesValue
    ) {
        val now = clock.now()
        val today = now.toLocalDate()

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DailyServiceTime.UPDATE,
                    id
                )
                val old = tx.getDailyServiceTimesValidity(id) ?: throw NotFound()
                if (old.validityPeriod.start <= today) {
                    throw BadRequest(
                        "Daily service times end cannot be updated if their validity has started"
                    )
                }
                if (body.validityPeriod.start <= today) {
                    throw BadRequest(
                        "Daily service times start cannot be updated to start on or before today"
                    )
                }
                val overlapping =
                    tx.getOverlappingChildDailyServiceTimes(old.childId, body.validityPeriod)
                        .filter { it.id != id }
                if (overlapping.isNotEmpty()) {
                    throw BadRequest(
                        "Daily service times cannot be updated because they overlap with other daily service times"
                    )
                }

                tx.updateChildDailyServiceTimes(id, body)
                deleteCollidingReservationsAndNotify(
                    tx,
                    today,
                    id,
                    old.childId,
                    body.validityPeriod
                )
                generateAbsencesFromIrregularDailyServiceTimes(tx, now, old.childId)
            }
        }
        Audit.ChildDailyServiceTimesEdit.log(targetId = id)
    }

    data class DailyServiceTimesEndDate(val endDate: LocalDate?)

    @PutMapping(
        "/daily-service-times/{id}/end", // deprecated
        "/employee/daily-service-times/{id}/end"
    )
    fun putDailyServiceTimesEnd(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DailyServiceTimesId,
        @RequestBody body: DailyServiceTimesEndDate
    ) {
        val now = clock.now()
        val today = now.toLocalDate()

        if (body.endDate != null && body.endDate <= today) {
            throw BadRequest("Daily service times end cannot be changed to end on or before today")
        }

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DailyServiceTime.UPDATE,
                    id
                )
                val old = tx.getDailyServiceTimesValidity(id) ?: throw NotFound()
                if ((old.validityPeriod.end ?: LocalDate.MAX) < today) {
                    throw BadRequest(
                        "Daily service times end cannot be updated if their validity has ended"
                    )
                }
                val newValidity = old.validityPeriod.copy(end = body.endDate)

                val overlapping =
                    tx.getOverlappingChildDailyServiceTimes(old.childId, newValidity).filter {
                        it.id != id
                    }
                if (overlapping.isNotEmpty()) {
                    throw BadRequest(
                        "Daily service times cannot be updated because they overlap with other daily service times"
                    )
                }

                tx.updateChildDailyServiceTimesValidity(id, newValidity)

                val oldEnd = old.validityPeriod.end
                when {
                    // end date is removed
                    oldEnd != null && newValidity.end == null -> DateRange(oldEnd.plusDays(1), null)
                    // end date is moved later
                    oldEnd != null && newValidity.end != null && newValidity.end > oldEnd ->
                        DateRange(oldEnd.plusDays(1), newValidity.end)
                    else -> null
                }?.let { changePeriod ->
                    deleteCollidingReservationsAndNotify(
                        tx = tx,
                        today = today,
                        id = id,
                        childId = old.childId,
                        validityPeriod = changePeriod
                    )
                }

                generateAbsencesFromIrregularDailyServiceTimes(tx, now, old.childId)
            }
        }
        Audit.ChildDailyServiceTimesEdit.log(targetId = id)
    }

    @DeleteMapping(
        "/daily-service-times/{id}", // deprecated
        "/employee/daily-service-times/{id}"
    )
    fun deleteDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DailyServiceTimesId
    ) {
        val now = clock.now()
        val today = now.toLocalDate()

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DailyServiceTime.DELETE,
                    id
                )
                val old = tx.getDailyServiceTimesValidity(id) ?: throw NotFound()
                if (old.validityPeriod.start <= today) {
                    throw BadRequest(
                        "Daily service times end cannot be deleted if their validity has started"
                    )
                }
                tx.deleteChildDailyServiceTimes(id)
                generateAbsencesFromIrregularDailyServiceTimes(tx, clock.now(), old.childId)
            }
        }

        Audit.ChildDailyServiceTimesDelete.log(targetId = id)
    }

    private fun updateOverlappingDailyServiceTimes(
        tx: Database.Transaction,
        childId: ChildId,
        new: DateRange
    ) {
        val overlapping = tx.getOverlappingChildDailyServiceTimes(childId, new)
        overlapping.forEach { (oldId, old) ->
            if (new.contains(old)) {
                tx.deleteChildDailyServiceTimes(oldId)
            } else {
                val updatedRange =
                    when {
                        old.contains(new) &&
                            new.start != old.start &&
                            old.end != null &&
                            new.end != old.end -> throw Conflict("Unsupported overlap")
                        new.start <= old.start -> DateRange(new.end!!.plusDays(1), old.end)
                        old.start < new.start -> DateRange(old.start, new.start.minusDays(1))
                        else -> throw Conflict("Unsupported overlap")
                    }
                tx.updateChildDailyServiceTimesValidity(oldId, updatedRange)
            }
        }
    }

    private fun deleteCollidingReservationsAndNotify(
        tx: Database.Transaction,
        today: LocalDate,
        id: DailyServiceTimesId,
        childId: ChildId,
        validityPeriod: DateRange
    ) {
        if ((validityPeriod.end ?: LocalDate.MAX) <= today)
            throw Error("Unexpected validity period")

        val actionableRange =
            DateRange(
                validityPeriod.start.takeIf { it > today } ?: today.plusDays(1),
                validityPeriod.end
            )

        val deletedReservationCount =
            tx.clearReservationsForRangeExceptInHolidayPeriod(childId, actionableRange)
        tx.addDailyServiceTimesNotification(
            today,
            id,
            childId,
            actionableRange.start,
            deletedReservationCount > 0
        )
    }
}
