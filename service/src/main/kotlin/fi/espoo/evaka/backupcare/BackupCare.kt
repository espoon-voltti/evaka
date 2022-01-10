// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.serviceneed.ServiceNeed
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
import java.time.LocalDate

data class ChildBackupCare(
    val id: BackupCareId,
    @Nested("unit_") val unit: BackupCareUnit,
    @Nested("group_")
    val group: BackupCareGroup?,
    val period: FiniteDateRange
)

data class UnitBackupCare(
    val id: BackupCareId,
    @Nested("child_") val child: BackupCareChild,
    @Nested("group_")
    val group: BackupCareGroup?,
    val period: FiniteDateRange,
    @Json
    val serviceNeeds: Set<ServiceNeed>,
    val missingServiceNeedDays: Int
)

data class GroupBackupCare(
    val id: BackupCareId,
    val childId: ChildId,
    val period: FiniteDateRange
)

data class BackupCareChild(val id: ChildId, val firstName: String, val lastName: String, val birthDate: LocalDate)
data class BackupCareUnit(val id: DaycareId, val name: String)
@PropagateNull("group_id")
data class BackupCareGroup(val id: GroupId, val name: String)
data class NewBackupCare(val unitId: DaycareId, val groupId: GroupId?, val period: FiniteDateRange)
