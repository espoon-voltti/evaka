// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.pis.controllers.PinCode
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigDecimal

data class Employee(
    val id: EmployeeId,
    val preferredFirstName: String?,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val externalId: ExternalId?,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime?,
    val temporaryInUnitId: DaycareId?
)

data class TemporaryEmployee(
    val firstName: String,
    val lastName: String,
    val groupIds: Set<GroupId>,
    val occupancyCoefficient: BigDecimal,
    val pinCode: PinCode?,
)
