// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementType
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ApplicationSummary(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val socialSecurityNumber: String? = null,
    val dateOfBirth: String? = null,
    val type: ApplicationType,
    val dueDate: String?,
    val startDate: String?,
    val preferredUnits: List<PreferredUnit>,
    val origin: ApplicationOrigin,
    val checkedByAdmin: Boolean,
    val status: ApplicationStatus,
    val additionalInfo: Boolean,
    val siblingBasis: Boolean,
    val assistanceNeed: Boolean,
    val wasOnClubCare: Boolean? = null,
    val wasOnDaycare: Boolean? = null,
    val extendedCare: Boolean,
    val duplicateApplication: Boolean = false,
    val transferApplication: Boolean,
    val additionalDaycareApplication: Boolean,
    val placementProposalStatus: PlacementProposalStatus?,
    val placementProposalUnitName: String?
)

data class PlacementProposalStatus(
    val unitConfirmationStatus: PlacementPlanConfirmationStatus,
    val unitRejectReason: PlacementPlanRejectReason?,
    val unitRejectOtherReason: String?
)

data class PersonApplicationSummary(
    val applicationId: UUID,
    val childId: UUID,
    val guardianId: UUID,
    val preferredUnitId: UUID?,
    val preferredUnitName: String?,
    val childName: String?,
    val childSsn: String?,
    val guardianName: String,
    val startDate: LocalDate?,
    val sentDate: LocalDate?,
    val type: String,
    val status: ApplicationStatus
)

data class ApplicationSummaries(
    val data: List<ApplicationSummary>,
    val pages: Int,
    val totalCount: Int
)

data class ApplicationDetails(
    val id: UUID,
    val type: ApplicationType,
    val form: ApplicationForm,
    val status: ApplicationStatus,
    val origin: ApplicationOrigin,

    val childId: UUID,
    val guardianId: UUID,
    val otherGuardianId: UUID?,
    val childRestricted: Boolean,
    val guardianRestricted: Boolean,
    val checkedByAdmin: Boolean,
    val createdDate: OffsetDateTime?,
    val modifiedDate: OffsetDateTime?,
    val sentDate: LocalDate?,
    val dueDate: LocalDate?,
    val transferApplication: Boolean,
    val additionalDaycareApplication: Boolean,
    val hideFromGuardian: Boolean
)

enum class ApplicationType(val type: String) {
    CLUB("club"),
    DAYCARE("daycare"),
    PRESCHOOL("preschool");

    override fun toString(): String {
        return type
    }
}

enum class ApplicationStatus {
    CREATED,
    SENT,
    WAITING_PLACEMENT,
    WAITING_UNIT_CONFIRMATION,
    WAITING_DECISION,
    WAITING_MAILING,
    WAITING_CONFIRMATION,
    REJECTED,
    ACTIVE,
    CANCELLED
}

enum class ApplicationOrigin {
    ELECTRONIC,
    PAPER
}

data class PreferredUnit(
    val id: UUID,
    val name: String
)

data class ApplicationNote(
    val id: UUID,
    val applicationId: UUID,
    val content: String,
    val createdBy: UUID,
    val createdByName: String,
    val updatedBy: UUID?,
    val updatedByName: String?,
    val created: Instant,
    val updated: Instant
)

data class ApplicationUnitSummary(
    val applicationId: UUID,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val guardianFirstName: String,
    val guardianLastName: String,
    val guardianPhone: String?,
    val guardianEmail: String?,
    val requestedPlacementType: PlacementType,
    val preferredStartDate: LocalDate,
    val preferenceOrder: Int,
    val status: ApplicationStatus
)
