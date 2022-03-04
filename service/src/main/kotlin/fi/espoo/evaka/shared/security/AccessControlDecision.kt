// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.domain.Forbidden

sealed interface AccessControlDecision {
    /**
     * Unconditionally permitted to an admin without evaluating any rules
     */
    object PermittedToAdmin : AccessControlDecision

    /**
     * No decision was made, so action is denied by default
     */
    object None : AccessControlDecision

    /**
     * Action was explicitly permitted based on some rule
     */
    data class Permitted(val rule: Any) : AccessControlDecision

    /**
     * Action was explicitly denied based on some rule.
     *
     * This is only used to customize the Forbidden exception message/errorCode
     */
    data class Denied(val rule: Any, val message: String? = null, val errorCode: String? = null) : AccessControlDecision

    fun isPermitted(): Boolean = when (this) {
        is PermittedToAdmin -> true
        is Permitted -> true
        is Denied -> false
        is None -> false
    }
    fun assert() = when (this) {
        is PermittedToAdmin -> {}
        is Permitted -> {}
        is None -> throw Forbidden()
        is Denied -> throw Forbidden(this.message ?: "Permission denied", this.errorCode)
    }
}
