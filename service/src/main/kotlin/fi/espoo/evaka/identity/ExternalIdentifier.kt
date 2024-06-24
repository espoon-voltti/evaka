// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.identity

/** Persons identifier in external system. e.g. the system used to log in with */
sealed class ExternalIdentifier {
    data class SSN private constructor(
        val ssn: String
    ) : ExternalIdentifier() {
        companion object {
            fun getInstance(ssn: String): SSN {
                require(isValidSSN(ssn)) { "Invalid Social Security Number: $ssn." }
                return SSN(ssn)
            }
        }

        override fun toString(): String = ssn
    }

    object NoID : ExternalIdentifier()
}
