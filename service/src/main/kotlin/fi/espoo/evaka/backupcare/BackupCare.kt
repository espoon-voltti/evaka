// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import java.time.LocalDate
import java.util.UUID

data class ChildBackupCare(
    val id: UUID,
    @Nested("unit_") val unit: BackupCareUnit,
    @Nested("group_")
    val group: BackupCareGroup?,
    val period: FiniteDateRange
)

data class UnitBackupCare(
    val id: UUID,
    @Nested("child_") val child: BackupCareChild,
    @Nested("group_")
    val group: BackupCareGroup?,
    val period: FiniteDateRange,
    val missingServiceNeedDays: Int
)

data class GroupBackupCare(
    val id: UUID,
    val childId: UUID,
    val period: FiniteDateRange
)

data class BackupCareChild(val id: UUID, val firstName: String, val lastName: String, val birthDate: LocalDate)
data class BackupCareUnit(val id: UUID, val name: String)
@PropagateNull("group_id")
data class BackupCareGroup(val id: GroupId, val name: String)
data class NewBackupCare(val unitId: UUID, val groupId: GroupId?, val period: FiniteDateRange)
