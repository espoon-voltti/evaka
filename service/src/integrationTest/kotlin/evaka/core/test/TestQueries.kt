// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.test

import evaka.core.application.ApplicationStatus
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.placement.PlacementType
import evaka.core.shared.ApplicationId
import evaka.core.shared.BackupCareId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.GroupId
import evaka.core.shared.PlacementId
import evaka.core.shared.PlacementPlanId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.FiniteDateRange
import java.time.Instant
import java.time.LocalDate

fun Database.Read.getApplicationStatus(applicationId: ApplicationId): ApplicationStatus =
    createQuery { sql("SELECT status FROM application WHERE id = ${bind(applicationId)}") }
        .exactlyOne()

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
    val resolvedBy: EvakaUserId?,
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getDecisionRowsByApplication(applicationId: ApplicationId) =
    createQuery {
            sql(
                "SELECT * FROM decision WHERE application_id = ${bind(applicationId)} ORDER BY type"
            )
        }
        .mapTo<DecisionTableRow>()

fun Database.Read.getDecisionRowById(id: DecisionId) =
    createQuery { sql("SELECT * FROM decision WHERE id = ${bind(id)}") }
        .exactlyOne<DecisionTableRow>()

data class PlacementTableRow(
    val id: PlacementId,
    val type: PlacementType,
    val childId: ChildId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getPlacementRowsByChild(childId: ChildId) =
    createQuery {
            sql("SELECT * FROM placement WHERE child_id = ${bind(childId)} ORDER BY start_date")
        }
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
    val deleted: Boolean,
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
    createQuery {
            sql("SELECT * FROM placement_plan WHERE application_id = ${bind(applicationId)}")
        }
        .exactlyOne<PlacementPlanTableRow>()

data class BackupCareTableRow(
    val id: BackupCareId,
    val childId: ChildId,
    val unitId: DaycareId,
    val groupId: GroupId?,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    fun period() = FiniteDateRange(startDate, endDate)
}

fun Database.Read.getBackupCareRowById(id: BackupCareId) =
    createQuery { sql("SELECT * FROM backup_care WHERE id = ${bind(id)}") }
        .exactlyOne<BackupCareTableRow>()

fun Database.Read.getBackupCareRowsByChild(childId: ChildId) =
    createQuery {
            sql("SELECT * FROM backup_care WHERE child_id = ${bind(childId)} ORDER BY start_date")
        }
        .mapTo<BackupCareTableRow>()
