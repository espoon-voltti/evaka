// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sensitive

import fi.espoo.evaka.attendance.ContactInfo
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import java.time.LocalDate

data class ChildBasicInformation(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    val placementType: PlacementType?,
    val contacts: List<ContactInfo>,
    val backupPickups: List<ContactInfo>,
)

data class ChildSensitiveInformation(
    val id: ChildId,
    val ssn: String,
    val childAddress: String,
    val allergies: String,
    val diet: String,
    val medication: String,
    val additionalInfo: String,
)
