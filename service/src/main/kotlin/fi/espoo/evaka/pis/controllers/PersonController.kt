// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.upsertAdditionalInformation
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.isValidSSN
import fi.espoo.evaka.pis.PersonSummary
import fi.espoo.evaka.pis.createEmptyPerson
import fi.espoo.evaka.pis.createFosterParentRelationship
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.duplicatePerson
import fi.espoo.evaka.pis.getFosterParents
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonDuplicateOf
import fi.espoo.evaka.pis.searchPeople
import fi.espoo.evaka.pis.service.FridgeFamilyService
import fi.espoo.evaka.pis.service.MergeService
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.PersonWithChildrenDTO
import fi.espoo.evaka.pis.service.addToGuardianBlocklist
import fi.espoo.evaka.pis.service.deleteFromGuardianBlocklist
import fi.espoo.evaka.pis.service.deleteGuardianRelationship
import fi.espoo.evaka.pis.service.getBlockedGuardians
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.pis.service.hideNonPermittedPersonData
import fi.espoo.evaka.pis.updateOphPersonOid
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.varda.getDistinctVardaPersonOidsByEvakaPersonId
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/person")
class PersonController(
    private val personService: PersonService,
    private val mergeService: MergeService,
    private val accessControl: AccessControl,
    private val fridgeFamilyService: FridgeFamilyService
) {
    @PostMapping
    fun createEmpty(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): PersonIdentityResponseJSON {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(it, user, clock, Action.Global.CREATE_PERSON)
                    createEmptyPerson(it, clock)
                }
            }
            .let { PersonIdentityResponseJSON.from(it) }
            .also { Audit.PersonCreate.log(targetId = it.id) }
    }

    @GetMapping("/{personId}")
    fun getPerson(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable personId: PersonId
    ): PersonResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ,
                        personId
                    )
                    tx.getPersonById(personId)?.let {
                        PersonResponse(
                            PersonJSON.from(it),
                            accessControl.getPermittedActions(tx, user, clock, personId)
                        )
                    }
                } ?: throw NotFound("Person $personId not found")
            }
            .also { Audit.PersonDetailsRead.log(targetId = personId) }
    }

    @PostMapping("/{personId}/duplicate")
    fun duplicate(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable personId: PersonId
    ): PersonId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.DUPLICATE,
                        personId
                    )

                    if (tx.getPersonDuplicateOf(personId) != null) {
                        throw BadRequest("Person $personId is duplicate")
                    }

                    val duplicateId =
                        tx.duplicatePerson(personId) ?: throw NotFound("Person $personId not found")
                    tx.getChild(personId)?.let { child ->
                        tx.upsertAdditionalInformation(
                            childId = duplicateId,
                            data = child.additionalInformation
                        )
                    }

                    val parentRelationships =
                        tx.getChildGuardians(personId).map { guardianId ->
                            CreateFosterParentRelationshipBody(
                                childId = duplicateId,
                                parentId = guardianId,
                                DateRange(clock.today(), null)
                            )
                        } +
                            tx.getFosterParents(personId).map { relationship ->
                                CreateFosterParentRelationshipBody(
                                    childId = duplicateId,
                                    parentId = relationship.parent.id,
                                    validDuring = relationship.validDuring
                                )
                            }
                    parentRelationships.forEach { relationship ->
                        tx.createFosterParentRelationship(relationship)
                    }

                    if (tx.getPersonById(duplicateId)!!.ophPersonOid.isNullOrBlank()) {
                        val vardaPersonOids = tx.getDistinctVardaPersonOidsByEvakaPersonId(personId)
                        if (vardaPersonOids.isNotEmpty()) {
                            tx.updateOphPersonOid(
                                duplicateId,
                                vardaPersonOids.sorted().joinToString(",")
                            )
                        }
                    }

                    duplicateId
                }
            }
            .also { Audit.PersonDuplicate.log(targetId = personId) }
    }

    @GetMapping(value = ["/details/{personId}", "/identity/{personId}"])
    fun getPersonIdentity(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "personId") personId: PersonId
    ): PersonJSON {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ,
                        personId
                    )
                    (tx.getPersonById(personId) ?: throw NotFound())
                        .hideNonPermittedPersonData(
                            includeInvoiceAddress =
                                accessControl.hasPermissionFor(
                                    tx,
                                    user,
                                    clock,
                                    Action.Person.READ_INVOICE_ADDRESS,
                                    personId
                                ),
                            includeOphOid =
                                accessControl.hasPermissionFor(
                                    tx,
                                    user,
                                    clock,
                                    Action.Person.READ_OPH_OID,
                                    personId
                                )
                        )
                        .let { PersonJSON.from(it) }
                }
            }
            .also { Audit.PersonDetailsRead.log(targetId = personId) }
    }

    @GetMapping("/dependants/{personId}")
    fun getPersonDependants(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "personId") personId: PersonId
    ): List<PersonWithChildrenDTO> {
        return db.connect { dbc ->
                dbc.transaction {
                        accessControl.requirePermissionFor(
                            it,
                            user,
                            clock,
                            Action.Person.READ_DEPENDANTS,
                            personId
                        )
                        personService.getPersonWithChildren(it, user, personId)
                    }
                    ?.children ?: throw NotFound()
            }
            .also {
                Audit.PersonDependantRead.log(targetId = personId, meta = mapOf("count" to it.size))
            }
    }

    @GetMapping("/guardians/{personId}")
    fun getPersonGuardians(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "personId") childId: ChildId
    ): List<PersonJSON> {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_GUARDIANS,
                        childId
                    )
                    personService.getGuardians(it, user, childId)
                }
            }
            .let { it.map { personDTO -> PersonJSON.from(personDTO) } }
            .also {
                Audit.PersonGuardianRead.log(targetId = childId, meta = mapOf("count" to it.size))
            }
    }

    @GetMapping("/blocked-guardians/{personId}")
    fun getPersonBlockedGuardians(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "personId") childId: ChildId
    ): List<PersonJSON> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_BLOCKED_GUARDIANS,
                        childId
                    )
                    tx.getBlockedGuardians(childId).mapNotNull { tx.getPersonById(it) }
                }
            }
            .let { it.map { personDTO -> PersonJSON.from(personDTO) } }
            .also {
                Audit.PersonBlockedGuardiansRead.log(
                    targetId = childId,
                    meta = mapOf("count" to it.size)
                )
            }
    }

    @PostMapping("/search")
    fun findBySearchTerms(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SearchPersonBody
    ): List<PersonSummary> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(it, user, clock, Action.Global.SEARCH_PEOPLE)
                    it.searchPeople(
                        user,
                        body.searchTerm,
                        body.orderBy,
                        body.sortDirection,
                        restricted =
                            !accessControl.hasPermissionFor(
                                it,
                                user,
                                clock,
                                Action.Global.SEARCH_PEOPLE_UNRESTRICTED
                            )
                    )
                }
            }
            .also { Audit.PersonDetailsSearch.log(meta = mapOf("count" to it.size)) }
    }

    @PatchMapping("/{personId}")
    fun updatePersonDetails(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "personId") personId: PersonId,
        @RequestBody data: PersonPatch
    ): PersonJSON {
        return db.connect { dbc ->
                val userEditablePersonData =
                    dbc.read { tx ->
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Person.UPDATE,
                            personId
                        )
                        data
                            .let {
                                if (
                                    accessControl.hasPermissionFor(
                                        tx,
                                        user,
                                        clock,
                                        Action.Person.UPDATE_PERSONAL_DETAILS,
                                        personId
                                    )
                                ) {
                                    it
                                } else {
                                    it.copy(
                                        firstName = null,
                                        lastName = null,
                                        dateOfBirth = null,
                                        streetAddress = null,
                                        postalCode = null,
                                        postOffice = null
                                    )
                                }
                            }
                            .let {
                                if (
                                    accessControl.hasPermissionFor(
                                        tx,
                                        user,
                                        clock,
                                        Action.Person.UPDATE_INVOICE_ADDRESS,
                                        personId
                                    )
                                ) {
                                    it
                                } else {
                                    it.copy(
                                        invoiceRecipientName = null,
                                        invoicingStreetAddress = null,
                                        invoicingPostalCode = null,
                                        invoicingPostOffice = null,
                                        forceManualFeeDecisions = null
                                    )
                                }
                            }
                            .let {
                                if (
                                    accessControl.hasPermissionFor(
                                        tx,
                                        user,
                                        clock,
                                        Action.Person.UPDATE_OPH_OID,
                                        personId
                                    )
                                ) {
                                    it
                                } else {
                                    it.copy(ophPersonOid = null)
                                }
                            }
                    }

                dbc.transaction {
                        personService.patchUserDetails(it, personId, userEditablePersonData)
                    }
                    .let { PersonJSON.from(it) }
            }
            .also { Audit.PersonUpdate.log(targetId = personId) }
    }

    @DeleteMapping("/{personId}")
    fun safeDeletePerson(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "personId") personId: PersonId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Person.DELETE, personId)
                mergeService.deleteEmptyPerson(it, personId)
            }
        }
        Audit.PersonDelete.log(targetId = personId)
    }

    @PutMapping("/{personId}/ssn")
    fun addSsn(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable personId: PersonId,
        @RequestBody body: AddSsnRequest
    ): PersonJSON {
        return db.connect { dbc ->
                val person =
                    dbc.read {
                        accessControl.requirePermissionFor(
                            it,
                            user,
                            clock,
                            Action.Person.ADD_SSN,
                            personId
                        )

                        it.getPersonById(personId)
                    } ?: throw NotFound("Person with id $personId not found")

                if (person.ssnAddingDisabled) {
                    dbc.read {
                        accessControl.requirePermissionFor(
                            it,
                            user,
                            clock,
                            Action.Person.ENABLE_SSN_ADDING,
                            personId
                        )
                    }
                }

                if (!isValidSSN(body.ssn)) {
                    throw BadRequest("Invalid social security number")
                }

                PersonJSON.from(
                    dbc.transaction {
                        personService.addSsn(
                            it,
                            user,
                            personId,
                            ExternalIdentifier.SSN.getInstance(body.ssn)
                        )
                    }
                )
            }
            .also { Audit.PersonUpdate.log(targetId = personId) }
    }

    @PutMapping("/{personId}/ssn/disable")
    fun disableSsn(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable personId: PersonId,
        @RequestBody body: DisableSsnRequest
    ) {
        db.connect { dbc ->
            dbc.transaction {
                if (!body.disabled) {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.ENABLE_SSN_ADDING,
                        personId
                    )
                } else {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.DISABLE_SSN_ADDING,
                        personId
                    )
                }

                personService.disableSsn(it, personId, body.disabled)
            }
        }
        Audit.PersonUpdate.log(targetId = personId)
    }

    @PostMapping("/details/ssn")
    fun getOrCreatePersonBySsn(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: GetOrCreatePersonBySsnRequest
    ): PersonJSON {
        if (!isValidSSN(body.ssn)) throw BadRequest("Invalid SSN")

        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.CREATE_PERSON_FROM_VTJ
                    )

                    personService.getOrCreatePerson(
                        it,
                        user,
                        ExternalIdentifier.SSN.getInstance(body.ssn),
                        body.readonly
                    )
                } ?: throw NotFound()
            }
            .let { PersonJSON.from(it) }
            .also { Audit.PersonDetailsRead.log(targetId = it.id) }
    }

    @PostMapping("/merge")
    fun mergePeople(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: MergeRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Person.MERGE,
                    setOf(body.master, body.duplicate)
                )
                mergeService.mergePeople(
                    tx,
                    clock,
                    master = body.master,
                    duplicate = body.duplicate
                )
            }
        }
        Audit.PersonMerge.log(targetId = body.master, objectId = body.duplicate)
    }

    @PostMapping("/create")
    fun createPerson(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: CreatePersonBody
    ): PersonId {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(it, user, clock, Action.Global.CREATE_PERSON)
                    createPerson(it, body)
                }
            }
            .also { personId -> Audit.PersonCreate.log(targetId = personId) }
    }

    @PostMapping("/{personId}/vtj-update")
    fun updatePersonAndFamilyFromVtj(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable personId: PersonId
    ) {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.UPDATE_FROM_VTJ,
                        personId
                    )
                }
                fridgeFamilyService.updatePersonAndFamilyFromVtj(dbc, user, clock, personId)
            }
            .also { Audit.PersonVtjFamilyUpdate.log(targetId = personId) }
    }

    data class EvakaRightsRequest(val guardianId: PersonId, val denied: Boolean)

    @PostMapping("/{childId}/evaka-rights")
    fun updateGuardianEvakaRights(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: EvakaRightsRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Person.UPDATE_EVAKA_RIGHTS,
                    childId
                )
                if (body.denied) {
                    tx.addToGuardianBlocklist(childId, body.guardianId)
                    tx.deleteGuardianRelationship(childId, body.guardianId)
                } else {
                    tx.deleteFromGuardianBlocklist(childId, body.guardianId)
                    personService.getGuardians(tx, user, childId, forceRefresh = true)
                }
            }
        }
        Audit.PersonUpdateEvakaRights.log(
            targetId = childId,
            objectId = body.guardianId,
            meta = mapOf("denied" to body.denied)
        )
    }

    data class PersonResponse(val person: PersonJSON, val permittedActions: Set<Action.Person>)

    data class MergeRequest(val master: PersonId, val duplicate: PersonId)

    data class AddSsnRequest(val ssn: String)

    data class DisableSsnRequest(val disabled: Boolean)

    data class PersonIdentityResponseJSON(val id: PersonId, val socialSecurityNumber: String?) {
        companion object {
            fun from(person: PersonDTO): PersonIdentityResponseJSON =
                PersonIdentityResponseJSON(
                    id = person.id,
                    socialSecurityNumber = (person.identity as? ExternalIdentifier.SSN)?.ssn
                )
        }
    }
}

data class SearchPersonBody(val searchTerm: String, val orderBy: String, val sortDirection: String)

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

data class GetOrCreatePersonBySsnRequest(val ssn: String, val readonly: Boolean = false)
