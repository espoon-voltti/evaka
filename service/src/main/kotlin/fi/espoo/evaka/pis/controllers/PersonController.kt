// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.isValidSSN
import fi.espoo.evaka.pis.PersonSummary
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
import fi.espoo.evaka.pis.service.hideNonPermittedPersonData
import fi.espoo.evaka.pis.updatePersonContactInfo
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.format.annotation.DateTimeFormat
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

@RestController
@RequestMapping("/person")
class PersonController(
    private val personService: PersonService,
    private val mergeService: MergeService,
    private val accessControl: AccessControl
) {
    @PostMapping
    fun createEmpty(db: Database, user: AuthenticatedUser): PersonIdentityResponseJSON {
        Audit.PersonCreate.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.ADMIN)
        return db.connect { dbc -> dbc.transaction { createEmptyPerson(it) } }
            .let { PersonIdentityResponseJSON.from(it) }
    }

    @GetMapping("/{personId}")
    fun getPerson(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId
    ): PersonResponse {
        Audit.PersonDetailsRead.log(targetId = personId)
        accessControl.requirePermissionFor(user, Action.Person.READ, personId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getPersonById(personId)?.let {
                    PersonResponse(
                        PersonJSON.from(it),
                        accessControl.getPermittedActions(tx, user, personId)
                    )
                }
            }
        } ?: throw NotFound("Person $personId not found")
    }

    @GetMapping(value = ["/details/{personId}", "/identity/{personId}"])
    fun getPersonIdentity(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: PersonId
    ): PersonJSON {
        Audit.PersonDetailsRead.log(targetId = personId)
        accessControl.requirePermissionFor(user, Action.Person.READ, personId)
        return db.connect { dbc -> dbc.transaction { it.getPersonById(personId) } }
            ?.hideNonPermittedPersonData(
                includeInvoiceAddress = accessControl.hasPermissionFor(
                    user,
                    Action.Person.READ_INVOICE_ADDRESS,
                    personId
                ),
                includeOphOid = accessControl.hasPermissionFor(user, Action.Person.READ_OPH_OID, personId)
            )
            ?.let { PersonJSON.from(it) }
            ?: throw NotFound()
    }

    @GetMapping("/dependants/{personId}")
    fun getPersonDependants(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: PersonId
    ): List<PersonWithChildrenDTO> {
        Audit.PersonDependantRead.log(targetId = personId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.ADMIN)
        return db.connect { dbc -> dbc.transaction { personService.getPersonWithChildren(it, user, personId) } }?.children
            ?: throw NotFound()
    }

    @GetMapping("/guardians/{personId}")
    fun getPersonGuardians(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") childId: ChildId
    ): List<PersonJSON> {
        Audit.PersonGuardianRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_GUARDIANS, childId)
        return db.connect { dbc -> dbc.transaction { personService.getGuardians(it, user, childId) } }
            .let { it.map { personDTO -> PersonJSON.from(personDTO) } }
    }

    @PostMapping("/search")
    fun findBySearchTerms(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: SearchPersonBody
    ): List<PersonSummary> {
        Audit.PersonDetailsSearch.log()
        accessControl.requirePermissionFor(user, Action.Global.SEARCH_PEOPLE)
        return db.connect { dbc ->
            dbc.read {
                it.searchPeople(
                    user,
                    body.searchTerm,
                    body.orderBy,
                    body.sortDirection
                )
            }
        }
    }

    @PutMapping(value = ["/{personId}/contact-info"])
    fun updateContactInfo(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: PersonId,
        @RequestBody contactInfo: ContactInfo
    ): ContactInfo {
        Audit.PersonContactInfoUpdate.log(targetId = personId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.STAFF)
        return if (db.connect { dbc -> dbc.transaction { it.updatePersonContactInfo(personId, contactInfo) } }) {
            contactInfo
        } else {
            throw NotFound()
        }
    }

    @PatchMapping("/{personId}")
    fun updatePersonDetails(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: PersonId,
        @RequestBody data: PersonPatch
    ): PersonJSON {
        Audit.PersonUpdate.log(targetId = personId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.STAFF)

        val userEditablePersonData = data
            .let {
                if (accessControl.hasPermissionFor(user, Action.Person.UPDATE_INVOICE_ADDRESS, personId)) it
                else it.copy(
                    invoiceRecipientName = null,
                    invoicingStreetAddress = null,
                    invoicingPostalCode = null,
                    invoicingPostOffice = null,
                    forceManualFeeDecisions = null
                )
            }
            .let {
                if (accessControl.hasPermissionFor(user, Action.Person.UPDATE_OPH_OID, personId)) it
                else it.copy(ophPersonOid = null)
            }

        return db.connect { dbc -> dbc.transaction { personService.patchUserDetails(it, personId, userEditablePersonData) } }
            .let { PersonJSON.from(it) }
    }

    @DeleteMapping("/{personId}")
    fun safeDeletePerson(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: PersonId
    ) {
        Audit.PersonDelete.log(targetId = personId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN)
        db.connect { dbc -> dbc.transaction { mergeService.deleteEmptyPerson(it, personId) } }
    }

    @PutMapping("/{personId}/ssn")
    fun addSsn(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId,
        @RequestBody body: AddSsnRequest
    ): PersonJSON {
        Audit.PersonUpdate.log(targetId = personId)
        accessControl.requirePermissionFor(user, Action.Person.ADD_SSN, personId)

        val person = db.connect { dbc -> dbc.transaction { it.getPersonById(personId) } }
            ?: throw NotFound("Person with id $personId not found")

        if (person.ssnAddingDisabled) {
            accessControl.requirePermissionFor(user, Action.Person.ENABLE_SSN_ADDING, personId)
        }

        if (!isValidSSN(body.ssn)) {
            throw BadRequest("Invalid social security number")
        }

        return PersonJSON.from(
            db.connect { dbc ->
                dbc.transaction {
                    personService.addSsn(it, user, personId, ExternalIdentifier.SSN.getInstance(body.ssn))
                }
            }
        )
    }

    @PutMapping("/{personId}/ssn/disable")
    fun disableSsn(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId,
        @RequestBody body: DisableSsnRequest
    ) {
        Audit.PersonUpdate.log(targetId = personId)
        if (!body.disabled) accessControl.requirePermissionFor(user, Action.Person.ENABLE_SSN_ADDING, personId)
        else accessControl.requirePermissionFor(user, Action.Person.DISABLE_SSN_ADDING, personId)

        db.connect { dbc -> dbc.transaction { personService.disableSsn(it, personId, body.disabled) } }
    }

    @PostMapping("/details/ssn")
    fun getOrCreatePersonBySsn(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: GetOrCreatePersonBySsnRequest
    ): PersonJSON {
        Audit.PersonDetailsRead.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        if (!isValidSSN(body.ssn)) throw BadRequest("Invalid SSN")

        return db.connect { dbc ->
            dbc.transaction {
                personService.getOrCreatePerson(
                    it,
                    user,
                    ExternalIdentifier.SSN.getInstance(body.ssn),
                    body.readonly
                )
            }
        }
            ?.let { PersonJSON.from(it) }
            ?: throw NotFound()
    }

    @GetMapping("/get-deceased/")
    fun getDeceased(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("sinceDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) sinceDate: LocalDate
    ): List<PersonJSON> {
        Audit.PersonDetailsRead.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)
        return db.connect { dbc -> dbc.read { it.getDeceasedPeople(sinceDate) } }.map { personDTO -> PersonJSON.from(personDTO) }
    }

    @PostMapping("/merge")
    fun mergePeople(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: MergeRequest
    ) {
        Audit.PersonMerge.log(targetId = body.master, objectId = body.duplicate)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN)
        db.connect { dbc ->
            dbc.transaction { tx ->
                mergeService.mergePeople(tx, master = body.master, duplicate = body.duplicate)
            }
        }
    }

    @PostMapping("/create")
    fun createPerson(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: CreatePersonBody
    ): PersonId {
        Audit.PersonCreate.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)
        return db.connect { dbc -> dbc.transaction { createPerson(it, body) } }
    }

    data class PersonResponse(
        val person: PersonJSON,
        val permittedActions: Set<Action.Person>
    )

    data class MergeRequest(
        val master: PersonId,
        val duplicate: PersonId
    )

    data class AddSsnRequest(
        val ssn: String
    )

    data class DisableSsnRequest(
        val disabled: Boolean
    )

    data class PersonIdentityResponseJSON(
        val id: PersonId,
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
    val phone: String,
    val email: String?
)

data class GetOrCreatePersonBySsnRequest(
    val ssn: String,
    val readonly: Boolean = false
)
