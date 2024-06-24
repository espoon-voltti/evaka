// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.domain.Forbidden

interface AccessControlDecision {
    /** Returns true if this decision permits the requested action. */
    fun isPermitted(): Boolean

    /** Throws an exception if this decision does not permit the requested action. */
    fun assert()

    /**
     * Throws an exception if this decision does not permit the requested action and the whole
     * decision checking procedure should be terminated.
     */
    fun assertIfTerminal()

    /** No decision was made, so action is denied by default */
    data object None : AccessControlDecision {
        override fun isPermitted(): Boolean = false

        override fun assert() = throw Forbidden()

        override fun assertIfTerminal() {}
    }

    /** Action was explicitly permitted based on some rule */
    data class Permitted(
        val rule: Any
    ) : AccessControlDecision {
        override fun isPermitted(): Boolean = true

        override fun assert() {}

        override fun assertIfTerminal() {}
    }
}
