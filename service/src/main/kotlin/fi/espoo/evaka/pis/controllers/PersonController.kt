// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.isValidSSN
import fi.espoo.evaka.pis.createEmptyPerson
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.getDeceasedPeople
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.searchPeople
import fi.espoo.evaka.pis.service.ContactInfo
import fi.espoo.evaka.pis.service.MergeService
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.PersonWithChildrenDTO
import fi.espoo.evaka.pis.updatePersonContactInfo
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/person")
class PersonController(
    private val personService: PersonService,
    private val mergeService: MergeService,
    private val asyncJobRunner: AsyncJobRunner
) {
    @PostMapping
    fun createEmpty(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<PersonIdentityResponseJSON> {
        Audit.PersonCreate.log()
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.ADMIN)
        return db.transaction { createEmptyPerson(it) }
            .let { ResponseEntity.ok().body(PersonIdentityResponseJSON.from(it)) }
    }

    @GetMapping(value = ["/details/{personId}", "/identity/{personId}"])
    fun getPerson(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID
    ): ResponseEntity<PersonJSON> {
        Audit.PersonDetailsRead.log(targetId = personId)
        return db.transaction { it.getPersonById(personId) }
            ?.let { ResponseEntity.ok().body(PersonJSON.from(it)) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/dependants/{personId}")
    fun getPersonDependants(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID
    ): ResponseEntity<List<PersonWithChildrenDTO>> {
        Audit.PersonDependantRead.log(targetId = personId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.ADMIN)
        return db.transaction { personService.getPersonWithChildren(it, user, personId) }
            ?.let { ResponseEntity.ok().body(it.children) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/guardians/{personId}")
    fun getPersonGuardians(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID
    ): ResponseEntity<List<PersonJSON>> {
        Audit.PersonGuardianRead.log(targetId = personId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.ADMIN)
        return db.transaction { personService.getGuardians(it, user, personId) }
            .let { ResponseEntity.ok().body(it.map { personDTO -> PersonJSON.from(personDTO) }) }
    }

    @PostMapping("/search")
    fun findBySearchTerms(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: SearchPersonBody
    ): ResponseEntity<List<PersonJSON>>? {
        Audit.PersonDetailsSearch.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return ResponseEntity.ok()
            .body(
                db.read {
                    it.searchPeople(
                        user,
                        body.searchTerm,
                        body.orderBy,
                        body.sortDirection
                    )
                }.map { personDTO -> PersonJSON.from(personDTO) }
            )
    }

    @PutMapping(value = ["/{personId}/contact-info"])
    fun updateContactInfo(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID,
        @RequestBody contactInfo: ContactInfo
    ): ResponseEntity<ContactInfo> {
        Audit.PersonContactInfoUpdate.log(targetId = personId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.STAFF)
        return if (db.transaction { it.updatePersonContactInfo(personId, contactInfo) }) {
            ResponseEntity.ok().body(contactInfo)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{personId}")
    fun updatePersonDetails(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID,
        @RequestBody data: PersonPatch
    ): ResponseEntity<PersonJSON> {
        Audit.PersonUpdate.log(targetId = personId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.STAFF)
        return db.transaction { personService.patchUserDetails(it, personId, data) }
            .let { ResponseEntity.ok(PersonJSON.from(it)) }
    }

    @DeleteMapping("/{personId}")
    fun safeDeletePerson(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID
    ): ResponseEntity<Unit> {
        Audit.PersonDelete.log(targetId = personId)
        user.requireOneOfRoles(UserRole.ADMIN)
        db.transaction { mergeService.deleteEmptyPerson(it, personId) }
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{personId}/ssn")
    fun addSsn(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID,
        @RequestBody body: AddSsnRequest
    ): ResponseEntity<PersonJSON> {
        Audit.PersonUpdate.log(targetId = personId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN)

        if (!isValidSSN(body.ssn)) {
            throw BadRequest("Invalid social security number")
        }
        val person = db.transaction {
            personService.addSsn(it, user, personId, ExternalIdentifier.SSN.getInstance(body.ssn))
        }
        return ResponseEntity.ok(PersonJSON.from(person))
    }

    @PostMapping("/details/ssn")
    fun getOrCreatePersonBySsn(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: GetOrCreatePersonBySsnRequest
    ): ResponseEntity<PersonJSON> {
        Audit.PersonDetailsRead.log()
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        if (!isValidSSN(body.ssn)) throw BadRequest("Invalid SSN")

        val person = db.transaction {
            personService.getOrCreatePerson(
                it,
                user,
                ExternalIdentifier.SSN.getInstance(body.ssn),
                body.readonly
            )
        }

        return person
            ?.let { ResponseEntity.ok().body(PersonJSON.from(it)) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/get-deceased/")
    fun getDeceased(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam("sinceDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) sinceDate: LocalDate
    ): ResponseEntity<List<PersonJSON>> {
        Audit.PersonDetailsRead.log()
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        return ResponseEntity.ok()
            .body(
                db.read { it.getDeceasedPeople(sinceDate) }.map { personDTO -> PersonJSON.from(personDTO) }
            )
    }

    @PostMapping("/merge")
    fun mergePeople(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: MergeRequest
    ): ResponseEntity<Unit> {
        Audit.PersonMerge.log(targetId = body.master, objectId = body.duplicate)
        user.requireOneOfRoles(UserRole.ADMIN)
        db.transaction { tx ->
            mergeService.mergePeople(tx, master = body.master, duplicate = body.duplicate)
        }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/create")
    fun createPerson(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreatePersonBody
    ): ResponseEntity<UUID> {
        Audit.PersonCreate.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)
        return db.transaction { createPerson(it, body) }
            .let { ResponseEntity.ok(it) }
    }

    data class MergeRequest(
        val master: UUID,
        val duplicate: UUID
    )

    data class AddSsnRequest(
        val ssn: String
    )

    data class PersonIdentityResponseJSON(
        val id: UUID,
        val socialSecurityNumber: String?
    ) {
        companion object {
            fun from(person: PersonDTO): PersonIdentityResponseJSON = PersonIdentityResponseJSON(
                id = person.id,
                socialSecurityNumber = (person.identity as? ExternalIdentifier.SSN)?.ssn
            )
        }
    }
}

data class SearchPersonBody(
    val searchTerm: String,
    val orderBy: String,
    val sortDirection: String
)

data class CreatePersonBody(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val phone: String?,
    val email: String?
)

data class GetOrCreatePersonBySsnRequest(
    val ssn: String,
    val readonly: Boolean = false
)
