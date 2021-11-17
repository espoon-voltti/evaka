// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sensitive

import fi.espoo.evaka.attendance.ContactInfo
import fi.espoo.evaka.placement.PlacementType
import java.time.LocalDate
import java.util.UUID

data class ChildSensitiveInformation(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    val ssn: String,
    val childAddress: String,
    val placementTypes: List<PlacementType>,
    val allergies: String,
    val diet: String,
    val medication: String,
    val contacts: List<ContactInfo>,
    val backupPickups: List<ContactInfo>
)
