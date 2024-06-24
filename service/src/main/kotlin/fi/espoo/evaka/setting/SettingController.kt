// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.setting

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    "/settings", // deprecated
    "/employee/settings"
)
class SettingController(
    private val accessControl: AccessControl
) {
    @GetMapping
    fun getSettings(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): Map<SettingType, String> =
        db
            .connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.UPDATE_SETTINGS
                    )
                    tx.getSettings()
                }
            }.also { Audit.SettingsRead.log() }

    @PutMapping
    fun putSettings(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody settings: Map<SettingType, String>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Global.UPDATE_SETTINGS)
                tx.setSettings(settings)
            }
        }
        Audit.SettingsUpdate.log()
    }
}
