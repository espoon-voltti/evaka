// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

enum class FamilyContactRole {
    LOCAL_GUARDIAN,
    LOCAL_ADULT,
    LOCAL_SIBLING,
    REMOTE_GUARDIAN
}

data class FamilyContact(
    val role: FamilyContactRole,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String
)
