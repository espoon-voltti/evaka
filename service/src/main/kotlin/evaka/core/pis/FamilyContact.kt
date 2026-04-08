// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.shared.PersonId

enum class FamilyContactRole {
    LOCAL_GUARDIAN,
    LOCAL_FOSTER_PARENT,
    LOCAL_ADULT,
    LOCAL_SIBLING,
    REMOTE_GUARDIAN,
    REMOTE_FOSTER_PARENT,
}

data class FamilyContact(
    val id: PersonId,
    val role: FamilyContactRole,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String,
    val backupPhone: String,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val priority: Int?,
)
