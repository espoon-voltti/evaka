// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.test

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.ClosedPeriod
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun getApplicationStatus(h: Handle, applicationId: UUID): ApplicationStatus = h.createQuery(
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
    val unitId: UUID,
    val applicationId: UUID,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: DecisionStatus,
    val documentUri: String?,
    val requestedStartDate: LocalDate?,
    val resolved: Instant?,
    val resolvedBy: UUID?
) {
    fun period() = ClosedPeriod(startDate, endDate)
}

fun getDecisionRowsByApplication(h: Handle, applicationId: UUID) = h.createQuery(
    // language=SQL
    "SELECT * FROM decision WHERE application_id = :applicationId ORDER BY type"
).bind("applicationId", applicationId).mapTo<DecisionTableRow>()

fun getDecisionRowById(h: Handle, id: UUID) = h.createQuery(
    // language=SQL
    "SELECT * FROM decision WHERE id = :id"
).bind("id", id).mapTo<DecisionTableRow>()

data class PlacementTableRow(
    val id: UUID,
    val type: PlacementType,
    val childId: UUID,
    val unitId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun period() = ClosedPeriod(startDate, endDate)
}

fun getPlacementRowsByChild(h: Handle, childId: UUID) = h.createQuery(
    // language=SQL
    "SELECT * FROM placement WHERE child_id = :childId ORDER BY start_date"
).bind("childId", childId).mapTo<PlacementTableRow>()

data class PlacementPlanTableRow(
    val id: UUID,
    val type: PlacementType,
    val unitId: UUID,
    val applicationId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val preschoolDaycareStartDate: LocalDate?,
    val preschoolDaycareEndDate: LocalDate?,
    val preparatoryStartDate: LocalDate?,
    val preparatoryEndDate: LocalDate?,
    val deleted: Boolean
) {
    fun period() = ClosedPeriod(startDate, endDate)
    fun preschoolDaycarePeriod() =
        if (preschoolDaycareStartDate != null && preschoolDaycareEndDate != null) ClosedPeriod(
            preschoolDaycareStartDate,
            preschoolDaycareEndDate
        ) else null
}

fun getPlacementPlanRowByApplication(h: Handle, applicationId: UUID) = h.createQuery(
    // language=SQL
    "SELECT * FROM placement_plan WHERE application_id = :applicationId"
).bind("applicationId", applicationId).mapTo<PlacementPlanTableRow>()

data class BackupCareTableRow(
    val id: UUID,
    val childId: UUID,
    val unitId: UUID,
    val groupId: UUID?,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun period() = ClosedPeriod(startDate, endDate)
}

fun getBackupCareRowById(h: Handle, id: UUID) = h.createQuery(
    // language=SQL
    "SELECT * FROM backup_care WHERE id = :id"
).bind("id", id).mapTo<BackupCareTableRow>()

fun getBackupCareRowsByChild(h: Handle, childId: UUID) = h.createQuery(
    // language=SQL
    "SELECT * FROM backup_care WHERE child_id = :childId ORDER BY start_date"
).bind("childId", childId).mapTo<BackupCareTableRow>()
