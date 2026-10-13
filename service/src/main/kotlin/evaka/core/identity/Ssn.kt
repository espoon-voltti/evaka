// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.identity

/**
 * A valid Finnish social security number. [toString] masks the value, so an Ssn in a log line or
 * an error message does not reveal it.
 */
@JvmInline
value class Ssn private constructor(val value: String) {
    override fun toString(): String = "${value.take(6)}*****"

    companion object {
        fun parse(raw: String): Ssn? = if (isValidSSN(raw)) Ssn(raw) else null
    }
}
