// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.enduser.ApplicationJson
import fi.espoo.evaka.application.enduser.ApplicationJsonType
import fi.espoo.evaka.application.enduser.ApplicationSerializer
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.Jdbi
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/enduser/v2/applications")
class ApplicationControllerEnduser(
    private val jdbi: Jdbi,
    private val personService: PersonService,
    private val serializer: ApplicationSerializer,
    private val applicationStateService: ApplicationStateService
) {

    @PostMapping
    fun createApplication(user: AuthenticatedUser, @RequestBody body: CreateApplicationEnduserRequest): ResponseEntity<UUID> {
        Audit.ApplicationCreate.log(targetId = user.id, objectId = body.childId)
        user.requireOneOfRoles(UserRole.END_USER)

        val guardian = personService.getUpToDatePerson(user, user.id)
            ?: throw NotFound("Guardian not found")
        val child = personService.getUpToDatePerson(user, body.childId)
            ?: throw NotFound("Child not found")
        val type = when (body.type) {
            ApplicationJsonType.CLUB -> ApplicationType.CLUB
            ApplicationJsonType.DAYCARE -> ApplicationType.DAYCARE
            ApplicationJsonType.PRESCHOOL -> ApplicationType.PRESCHOOL
        }

        val id = jdbi.transaction { h ->
            if (duplicateApplicationExists(h, body.childId, user.id, type)) {
                throw BadRequest("Duplicate application exsts")
            }
            val id = insertApplication(
                h = h,
                guardianId = user.id,
                childId = body.childId,
                origin = ApplicationOrigin.ELECTRONIC
            )
            val form = ApplicationForm.initForm(
                type = type,
                guardian = guardian,
                child = child
            )
            updateForm(h, id, form, type, child.restrictedDetailsEnabled, guardian.restrictedDetailsEnabled)
            id
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(id)
    }

    @GetMapping
    fun getApplications(user: AuthenticatedUser): ResponseEntity<List<ApplicationJson>> {
        Audit.ApplicationRead.log(targetId = user.id)
        user.requireOneOfRoles(UserRole.END_USER)

        val applications = jdbi.transaction { h ->
            fetchOwnApplicationIds(h, user.id)
                .mapNotNull { fetchApplicationDetails(h, it) }
                .map { serializer.serialize(user, it) }
        }

        return ResponseEntity.ok(applications)
    }

    @GetMapping("/{applicationId}")
    fun getApplication(
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID
    ): ResponseEntity<ApplicationJson> {
        Audit.ApplicationRead.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        val application = jdbi.transaction { h -> fetchApplicationDetails(h, applicationId) }
            ?.takeIf { it.guardianId == user.id }
            ?.let { serializer.serialize(user, it) }
            ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")

        return ResponseEntity.ok(application)
    }

    @PutMapping("/{applicationId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateApplication(
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID,
        @RequestBody formJson: String
    ): ResponseEntity<ApplicationJson> {
        val formV0 = serializer.deserialize(user, formJson)
        val updatedApplication = applicationStateService
            .updateOwnApplicationContents(user, applicationId, formV0)
            .let { serializer.serialize(user, it) }

        return ResponseEntity.ok(updatedApplication)
    }

    @DeleteMapping("/{applicationId}")
    fun deleteApplication(
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID
    ): ResponseEntity<Unit> {
        Audit.ApplicationDelete.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        jdbi.transaction { h ->
            val application = fetchApplicationDetails(h, applicationId)
                ?.takeIf { it.guardianId == user.id }
                ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")

            if (application.status != ApplicationStatus.CREATED)
                throw BadRequest("Only drafts can be deleted")

            deleteApplication(h, applicationId)
        }

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/send-application")
    fun sendApplication(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID
    ): ResponseEntity<Unit> {
        applicationStateService.sendApplication(user, applicationId, isEnduser = true)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/accept-decision")
    fun acceptDecision(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: AcceptDecisionRequest
    ): ResponseEntity<Unit> {
        applicationStateService.acceptDecision(user, applicationId, body.decisionId, body.requestedStartDate, isEnduser = true)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: RejectDecisionRequest
    ): ResponseEntity<Unit> {
        applicationStateService.rejectDecision(user, applicationId, body.decisionId, isEnduser = true)
        return ResponseEntity.noContent().build()
    }
}

data class CreateApplicationEnduserRequest(
    val childId: UUID,
    val type: ApplicationJsonType
)
