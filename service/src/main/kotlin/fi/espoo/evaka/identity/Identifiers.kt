// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.identity

import java.util.UUID

/**
 * Voltti wide natural person identifier
 */
typealias VolttiIdentifier = UUID

/**
 * Persons identifier in external system. e.g. the system used
 * to log in with
 */
sealed class ExternalIdentifier {
    class SSN private constructor(val ssn: String) : ExternalIdentifier() {
        companion object {
            fun getInstance(ssn: String): SSN {
                require(isValidSSN(ssn)) { "Invalid Social Security Number: $ssn." }
                return SSN(ssn)
            }
        }

        override fun toString(): String = ssn

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SSN

            if (ssn != other.ssn) return false

            return true
        }

        override fun hashCode(): Int {
            return ssn.hashCode()
        }
    }

    class NoID : ExternalIdentifier()
}
