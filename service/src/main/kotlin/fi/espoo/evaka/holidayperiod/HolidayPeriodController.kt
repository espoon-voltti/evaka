// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.deleteAllCitizenEditableAbsencesInRange
import fi.espoo.evaka.reservations.deleteAllCitizenReservationsInRange
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.Period
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    "/holiday-period", // deprecated
    "/employee/holiday-period",
)
class HolidayPeriodController(private val accessControl: AccessControl) {
    @GetMapping
    fun getHolidayPeriods(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<HolidayPeriod> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_HOLIDAY_PERIODS,
                    )
                    it.getHolidayPeriods()
                }
            }
            .also { Audit.HolidayPeriodsList.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/{id}")
    fun getHolidayPeriod(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: HolidayPeriodId,
    ): HolidayPeriod {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_HOLIDAY_PERIOD,
                    )
                    it.getHolidayPeriod(id)
                } ?: throw NotFound()
            }
            .also { Audit.HolidayPeriodRead.log(targetId = AuditId(id)) }
    }

    @PostMapping
    fun createHolidayPeriod(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: HolidayPeriodCreate,
    ): HolidayPeriod {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.CREATE_HOLIDAY_PERIOD,
                    )
                    try {
                        if (body.period.start.isBefore(clock.today().plusWeeks(4))) {
                            throw BadRequest("Holiday period must start at least 4 weeks from now")
                        }
                        if (Period.between(body.period.start, body.period.end).days > 8 * 7) {
                            throw BadRequest("Holiday period must be at most 8 weeks long")
                        }
                        if (body.reservationsOpenOn > body.period.start) {
                            throw BadRequest(
                                "Reservations open must be before holiday period starts"
                            )
                        }
                        if (
                            !(body.reservationDeadline <= body.reservationDeadline &&
                                body.reservationDeadline <= body.period.start)
                        ) {
                            throw BadRequest(
                                "Reservation deadline must be between reservations open and holiday period start"
                            )
                        }

                        it.deleteAllCitizenReservationsInRange(body.period)
                        it.deleteAllCitizenEditableAbsencesInRange(body.period)
                        it.insertHolidayPeriod(
                            body.period,
                            body.reservationsOpenOn,
                            body.reservationDeadline,
                        )
                    } catch (e: Exception) {
                        throw mapPSQLException(e)
                    }
                }
            }
            .also { holidayPeriod ->
                Audit.HolidayPeriodCreate.log(targetId = AuditId(holidayPeriod.id))
            }
    }

    @PutMapping("/{id}")
    fun updateHolidayPeriod(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: HolidayPeriodId,
        @RequestBody body: HolidayPeriodUpdate,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.UPDATE_HOLIDAY_PERIOD,
                )
                try {
                    it.updateHolidayPeriod(
                        id = id,
                        reservationsOpenOn = body.reservationsOpenOn,
                        reservationDeadline = body.reservationDeadline,
                    )
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
        Audit.HolidayPeriodUpdate.log(targetId = AuditId(id))
    }

    @DeleteMapping("/{id}")
    fun deleteHolidayPeriod(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: HolidayPeriodId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.DELETE_HOLIDAY_PERIOD,
                )
                it.deleteHolidayPeriod(id)
            }
        }
        Audit.HolidayPeriodDelete.log(targetId = AuditId(id))
    }
}
