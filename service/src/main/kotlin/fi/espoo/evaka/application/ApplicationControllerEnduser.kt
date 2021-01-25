// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.enduser.ApplicationJson
import fi.espoo.evaka.application.enduser.ApplicationSerializer
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
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
    private val personService: PersonService,
    private val serializer: ApplicationSerializer,
    private val applicationStateService: ApplicationStateService
) {

    @PostMapping
    fun createApplication(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: CreateApplicationEnduserRequest
    ): ResponseEntity<UUID> {
        Audit.ApplicationCreate.log(targetId = user.id, objectId = body.childId)
        user.requireOneOfRoles(UserRole.END_USER)

        val id = db.transaction { tx ->
            val guardian = personService.getUpToDatePerson(tx, user, user.id)
                ?: throw NotFound("Guardian not found")
            val child = personService.getUpToDatePerson(tx, user, body.childId)
                ?: throw NotFound("Child not found")
            val type = body.type

            if (duplicateApplicationExists(tx.handle, body.childId, user.id, type)) {
                throw BadRequest("Duplicate application exsts")
            }
            val id = insertApplication(
                h = tx.handle,
                guardianId = user.id,
                childId = body.childId,
                origin = ApplicationOrigin.ELECTRONIC
            )
            val form = ApplicationForm.initForm(
                type = type,
                guardian = guardian,
                child = child
            )
            updateForm(tx.handle, id, form, type, child.restrictedDetailsEnabled, guardian.restrictedDetailsEnabled)
            id
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(id)
    }

    @GetMapping
    fun getApplications(db: Database, user: AuthenticatedUser): ResponseEntity<List<ApplicationJson>> {
        Audit.ApplicationRead.log(targetId = user.id)
        user.requireOneOfRoles(UserRole.END_USER)

        val applications = db.transaction { tx ->
            fetchOwnApplicationIds(tx.handle, user.id)
                .mapNotNull { fetchApplicationDetails(tx.handle, it) }
                .map { serializer.serialize(tx, user, it) }
        }

        return ResponseEntity.ok(applications)
    }

    @GetMapping("/{applicationId}")
    fun getApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID
    ): ResponseEntity<ApplicationJson> {
        Audit.ApplicationRead.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        val application = db.transaction { tx ->
            fetchApplicationDetails(tx.handle, applicationId)
                ?.takeIf { it.guardianId == user.id }
                ?.let { serializer.serialize(tx, user, it) }
                ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")
        }

        return ResponseEntity.ok(application)
    }

    @PutMapping("/{applicationId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID,
        @RequestBody formJson: String
    ): ResponseEntity<ApplicationJson> {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        val formV0 = serializer.deserialize(user, formJson)
        val updatedApplication = db.transaction { tx ->
            applicationStateService
                .updateOwnApplicationContentsEnduser(tx, user, applicationId, formV0)
                .let { serializer.serialize(tx, user, it) }
        }

        return ResponseEntity.ok(updatedApplication)
    }

    @DeleteMapping("/{applicationId}")
    fun deleteApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID
    ): ResponseEntity<Unit> {
        Audit.ApplicationDelete.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        db.transaction { tx ->
            val application = fetchApplicationDetails(tx.handle, applicationId)
                ?.takeIf { it.guardianId == user.id }
                ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")

            if (application.status != ApplicationStatus.CREATED)
                throw BadRequest("Only drafts can be deleted")

            deleteApplication(tx.handle, applicationId)
        }

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/send-application")
    fun sendApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID
    ): ResponseEntity<Unit> {
        db.transaction { applicationStateService.sendApplication(it, user, applicationId, isEnduser = true) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/accept-decision")
    fun acceptDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: AcceptDecisionRequest
    ): ResponseEntity<Unit> {
        db.transaction {
            applicationStateService.acceptDecision(
                it,
                user,
                applicationId,
                body.decisionId,
                body.requestedStartDate,
                isEnduser = true
            )
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: RejectDecisionRequest
    ): ResponseEntity<Unit> {
        db.transaction {
            applicationStateService.rejectDecision(it, user, applicationId, body.decisionId, isEnduser = true)
        }
        return ResponseEntity.noContent().build()
    }
}

data class CreateApplicationEnduserRequest(
    val childId: UUID,
    val type: ApplicationType
)
