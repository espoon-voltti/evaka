// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sensitive

import fi.espoo.evaka.attendance.ContactInfo
import fi.espoo.evaka.backuppickup.getBackupPickupsForChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.pis.controllers.fetchFamilyContacts
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.getCurrentPlacementForChild
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock

fun Database.Read.getChildBasicInfo(clock: EvakaClock, childId: ChildId): ChildBasicInformation? {
    val person = getPersonById(childId) ?: return null

    val placementType = getCurrentPlacementForChild(clock, childId)?.let { it.type }
    val child = getChild(childId)
    val backupPickups = getBackupPickupsForChild(childId)
    val familyContacts = fetchFamilyContacts(clock.today(), childId)

    return ChildBasicInformation(
        id = person.id,
        firstName = person.firstName,
        lastName = person.lastName,
        preferredName = child?.additionalInformation?.preferredName ?: "",
        dateOfBirth = person.dateOfBirth,
        placementType = placementType,
        contacts =
            familyContacts
                .filter { it.priority != null }
                .sortedBy { it.priority }
                .map {
                    ContactInfo(
                        id = it.id.toString(),
                        firstName = it.firstName,
                        lastName = it.lastName,
                        phone = it.phone,
                        backupPhone = it.backupPhone,
                        email = it.email ?: "",
                        priority = it.priority,
                    )
                },
        backupPickups =
            backupPickups.map {
                ContactInfo(
                    id = it.id.toString(),
                    firstName = it.name,
                    lastName = "",
                    phone = it.phone,
                    backupPhone = "",
                    email = "",
                    priority = 1,
                )
            },
    )
}

fun Database.Read.getChildSensitiveInfo(childId: ChildId): ChildSensitiveInformation? {
    val person = getPersonById(childId) ?: return null
    val child = getChild(childId)

    return ChildSensitiveInformation(
        id = person.id,
        ssn = person.identity.toString(),
        childAddress = person.streetAddress,
        allergies = child?.additionalInformation?.allergies ?: "",
        diet = child?.additionalInformation?.diet ?: "",
        medication = child?.additionalInformation?.medication ?: "",
        additionalInfo = child?.additionalInformation?.additionalInfo ?: "",
    )
}
