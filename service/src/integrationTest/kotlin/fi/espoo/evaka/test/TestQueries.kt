// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.test

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.PlacementPlanId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.Instant
import java.time.LocalDate

fun Database.Read.getApplicationStatus(applicationId: ApplicationId): ApplicationStatus =
    @Suppress("DEPRECATION")
    createQuery(
        // language=SQL
        """
SELECT status
FROM application
WHERE id = :applicationId
"""
    ).bind("applicationId", applicationId)
        .exactlyOne<ApplicationStatus>()

data class DecisionTableRow(
    val id: DecisionId,
    val number: Int,
    val createdBy: EvakaUserId,
    val sentDate: LocalDate,
    val unitId: DaycareId,
    val applicationId: ApplicationId,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: DecisionStatus,
    val documentKey: String?,
    val requestedStartDate: LocalDate?,
    val resolved: Instant?,
    val resolvedBy: EvakaUserId?
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getDecisionRowsByApplication(applicationId: ApplicationId) =
    @Suppress("DEPRECATION")
    createQuery(
        // language=SQL
        "SELECT * FROM decision WHERE application_id = :applicationId ORDER BY type"
    ).bind("applicationId", applicationId)
        .mapTo<DecisionTableRow>()

fun Database.Read.getDecisionRowById(id: DecisionId) =
    @Suppress("DEPRECATION")
    createQuery(
        // language=SQL
        "SELECT * FROM decision WHERE id = :id"
    ).bind("id", id)
        .exactlyOne<DecisionTableRow>()

data class PlacementTableRow(
    val id: PlacementId,
    val type: PlacementType,
    val childId: ChildId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getPlacementRowsByChild(childId: ChildId) =
    @Suppress("DEPRECATION")
    createQuery(
        // language=SQL
        "SELECT * FROM placement WHERE child_id = :childId ORDER BY start_date"
    ).bind("childId", childId)
        .mapTo<PlacementTableRow>()

data class PlacementPlanTableRow(
    val id: PlacementPlanId,
    val type: PlacementType,
    val unitId: DaycareId,
    val applicationId: ApplicationId,
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
        if (preschoolDaycareStartDate != null && preschoolDaycareEndDate != null) {
            FiniteDateRange(preschoolDaycareStartDate, preschoolDaycareEndDate)
        } else {
            null
        }
}

fun Database.Read.getPlacementPlanRowByApplication(applicationId: ApplicationId) =
    @Suppress("DEPRECATION")
    createQuery(
        // language=SQL
        "SELECT * FROM placement_plan WHERE application_id = :applicationId"
    ).bind("applicationId", applicationId)
        .exactlyOne<PlacementPlanTableRow>()

data class BackupCareTableRow(
    val id: BackupCareId,
    val childId: ChildId,
    val unitId: DaycareId,
    val groupId: GroupId?,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getBackupCareRowById(id: BackupCareId) =
    @Suppress("DEPRECATION")
    createQuery(
        // language=SQL
        "SELECT * FROM backup_care WHERE id = :id"
    ).bind("id", id)
        .exactlyOne<BackupCareTableRow>()

fun Database.Read.getBackupCareRowsByChild(childId: ChildId) =
    @Suppress("DEPRECATION")
    createQuery(
        // language=SQL
        "SELECT * FROM backup_care WHERE child_id = :childId ORDER BY start_date"
    ).bind("childId", childId)
        .mapTo<BackupCareTableRow>()
