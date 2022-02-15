// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/holiday-period")
class HolidayPeriodController(private val accessControl: AccessControl) {
    @GetMapping
    fun getHolidayPeriods(
        db: Database,
        user: AuthenticatedUser,
    ): List<HolidayPeriod> {
        Audit.HolidayPeriodsList.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_HOLIDAY_PERIODS)
        return db.connect { dbc -> dbc.read { it.getHolidayPeriods() } }
    }

    @GetMapping("/{id}")
    fun getHolidayPeriod(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: HolidayPeriodId,
    ): HolidayPeriod {
        Audit.HolidayPeriodRead.log(id)
        accessControl.requirePermissionFor(user, Action.Global.READ_HOLIDAY_PERIOD)
        return db.connect { dbc -> dbc.read { it.getHolidayPeriod(id) } } ?: throw NotFound()
    }

    @PostMapping
    fun createHolidayPeriod(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: HolidayPeriodBody
    ): HolidayPeriod {
        Audit.HolidayPeriodCreate.log()
        accessControl.requirePermissionFor(user, Action.Global.CREATE_HOLIDAY_PERIOD)
        return db.connect { dbc ->
            dbc.transaction {
                try {
                    it.createHolidayPeriod(body)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    @PutMapping("/{id}")
    fun updateHolidayPeriod(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: HolidayPeriodId,
        @RequestBody body: HolidayPeriodBody
    ) {
        Audit.HolidayPeriodUpdate.log(id)
        accessControl.requirePermissionFor(user, Action.Global.UPDATE_HOLIDAY_PERIOD)
        db.connect { dbc ->
            dbc.transaction {
                try {
                    it.updateHolidayPeriod(id, body)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    @DeleteMapping("/{id}")
    fun deleteHolidayPeriod(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: HolidayPeriodId
    ) {
        Audit.HolidayPeriodDelete.log(id)
        accessControl.requirePermissionFor(user, Action.Global.DELETE_HOLIDAY_PERIOD)
        db.connect { dbc -> dbc.transaction { it.deleteHolidayPeriod(id) } }
    }
}
