//  SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive

/** Structural constraints for a password */
data class PasswordConstraints(
    /** Minimum length in characters */
    val minLength: Int,
    /** Maximum length in characters */
    val maxLength: Int,
    /** Minimum number of lowercase characters */
    val minLowers: Int,
    /** Minimum number of uppercase characters */
    val minUppers: Int,
    /** Minimum number of digit characters */
    val minDigits: Int,
    /** Minimum number of symbol characters */
    val minSymbols: Int,
) {
    init {
        require(minLength in SUPPORTED_LENGTH)
        require(maxLength in SUPPORTED_LENGTH)
        require(minLength <= maxLength)
        require(minLowers >= 0)
        require(minUppers >= 0)
        require(minDigits >= 0)
        require(minSymbols >= 0)
    }

    /** Returns true if the given password is structurally valid based on these constraints */
    fun isPasswordStructureValid(password: Sensitive<String>): Boolean {
        if (password.value.length !in minLength..maxLength) {
            return false
        }
        if (password.value.count { it.isLowerCase() } < minLowers) {
            return false
        }
        if (password.value.count { it.isUpperCase() } < minUppers) {
            return false
        }
        if (password.value.count { it.isDigit() } < minDigits) {
            return false
        }
        if (password.value.count { !it.isLetterOrDigit() } < minSymbols) {
            return false
        }
        return true
    }

    companion object {
        /**
         * Min/max password length supported by the system.
         *
         * This is not changeable, because it's a technical constraint
         */
        val SUPPORTED_LENGTH = 1..128

        val UNCONSTRAINED =
            PasswordConstraints(
                minLength = SUPPORTED_LENGTH.first,
                maxLength = SUPPORTED_LENGTH.last,
                minLowers = 0,
                minUppers = 0,
                minDigits = 0,
                minSymbols = 0,
            )
    }
}
