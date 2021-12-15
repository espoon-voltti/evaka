// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.setting

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/settings")
class SettingController(private val accessControl: AccessControl) {

    @GetMapping
    fun getSettings(db: Database.DeprecatedConnection, user: AuthenticatedUser): Map<SettingType, String> {
        Audit.SettingsRead.log()
        accessControl.requirePermissionFor(user, Action.Global.UPDATE_SETTINGS)
        return db.read { tx -> tx.getSettings() }
    }

    @PutMapping
    fun setSettings(db: Database.DeprecatedConnection, user: AuthenticatedUser, @RequestBody settings: Map<SettingType, String>) {
        Audit.SettingsUpdate.log()
        accessControl.requirePermissionFor(user, Action.Global.UPDATE_SETTINGS)
        return db.transaction { tx -> tx.setSettings(settings) }
    }
}
