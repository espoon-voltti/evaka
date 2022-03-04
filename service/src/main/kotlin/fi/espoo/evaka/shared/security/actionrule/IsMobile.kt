// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.MobileAuthLevel

data class IsMobile(val requirePinLogin: Boolean) : StaticActionRule, ActionRuleParams<IsMobile> {
    override fun isPermitted(user: AuthenticatedUser): Boolean = when (user) {
        is AuthenticatedUser.MobileDevice -> if (requirePinLogin) {
            user.authLevel == MobileAuthLevel.PIN_LOGIN
        } else true
        else -> false
    }

    override fun merge(other: IsMobile): IsMobile = IsMobile(this.requirePinLogin && other.requirePinLogin)
}
