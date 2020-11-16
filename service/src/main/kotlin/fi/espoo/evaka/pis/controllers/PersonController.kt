// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.VolttiIdentifier
import fi.espoo.evaka.identity.isValidSSN
import fi.espoo.evaka.pis.createEmptyPerson
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.getDeceasedPeople
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.searchPeople
import fi.espoo.evaka.pis.service.ContactInfo
import fi.espoo.evaka.pis.service.MergeService
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.PersonWithChildrenDTO
import fi.espoo.evaka.pis.updatePersonContactInfo
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles.ADMIN
import fi.espoo.evaka.shared.config.Roles.END_USER
import fi.espoo.evaka.shared.config.Roles.FINANCE_ADMIN
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.config.Roles.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
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
    @PostMapping("/identity")
    fun postPersonIdentity(db: Database, @RequestBody person: PersonIdentityJSON): ResponseEntity<AuthenticatedUser> {
        Audit.PersonCreate.log()
        return db.transaction { tx ->
            tx.handle.getPersonBySSN(person.socialSecurityNumber) ?: tx.handle.createPerson(
                PersonIdentityRequest(
                    identity = person.toIdentifier(),
                    firstName = person.firstName,
                    lastName = person.lastName,
                    email = person.email,
                    language = person.language
                )
            )
        }
            .let { ResponseEntity.ok().body(AuthenticatedUser(it.id, setOf(END_USER))) }
    }

    @PostMapping
    fun createEmpty(db: Database, user: AuthenticatedUser): ResponseEntity<PersonIdentityResponseJSON> {
        Audit.PersonCreate.log()
        user.requireOneOfRoles(SERVICE_WORKER, FINANCE_ADMIN, ADMIN)
        return db.transaction { it.handle.createEmptyPerson() }
            .let { ResponseEntity.ok().body(PersonIdentityResponseJSON.from(it)) }
    }

    @GetMapping(value = ["/details/{personId}", "/identity/{personId}"])
    fun getPerson(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: VolttiIdentifier
    ): ResponseEntity<PersonJSON> {
        Audit.PersonDetailsRead.log(targetId = personId)
        return db.transaction {
            personService.getUpToDatePerson(it, user, personId)
        }
            ?.let { ResponseEntity.ok().body(PersonJSON.from(it)) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/dependants/{personId}")
    fun getPersonDependants(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: VolttiIdentifier
    ): ResponseEntity<List<PersonWithChildrenDTO>> {
        Audit.PersonDependantRead.log(targetId = personId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, ADMIN)
        return db.transaction { personService.getUpToDatePersonWithChildren(it, user, personId) }
            ?.let { ResponseEntity.ok().body(it.children) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/guardians/{personId}")
    fun getPersonGuardians(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: VolttiIdentifier
    ): ResponseEntity<List<PersonJSON>> {
        Audit.PersonGuardianRead.log(targetId = personId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, ADMIN)
        return db.transaction { personService.getGuardians(it, user, personId) }
            .let { ResponseEntity.ok().body(it.map { personDTO -> PersonJSON.from(personDTO) }) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/search")
    fun findBySearchTerms(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam(
            value = "searchTerm",
            required = true
        ) searchTerm: String,
        @RequestParam(
            value = "orderBy",
            required = true
        ) orderBy: String,
        @RequestParam(
            value = "sortDirection",
            required = true
        ) sortDirection: String
    ): ResponseEntity<List<PersonJSON>>? {
        Audit.PersonDetailsSearch.log()
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)
        return ResponseEntity.ok()
            .body(
                db.read {
                    it.handle.searchPeople(
                        searchTerm,
                        orderBy,
                        sortDirection
                    )
                }.map { personDTO -> PersonJSON.from(personDTO) }
            )
    }

    @PutMapping(value = ["/{personId}/contact-info"])
    fun updateContactInfo(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: VolttiIdentifier,
        @RequestBody contactInfo: ContactInfo
    ): ResponseEntity<ContactInfo> {
        Audit.PersonContactInfoUpdate.log(targetId = personId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)
        return if (db.transaction { it.handle.updatePersonContactInfo(personId, contactInfo) }) {
            ResponseEntity.ok().body(contactInfo)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{personId}")
    fun updatePersonDetails(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: VolttiIdentifier,
        @RequestBody data: PersonPatch
    ): ResponseEntity<PersonJSON> {
        Audit.PersonUpdate.log(targetId = personId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)
        return db.transaction { personService.patchUserDetails(it, personId, data) }
            .let { ResponseEntity.ok(PersonJSON.from(it)) }
    }

    @DeleteMapping("/{personId}")
    fun safeDeletePerson(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: VolttiIdentifier
    ): ResponseEntity<Unit> {
        Audit.PersonDelete.log(targetId = personId)
        user.requireOneOfRoles(ADMIN)
        db.transaction { mergeService.deleteEmptyPerson(it, personId) }
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{personId}/ssn")
    fun addSsn(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: VolttiIdentifier,
        @RequestBody body: AddSsnRequest
    ): ResponseEntity<PersonJSON> {
        Audit.PersonUpdate.log(targetId = personId)
        user.requireOneOfRoles(SERVICE_WORKER, ADMIN)

        if (!isValidSSN(body.ssn)) {
            throw BadRequest("Invalid social security number")
        }
        db.transaction {
            personService.addSsn(it, user, personId, ExternalIdentifier.SSN.getInstance(body.ssn))
        }
        val person = db.transaction { personService.getUpToDatePerson(it, user, personId)!! }
        return ResponseEntity.ok(PersonJSON.from(person))
    }

    @GetMapping("/details/ssn/{ssn}")
    fun getOrCreatePersonBySsn(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("ssn") ssn: String,
        @RequestParam("readonly", required = false) readonly: Boolean = false
    ): ResponseEntity<PersonJSON> {
        Audit.PersonDetailsRead.log()
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        if (!isValidSSN(ssn)) throw BadRequest("Invalid SSN")

        val person = if (readonly) {
            personService.getPersonFromVTJ(user, ExternalIdentifier.SSN.getInstance(ssn))
        } else {
            db.transaction {
                personService.getOrCreatePerson(
                    it,
                    user,
                    ExternalIdentifier.SSN.getInstance(ssn)
                )
            }
        }

        return person
            ?.let { ResponseEntity.ok().body(PersonJSON.from(it)) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/get-deceased/")
    fun getDeceased(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("sinceDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) sinceDate: LocalDate
    ): ResponseEntity<List<PersonJSON>> {
        Audit.PersonDetailsRead.log()
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        return ResponseEntity.ok()
            .body(
                db.read { it.handle.getDeceasedPeople(sinceDate) }.map { personDTO -> PersonJSON.from(personDTO) }
            )
    }

    @PostMapping("/merge")
    fun mergePeople(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: MergeRequest
    ): ResponseEntity<Unit> {
        Audit.PersonMerge.log(targetId = body.master, objectId = body.duplicate)
        user.requireOneOfRoles(ADMIN)
        db.transaction { tx ->
            mergeService.mergePeople(tx, master = body.master, duplicate = body.duplicate)
        }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/create")
    fun createPerson(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: CreatePersonBody
    ): ResponseEntity<UUID> {
        Audit.PersonCreate.log()
        user.requireOneOfRoles(ADMIN, SERVICE_WORKER, FINANCE_ADMIN)
        return db.transaction { it.handle.createPerson(body) }
            .let { ResponseEntity.ok(it) }
    }

    data class MergeRequest(
        val master: UUID,
        val duplicate: UUID
    )

    data class AddSsnRequest(
        val ssn: String
    )

    data class PersonIdentityJSON(
        val socialSecurityNumber: String,
        val customerId: Long?,
        val firstName: String,
        val lastName: String,
        val email: String?,
        val language: String?
    ) {
        fun toIdentifier(): ExternalIdentifier.SSN = when {
            this.socialSecurityNumber.isNotBlank() -> ExternalIdentifier.SSN.getInstance(this.socialSecurityNumber)
            else -> throw IllegalArgumentException("Identifier can not be empty.")
        }
    }

    data class PersonIdentityResponseJSON(
        val id: UUID,
        val socialSecurityNumber: String?,
        val customerId: Long?
    ) {
        companion object {
            fun from(person: PersonDTO): PersonIdentityResponseJSON = PersonIdentityResponseJSON(
                id = person.id,
                socialSecurityNumber = (person.identity as? ExternalIdentifier.SSN)?.ssn,
                customerId = person.customerId
            )
        }
    }
}

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
