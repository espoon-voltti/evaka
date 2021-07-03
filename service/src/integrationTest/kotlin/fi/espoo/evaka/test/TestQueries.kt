// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.test

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getApplicationStatus(applicationId: UUID): ApplicationStatus = createQuery(
    // language=SQL
    """
SELECT status
FROM application
WHERE id = :applicationId
"""
).bind("applicationId", applicationId)
    .mapTo<ApplicationStatus>()
    .one()

data class DecisionTableRow(
    val id: UUID,
    val number: Int,
    val createdBy: UUID,
    val sentDate: LocalDate,
    val unitId: DaycareId,
    val applicationId: UUID,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: DecisionStatus,
    val documentKey: String?,
    val requestedStartDate: LocalDate?,
    val resolved: Instant?,
    val resolvedBy: UUID?
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getDecisionRowsByApplication(applicationId: UUID) = createQuery(
    // language=SQL
    "SELECT * FROM decision WHERE application_id = :applicationId ORDER BY type"
).bind("applicationId", applicationId).mapTo<DecisionTableRow>()

fun Database.Read.getDecisionRowById(id: UUID) = createQuery(
    // language=SQL
    "SELECT * FROM decision WHERE id = :id"
).bind("id", id).mapTo<DecisionTableRow>()

data class PlacementTableRow(
    val id: UUID,
    val type: PlacementType,
    val childId: UUID,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getPlacementRowsByChild(childId: UUID) = createQuery(
    // language=SQL
    "SELECT * FROM placement WHERE child_id = :childId ORDER BY start_date"
).bind("childId", childId).mapTo<PlacementTableRow>()

data class PlacementPlanTableRow(
    val id: UUID,
    val type: PlacementType,
    val unitId: DaycareId,
    val applicationId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val preschoolDaycareStartDate: LocalDate?,
    val preschoolDaycareEndDate: LocalDate?,
    val preparatoryStartDate: LocalDate?,
    val preparatoryEndDate: LocalDate?,
    val deleted: Boolean
) {
    fun period() = FiniteDateRange(startDate, endDate)
    fun preschoolDaycarePeriod() =
        if (preschoolDaycareStartDate != null && preschoolDaycareEndDate != null) FiniteDateRange(
            preschoolDaycareStartDate,
            preschoolDaycareEndDate
        ) else null
}

fun Database.Read.getPlacementPlanRowByApplication(applicationId: UUID) = createQuery(
    // language=SQL
    "SELECT * FROM placement_plan WHERE application_id = :applicationId"
).bind("applicationId", applicationId).mapTo<PlacementPlanTableRow>()

data class BackupCareTableRow(
    val id: UUID,
    val childId: UUID,
    val unitId: DaycareId,
    val groupId: GroupId?,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getBackupCareRowById(id: UUID) = createQuery(
    // language=SQL
    "SELECT * FROM backup_care WHERE id = :id"
).bind("id", id).mapTo<BackupCareTableRow>()

fun Database.Read.getBackupCareRowsByChild(childId: UUID) = createQuery(
    // language=SQL
    "SELECT * FROM backup_care WHERE child_id = :childId ORDER BY start_date"
).bind("childId", childId).mapTo<BackupCareTableRow>()
