// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.upsertCitizenUser
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate

data class CitizenApplicationUpdate(
    val form: ApplicationFormUpdate,
    val allowOtherGuardianAccess: Boolean,
)

data class ApplicationUpdate(val form: ApplicationFormUpdate, val dueDate: LocalDate? = null)

data class ApplicationSummary(
    val id: ApplicationId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate? = null,
    val type: ApplicationType,
    val placementType: PlacementType,
    val serviceNeed: ServiceNeedOption?,
    val dueDate: LocalDate?,
    val startDate: LocalDate?,
    val preferredUnits: List<PreferredUnit>,
    val origin: ApplicationOrigin,
    val checkedByAdmin: Boolean,
    val confidential: Boolean?,
    val status: ApplicationStatus,
    val additionalInfo: Boolean,
    val serviceWorkerNote: String,
    val siblingBasis: Boolean,
    val assistanceNeed: Boolean,
    val wasOnClubCare: Boolean? = null,
    val wasOnDaycare: Boolean? = null,
    val continuation: Boolean,
    val extendedCare: Boolean,
    val duplicateApplication: Boolean = false,
    val transferApplication: Boolean,
    val urgent: Boolean,
    val attachmentCount: Int,
    val additionalDaycareApplication: Boolean,
    val placementProposalStatus: PlacementProposalStatus?,
    val placementPlanStartDate: LocalDate?,
    val placementPlanUnitName: String?,
    val currentPlacementUnit: PreferredUnit?,
    val streetAddress: String?,
    val postalCode: String?,
    val postOffice: String?,
)

data class PlacementProposalStatus(
    val unitConfirmationStatus: PlacementPlanConfirmationStatus,
    val unitRejectReason: PlacementPlanRejectReason?,
    val unitRejectOtherReason: String?,
    val modifiedAt: HelsinkiDateTime?,
    val modifiedBy: EvakaUser?,
)

data class PersonApplicationSummary(
    val applicationId: ApplicationId,
    val childId: ChildId,
    val guardianId: PersonId,
    val preferredUnitId: DaycareId?,
    val preferredUnitName: String?,
    val childName: String?,
    val childSsn: String?,
    val guardianName: String,
    val preferredStartDate: LocalDate?,
    val sentDate: LocalDate?,
    val type: ApplicationType,
    val status: ApplicationStatus,
    val connectedDaycare: Boolean = false,
    val preparatoryEducation: Boolean = false,
)

data class ApplicationDetails(
    val id: ApplicationId,
    val type: ApplicationType,
    val form: ApplicationForm,
    val status: ApplicationStatus,
    val origin: ApplicationOrigin,
    val childId: ChildId,
    val guardianId: PersonId,
    val hasOtherGuardian: Boolean,
    val otherGuardianLivesInSameAddress: Boolean?,
    val childRestricted: Boolean,
    val guardianRestricted: Boolean,
    val guardianDateOfDeath: LocalDate?,
    val checkedByAdmin: Boolean,
    val confidential: Boolean?,
    val createdAt: HelsinkiDateTime?,
    val createdBy: EvakaUser?,
    val modifiedAt: HelsinkiDateTime?,
    val modifiedBy: EvakaUser?,
    val sentDate: LocalDate?,
    val dueDate: LocalDate?,
    val dueDateSetManuallyAt: HelsinkiDateTime?,
    val transferApplication: Boolean,
    val additionalDaycareApplication: Boolean,
    val hideFromGuardian: Boolean,
    val allowOtherGuardianAccess: Boolean,
    val attachments: List<ApplicationAttachment>,
) {
    fun derivePlacementType(): PlacementType =
        when (type) {
            ApplicationType.PRESCHOOL -> {
                if (form.preferences.preparatory) {
                    if (form.preferences.serviceNeed != null) PlacementType.PREPARATORY_DAYCARE
                    else PlacementType.PREPARATORY
                } else {
                    if (form.preferences.serviceNeed != null)
                        form.preferences.serviceNeed.serviceNeedOption?.validPlacementType
                            ?: PlacementType.PRESCHOOL_DAYCARE
                    else PlacementType.PRESCHOOL
                }
            }
            ApplicationType.DAYCARE ->
                if (form.preferences.serviceNeed?.partTime == true) PlacementType.DAYCARE_PART_TIME
                else PlacementType.DAYCARE
            ApplicationType.CLUB -> PlacementType.CLUB
        }
}

enum class ApplicationAttachmentType {
    URGENCY,
    EXTENDED_CARE,
    SERVICE_WORKER_ATTACHMENT,
}

data class ApplicationAttachment(
    val id: AttachmentId,
    val name: String,
    val contentType: String,
    val receivedAt: HelsinkiDateTime,
    val type: ApplicationAttachmentType,
    val uploadedByEmployee: EmployeeId?,
    val uploadedByPerson: PersonId?,
)

@ConstList("applicationTypes")
enum class ApplicationType : DatabaseEnum {
    CLUB,
    DAYCARE,
    PRESCHOOL;

    override val sqlType: String = "application_type"
}

enum class ApplicationStatus : DatabaseEnum {
    CREATED,
    SENT,
    WAITING_PLACEMENT,
    WAITING_UNIT_CONFIRMATION,
    WAITING_DECISION,
    WAITING_MAILING,
    WAITING_CONFIRMATION,
    REJECTED,
    ACTIVE,
    CANCELLED;

    override val sqlType: String = "application_status_type"
}

enum class ApplicationOrigin : DatabaseEnum {
    ELECTRONIC,
    PAPER;

    override val sqlType: String = "application_origin_type"
}

data class PreferredUnit(val id: DaycareId, val name: String)

data class ApplicationNote(
    val id: ApplicationNoteId,
    val applicationId: ApplicationId,
    val content: String,
    val createdAt: HelsinkiDateTime,
    val createdBy: EvakaUserId,
    val createdByName: String,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUserId,
    val modifiedByName: String,
    val messageContentId: MessageContentId?,
    val messageThreadId: MessageThreadId?,
)

data class ApplicationUnitSummary(
    val applicationId: ApplicationId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val guardianFirstName: String,
    val guardianLastName: String,
    val guardianPhone: String?,
    val guardianEmail: String?,
    val requestedPlacementType: PlacementType,
    val serviceNeed: ServiceNeedOption?,
    val preferredStartDate: LocalDate,
    val extendedCare: Boolean,
    val preferenceOrder: Int,
    val status: ApplicationStatus,
)

data class TransferApplicationUnitSummary(
    val applicationId: ApplicationId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val preferredStartDate: LocalDate,
)

data class CitizenApplicationSummary(
    val applicationId: ApplicationId,
    val type: ApplicationType,
    val childId: ChildId,
    val childName: String?,
    val preferredUnitName: String?,
    val allPreferredUnitNames: List<String>,
    val startDate: LocalDate?,
    val sentDate: LocalDate?,
    val applicationStatus: ApplicationStatus,
    val createdDate: HelsinkiDateTime,
    val transferApplication: Boolean,
)

fun savePaperApplication(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    clock: EvakaClock,
    body: PaperApplicationCreateRequest,
    personService: PersonService,
    applicationStateService: ApplicationStateService,
): Pair<PersonId, ApplicationId> {
    val child =
        tx.getPersonById(body.childId)
            ?: throw BadRequest("Could not find the child with id ${body.childId}")

    val guardianId =
        body.guardianId
            ?: if (!body.guardianSsn.isNullOrEmpty()) {
                personService
                    .getOrCreatePerson(
                        tx,
                        user,
                        ExternalIdentifier.SSN.getInstance(body.guardianSsn),
                    )
                    ?.id
                    ?: throw BadRequest("Could not find the guardian with ssn ${body.guardianSsn}")
            } else if (body.guardianToBeCreated != null) {
                createPerson(tx, body.guardianToBeCreated)
            } else {
                throw BadRequest(
                    "Could not find guardian info from paper application request for ${body.childId}"
                )
            }

    val guardian =
        tx.getPersonById(guardianId)
            ?: throw BadRequest("Could not find the guardian with id $guardianId")

    // If the guardian has never logged in to eVaka, evaka_user might not contain a
    // row for them yet
    tx.upsertCitizenUser(guardianId)

    val applicationId =
        applicationStateService.createApplication(
            tx = tx,
            user = user,
            now = clock.now(),
            type = body.type,
            guardian = guardian,
            child = child,
            origin = ApplicationOrigin.PAPER,
            hideFromGuardian = body.hideFromGuardian,
            sentDate = body.sentDate,
            allowOtherGuardianAccess = true,
        )
    return Pair(guardianId, applicationId)
}
