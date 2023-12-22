// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.identity

import java.time.LocalDate

const val SSN_PATTERN = "^(\\d{2})(\\d{2})(\\d{2})[-+ABCDEFUVWXY](\\d{3})[\\dA-Z]$"
private val CHECK_DIGITS =
    arrayOf(
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'A',
        'B',
        'C',
        'D',
        'E',
        'F',
        'H',
        'J',
        'K',
        'L',
        'M',
        'N',
        'P',
        'R',
        'S',
        'T',
        'U',
        'V',
        'W',
        'X',
        'Y'
    )

private fun checkDigitMatches(ssn: String): Boolean {
    val birthNumber = ssn.substring(0, 6)
    val identityNumber = ssn.substring(7, 10)
    val checkDigit = CHECK_DIGITS[(birthNumber + identityNumber).toInt() % 31]
    return ssn[10] == checkDigit
}

fun isValidSSN(ssn: String?): Boolean {
    return (ssn != null &&
        SSN_PATTERN.toRegex().matches(ssn) &&
        containsValidDate(ssn) &&
        checkDigitMatches(ssn))
}

private fun containsValidDate(ssn: String): Boolean =
    try {
        getDobFromSsn(ssn)
        true
    } catch (e: Exception) {
        false
    }

fun getDobFromSsn(ssn: String): LocalDate {
    val year = getYearFromSSN(ssn)
    val month = ssn.substring(2, 4).toInt()
    val day = ssn.substring(0, 2).toInt()
    return LocalDate.of(year, month, day)
}

fun getYearFromSSN(ssn: String): Int {
    val century =
        when (val c = ssn[6]) {
            'A',
            'B',
            'C',
            'D',
            'E',
            'F' -> 2000
            '-',
            'U',
            'V',
            'W',
            'X',
            'Y' -> 1900
            '+' -> 1800
            else -> throw java.lang.IllegalArgumentException("Invalid century in SSN ('$c')")
        }
    return century + ssn.substring(4, 6).toInt()
}
