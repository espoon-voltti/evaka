// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.auth.AuthenticatedUser

/**
 * A rule that grants permission based on an `AuthenticatedUser`, without needing any additional information
 */
interface StaticActionRule {
    fun isPermitted(user: AuthenticatedUser): Boolean
}
