// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.auth.AuthenticatedUser

data class IsCitizen(val allowWeakLogin: Boolean) : StaticActionRule, ActionRuleParams<IsCitizen> {
    override fun isPermitted(user: AuthenticatedUser): Boolean = when (user) {
        is AuthenticatedUser.Citizen -> true
        is AuthenticatedUser.WeakCitizen -> allowWeakLogin
        else -> false
    }

    override fun merge(other: IsCitizen): IsCitizen = IsCitizen(this.allowWeakLogin || other.allowWeakLogin)
}
