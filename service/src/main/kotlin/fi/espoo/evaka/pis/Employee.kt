// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.ForceCodeGenType
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.OffsetDateTime

data class Employee(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val externalId: ExternalId?,
    @ForceCodeGenType(OffsetDateTime::class)
    val created: HelsinkiDateTime,
    @ForceCodeGenType(OffsetDateTime::class)
    val updated: HelsinkiDateTime?
)
