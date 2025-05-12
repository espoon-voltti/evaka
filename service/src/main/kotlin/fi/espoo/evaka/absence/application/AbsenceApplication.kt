// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence.application

import fi.espoo.evaka.pis.PersonNameDetails
import fi.espoo.evaka.shared.AbsenceApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

enum class AbsenceApplicationStatus {
    WAITING_DECISION,
    ACCEPTED,
    REJECTED,
}

data class AbsenceApplication(
    val id: AbsenceApplicationId,
    val createdAt: HelsinkiDateTime,
    val createdBy: EvakaUserId,
    val updatedAt: HelsinkiDateTime,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUserId,
    val childId: ChildId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String,
    val status: AbsenceApplicationStatus,
    val decidedAt: HelsinkiDateTime?,
    val decidedBy: EvakaUserId?,
    val rejectedReason: String?,
)

data class AbsenceApplicationSummary(
    val id: AbsenceApplicationId,
    val createdAt: HelsinkiDateTime,
    @Nested("created_by") val createdBy: EvakaUser,
    @Nested("child") val child: PersonNameDetails,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String,
    val status: AbsenceApplicationStatus,
    val decidedAt: HelsinkiDateTime?,
    @Nested("decided_by") val decidedBy: EvakaUser?,
    val rejectedReason: String?,
)

data class AbsenceApplicationSummaryEmployee(
    val data: AbsenceApplicationSummary,
    val actions: Set<Action.AbsenceApplication>,
)

data class AbsenceApplicationSummaryCitizen(
    val data: AbsenceApplicationSummary,
    val actions: Set<Action.Citizen.AbsenceApplication>,
)

data class AbsenceApplicationCreateRequest(
    val childId: ChildId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String,
)

data class AbsenceApplicationStatusUpdateRequest(
    val status: AbsenceApplicationStatus,
    val reason: String?,
)
