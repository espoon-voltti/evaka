// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

fun formatName(
    firstName: String?,
    lastName: String?,
    lastNameFirst: Boolean? = false
): String =
    if (lastNameFirst == true) {
        listOfNotNull(lastName, firstName).joinToString(" ")
    } else {
        listOfNotNull(firstName, lastName).joinToString(" ")
    }
