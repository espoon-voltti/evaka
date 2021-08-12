// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.getDependantGuardians
import fi.espoo.evaka.pis.getGuardianDependants
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.lockPersonBySSN
import fi.espoo.evaka.pis.updatePersonContactInfo
import fi.espoo.evaka.pis.updatePersonDetails
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.PersonDetails
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class PersonService(
    private val personDetailsService: IPersonDetailsService
) {
    // Does a request to VTJ if the person has a SSN and updates person data
    fun getUpToDatePersonFromVtj(tx: Database.Transaction, user: AuthenticatedUser, id: UUID): PersonDTO? {
        val person = tx.getPersonById(id) ?: return null
        return if (person.identity is ExternalIdentifier.SSN) {
            val personDetails =
                personDetailsService.getBasicDetailsFor(
                    IPersonDetailsService.DetailsQuery(user, person.identity)
                )
            if (personDetails is PersonDetails.Result) {
                upsertVtjPerson(tx, personDetails.vtjPerson.mapToDto())
            } else {
                hideNonDisclosureInfo(person)
            }
        } else {
            person
        }
    }

    fun personsLiveInTheSameAddress(db: Database.Read, person1Id: UUID, person2Id: UUID): Boolean {
        val person1 = db.getPersonById(person1Id)
        val person2 = db.getPersonById(person2Id)

        return personsHaveSameResidenceCode(person1, person2) || personsHaveSameAddress(person1, person2)
    }

    private fun personsHaveSameResidenceCode(person1: PersonDTO?, person2: PersonDTO?): Boolean {
        return person1 != null && person2 != null &&
            !person1.restrictedDetailsEnabled && !person2.restrictedDetailsEnabled &&
            !person1.residenceCode.isNullOrBlank() && !person2.residenceCode.isNullOrBlank() &&
            person1.residenceCode == person2.residenceCode
    }

    private fun personsHaveSameAddress(person1: PersonDTO?, person2: PersonDTO?): Boolean {
        return person1 != null && person2 != null &&
            !person1.restrictedDetailsEnabled && !person2.restrictedDetailsEnabled &&
            !person1.streetAddress.isNullOrBlank() && !person2.streetAddress.isNullOrBlank() &&
            !person1.postalCode.isNullOrBlank() && !person2.postalCode.isNullOrBlank() &&
            person1.streetAddress.lowercase() == person2.streetAddress.lowercase() &&
            person1.postalCode == person2.postalCode
    }

    // Does a request to VTJ if SSN is present and the person hasn't had their dependants initialized
    fun getPersonWithChildren(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        id: UUID,
        forceRefresh: Boolean = false
    ): PersonWithChildrenDTO? {
        val guardian = tx.getPersonById(id) ?: return null

        return when (guardian.identity) {
            is ExternalIdentifier.NoID -> toPersonWithChildrenDTO(guardian)
            is ExternalIdentifier.SSN -> {
                if (forceRefresh || guardian.vtjDependantsQueried == null) {
                    getPersonWithDependants(user, guardian.identity)
                        ?.let { upsertVtjChildren(tx, it) }
                        ?.let { toPersonWithChildrenDTO(it) }
                } else {
                    val children = tx.getGuardianDependants(id)
                        .map { toPersonWithChildrenDTO(it) }
                    toPersonWithChildrenDTO(guardian).copy(children = children)
                }
            }
        }
    }

    // Does a request to VTJ if SSN is present
    fun getGuardians(tx: Database.Transaction, user: AuthenticatedUser, id: UUID): List<PersonDTO> {
        val child = tx.getPersonById(id) ?: return emptyList()

        return when (child.identity) {
            is ExternalIdentifier.NoID -> emptyList()
            is ExternalIdentifier.SSN -> {
                if (child.vtjGuardiansQueried == null) {
                    getPersonWithGuardians(user, child.identity)
                        ?.let { upsertVtjGuardians(tx, it) }
                        ?.guardians?.map(::toPersonDTO)
                        ?: emptyList()
                } else {
                    tx.getDependantGuardians(id)
                }
            }
        }
    }

    // In extremely rare cases there might be more than 2 guardians, but it was agreed with product management to use
    // just one of these as the other guardian.
    fun getOtherGuardian(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        otherGuardianId: UUID,
        childId: UUID
    ): PersonDTO? = getGuardians(tx, user, childId).firstOrNull { guardian -> guardian.id != otherGuardianId }

    // Does a request to VTJ if person is not found in database or person data has not been initialized from VTJ
    fun getOrCreatePerson(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        ssn: ExternalIdentifier.SSN,
        readonly: Boolean = false
    ): PersonDTO? {
        val person = tx.getPersonBySSN(ssn.ssn)
        return if (person?.updatedFromVtj == null) {
            val personDetails = personDetailsService.getBasicDetailsFor(
                IPersonDetailsService.DetailsQuery(user, ssn)
            )
            if (personDetails is PersonDetails.Result) {
                if (readonly) return personDetails.vtjPerson.mapToDto().let { toPersonDTO(it) }

                upsertVtjPerson(tx, personDetails.vtjPerson.mapToDto())
                tx.getPersonBySSN(ssn.ssn)
            } else {
                null
            }
        } else {
            person
        }
    }

    fun patchUserDetails(tx: Database.Transaction, id: UUID, data: PersonPatch): PersonDTO {
        val person = tx.getPersonById(id) ?: throw NotFound("Person $id not found")

        when (person.identity) {
            is ExternalIdentifier.SSN -> tx.updatePersonContactInfo(
                id,
                ContactInfo(
                    email = data.email ?: person.email ?: "",
                    phone = data.phone ?: person.phone ?: "",
                    backupPhone = data.backupPhone ?: person.backupPhone,
                    invoiceRecipientName = data.invoiceRecipientName ?: person.invoiceRecipientName,
                    invoicingStreetAddress = data.invoicingStreetAddress ?: person.invoicingStreetAddress,
                    invoicingPostalCode = data.invoicingPostalCode ?: person.invoicingPostalCode,
                    invoicingPostOffice = data.invoicingPostOffice ?: person.invoicingPostOffice,
                    forceManualFeeDecisions = data.forceManualFeeDecisions ?: person.forceManualFeeDecisions
                )
            )
            is ExternalIdentifier.NoID -> tx.updatePersonDetails(id, data)
        }.exhaust()

        return tx.getPersonById(id)!!
    }

    fun addSsn(tx: Database.Transaction, user: AuthenticatedUser, id: UUID, ssn: ExternalIdentifier.SSN): PersonDTO {
        val person = tx.getPersonById(id) ?: throw NotFound("Person $id not found")

        return when (person.identity) {
            is ExternalIdentifier.SSN -> throw BadRequest("User already has ssn")
            is ExternalIdentifier.NoID -> {
                if (tx.getPersonBySSN(ssn.ssn) != null) {
                    throw Conflict("User with same ssn already exists")
                }

                val personDetails =
                    personDetailsService.getBasicDetailsFor(IPersonDetailsService.DetailsQuery(user, ssn))

                if (personDetails is PersonDetails.Result) {
                    val updatedPerson = getPersonWithUpdatedProperties(
                        personDetails.vtjPerson.mapToDto(),
                        person.copy(identity = ssn)
                    )
                    tx.updatePersonFromVtj(updatedPerson)
                    upsertVtjPerson(tx, personDetails.vtjPerson.mapToDto())
                } else {
                    error("Failed to fetch person data from VTJ")
                }
            }
        }.exhaust()
    }

    // Does a request to VTJ
    private fun getPersonWithDependants(user: AuthenticatedUser, ssn: ExternalIdentifier.SSN): VtjPersonDTO? {
        return personDetailsService.getPersonWithDependants(IPersonDetailsService.DetailsQuery(user, ssn))
            .let { it as? PersonDetails.Result }?.vtjPerson?.mapToDto()
    }

    // Does a request to VTJ
    private fun getPersonWithGuardians(user: AuthenticatedUser, ssn: ExternalIdentifier.SSN): VtjPersonDTO? {
        return personDetailsService.getPersonWithGuardians(IPersonDetailsService.DetailsQuery(user, ssn))
            .let { it as? PersonDetails.Result }?.vtjPerson?.mapToDto()
    }

    private fun hideNonDisclosureInfo(person: PersonDTO): PersonDTO {
        return person.copy(
            streetAddress = "",
            postalCode = "",
            postOffice = ""
        )
    }

    private fun toPersonDTO(person: VtjPersonDTO): PersonDTO =
        PersonDTO(
            id = person.id,
            identity = ExternalIdentifier.SSN.getInstance(person.socialSecurityNumber),
            firstName = person.firstName,
            lastName = person.lastName,
            email = null,
            phone = null,
            backupPhone = "",
            language = person.nativeLanguage?.toString(),
            dateOfBirth = person.dateOfBirth,
            dateOfDeath = person.dateOfDeath,
            streetAddress = person.streetAddress,
            postalCode = person.postalCode,
            postOffice = person.city,
            residenceCode = person.residenceCode,
            restrictedDetailsEnabled = person.restrictedDetailsEnabled,
            restrictedDetailsEndDate = person.restrictedDetailsEndDate
        )

    private fun toPersonWithChildrenDTO(person: VtjPersonDTO): PersonWithChildrenDTO =
        PersonWithChildrenDTO(
            id = person.id,
            socialSecurityNumber = person.socialSecurityNumber,
            firstName = person.firstName,
            lastName = person.lastName,
            dateOfBirth = person.dateOfBirth,
            dateOfDeath = person.dateOfDeath,
            nationalities = person.nationalities.toSet(),
            nativeLanguage = person.nativeLanguage,
            restrictedDetails = RestrictedDetails(
                enabled = person.restrictedDetailsEnabled,
                endDate = person.restrictedDetailsEndDate
            ),
            addresses = if (hasAddress(person) && !hasRestriction(person)) {
                setOf(
                    PersonAddressDTO(
                        origin = PersonAddressDTO.Origin.VTJ,
                        streetAddress = person.streetAddress,
                        postalCode = person.postalCode,
                        city = person.city,
                        residenceCode = person.residenceCode
                    )
                )
            } else {
                emptySet()
            },
            residenceCode = person.residenceCode,
            children = person.children.map(::toPersonWithChildrenDTO)
        )

    private fun toPersonWithChildrenDTO(person: PersonDTO): PersonWithChildrenDTO =
        PersonWithChildrenDTO(
            id = person.id,
            socialSecurityNumber = (person.identity as? ExternalIdentifier.SSN)?.toString(),
            firstName = person.firstName ?: "",
            lastName = person.lastName ?: "",
            dateOfBirth = person.dateOfBirth,
            dateOfDeath = person.dateOfDeath,
            nationalities = emptySet(),
            nativeLanguage = person.language?.let { NativeLanguage(languageName = it, code = it) },
            restrictedDetails = RestrictedDetails(
                enabled = false
            ),
            addresses = setOf(
                PersonAddressDTO(
                    origin = PersonAddressDTO.Origin.EVAKA,
                    streetAddress = person.streetAddress ?: "",
                    postalCode = person.postalCode ?: "",
                    city = person.postOffice ?: "",
                    residenceCode = person.residenceCode
                )
            ),
            residenceCode = person.residenceCode,
            children = emptyList()
        )

    private fun hasRestriction(person: VtjPersonDTO) = person.restrictedDetailsEnabled

    private fun hasAddress(person: VtjPersonDTO): Boolean =
        setOf(
            person.streetAddress,
            person.postalCode,
            person.city
        ).all(String::isNotBlank)
}

data class PersonDTO(
    val id: UUID,
    val identity: ExternalIdentifier,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val backupPhone: String = "",
    val language: String?,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate? = null,
    val streetAddress: String? = "",
    val postalCode: String? = "",
    val postOffice: String? = "",
    val residenceCode: String? = "",
    val nationalities: List<String> = emptyList(),
    val restrictedDetailsEnabled: Boolean = false,
    val restrictedDetailsEndDate: LocalDate? = null,
    val updatedFromVtj: HelsinkiDateTime? = null,
    val vtjGuardiansQueried: HelsinkiDateTime? = null,
    val vtjDependantsQueried: HelsinkiDateTime? = null,
    val invoiceRecipientName: String = "",
    val invoicingStreetAddress: String = "",
    val invoicingPostalCode: String = "",
    val invoicingPostOffice: String = "",
    val forceManualFeeDecisions: Boolean = false
) {
    fun toVtjPersonDTO() = VtjPersonDTO(
        id = this.id,
        firstName = this.firstName ?: "",
        lastName = this.lastName ?: "",
        dateOfBirth = this.dateOfBirth,
        dateOfDeath = this.dateOfDeath,
        socialSecurityNumber = this.identity.toString(),
        restrictedDetailsEndDate = this.restrictedDetailsEndDate,
        restrictedDetailsEnabled = this.restrictedDetailsEnabled,
        nativeLanguage = NativeLanguage(
            code = this.language ?: ""
        ),
        streetAddress = this.streetAddress ?: "",
        postalCode = this.postalCode ?: "",
        city = this.postOffice ?: "",
        streetAddressSe = "",
        citySe = "",
        residenceCode = this.residenceCode
    )
}

data class PersonJSON(
    val id: UUID,
    val socialSecurityNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val backupPhone: String = "",
    val language: String? = null,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate? = null,
    val streetAddress: String? = null,
    val postOffice: String? = null,
    val postalCode: String? = null,
    val residenceCode: String? = null,
    val restrictedDetailsEnabled: Boolean = false,
    val invoiceRecipientName: String = "",
    val invoicingStreetAddress: String = "",
    val invoicingPostalCode: String = "",
    val invoicingPostOffice: String = "",
    val forceManualFeeDecisions: Boolean = false
) {
    companion object {
        fun from(p: PersonDTO): PersonJSON = PersonJSON(
            id = p.id,
            socialSecurityNumber = (p.identity as? ExternalIdentifier.SSN)?.ssn,
            firstName = p.firstName,
            lastName = p.lastName,
            email = p.email,
            phone = p.phone,
            backupPhone = p.backupPhone,
            language = p.language,
            dateOfBirth = p.dateOfBirth,
            dateOfDeath = p.dateOfDeath,
            streetAddress = p.streetAddress,
            postOffice = p.postOffice,
            postalCode = p.postalCode,
            residenceCode = p.residenceCode,
            restrictedDetailsEnabled = p.restrictedDetailsEnabled,
            invoiceRecipientName = p.invoiceRecipientName,
            invoicingStreetAddress = p.invoicingStreetAddress,
            invoicingPostalCode = p.invoicingPostalCode,
            invoicingPostOffice = p.invoicingPostOffice,
            forceManualFeeDecisions = p.forceManualFeeDecisions
        )
    }
}

data class ContactInfo(
    val email: String,
    val phone: String,
    val backupPhone: String,
    val invoiceRecipientName: String?,
    val invoicingStreetAddress: String?,
    val invoicingPostalCode: String?,
    val invoicingPostOffice: String?,
    val forceManualFeeDecisions: Boolean = false
)

data class PersonPatch(
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: LocalDate? = null,
    val email: String? = null,
    val phone: String? = null,
    val backupPhone: String? = null,
    val streetAddress: String? = null,
    val postalCode: String? = null,
    val postOffice: String? = null,
    val invoiceRecipientName: String? = null,
    val invoicingStreetAddress: String? = null,
    val invoicingPostalCode: String? = null,
    val invoicingPostOffice: String? = null,
    val forceManualFeeDecisions: Boolean? = null
)

data class PersonWithChildrenDTO(
    val id: UUID,
    val socialSecurityNumber: String?,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate?,
    val firstName: String,
    val lastName: String,
    val addresses: Set<PersonAddressDTO> = emptySet(),
    val residenceCode: String?,
    val children: List<PersonWithChildrenDTO>,
    val nationalities: Set<Nationality>,
    val nativeLanguage: NativeLanguage?,
    val restrictedDetails: RestrictedDetails
)

data class PersonAddressDTO(
    val origin: Origin,
    val streetAddress: String,
    val postalCode: String,
    val city: String,
    val residenceCode: String?
) {
    enum class Origin {
        VTJ, MUNICIPAL, EVAKA
    }
}

data class RestrictedDetails(val enabled: Boolean, val endDate: LocalDate? = null)

private fun upsertVtjGuardians(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): VtjPersonDTO {
    val child = upsertVtjPerson(tx, vtjPersonDTO)
    val guardians = vtjPersonDTO.guardians.map { upsertVtjPerson(tx, it) }
    createOrReplaceChildRelationships(
        tx,
        childId = child.id,
        guardianIds = guardians.map { it.id }
    )
    initChildIfNotExists(tx, listOf(child.id))
    return child.toVtjPersonDTO().copy(guardians = guardians.map { it.toVtjPersonDTO() })
}

private fun upsertVtjChildren(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): VtjPersonDTO {
    val guardian = upsertVtjPerson(tx, vtjPersonDTO)
    val children = vtjPersonDTO.children.map { upsertVtjPerson(tx, it) }
    createOrReplaceGuardianRelationships(
        tx,
        guardianId = guardian.id,
        childIds = children.map { it.id }
    )
    initChildIfNotExists(tx, children.map { it.id })
    return guardian.toVtjPersonDTO().copy(children = children.map { it.toVtjPersonDTO() })
}

private fun upsertVtjPerson(tx: Database.Transaction, inputPerson: VtjPersonDTO): PersonDTO {
    val existingPerson = tx.lockPersonBySSN(inputPerson.socialSecurityNumber)

    return if (existingPerson == null) {
        val newPerson = newPersonFromVtjData(inputPerson)
        createPersonFromVtj(tx, newPerson)
    } else {
        val updatedPerson = getPersonWithUpdatedProperties(inputPerson, existingPerson)
        tx.updatePersonFromVtj(updatedPerson)
    }
}

private fun initChildIfNotExists(tx: Database.Transaction, childIds: List<UUID>) {
    childIds.forEach { childId ->
        if (tx.getChild(childId) == null)
            tx.createChild(Child(id = childId, additionalInformation = AdditionalInformation()))
    }
}

private fun createOrReplaceGuardianRelationships(tx: Database.Transaction, guardianId: UUID, childIds: List<UUID>) {
    tx.deleteGuardianChildRelationShips(guardianId)
    tx.insertGuardianChildren(guardianId, childIds)
    tx.updateVtjDependantsQueriedTimestamp(guardianId)
}

private fun createOrReplaceChildRelationships(tx: Database.Transaction, childId: UUID, guardianIds: List<UUID>) {
    tx.deleteChildGuardianRelationships(childId)
    tx.insertChildGuardians(childId, guardianIds)
    tx.updateVtjGuardiansQueriedTimestamp(childId)
}

private fun newPersonFromVtjData(inputPerson: VtjPersonDTO): PersonDTO = PersonDTO(
    id = inputPerson.id, // This will be overwritten
    identity = ExternalIdentifier.SSN.getInstance(inputPerson.socialSecurityNumber),
    firstName = inputPerson.firstName,
    lastName = inputPerson.lastName,

    streetAddress = getStreetAddressByLanguage(inputPerson),
    postalCode = inputPerson.postalCode,
    postOffice = getPostOfficeByLanguage(inputPerson),
    residenceCode = inputPerson.residenceCode,

    restrictedDetailsEnabled = inputPerson.restrictedDetailsEnabled,
    restrictedDetailsEndDate = inputPerson.restrictedDetailsEndDate,
    nationalities = inputPerson.nationalities.map { it.countryCode },
    language = inputPerson.nativeLanguage?.code ?: "",

    dateOfBirth = inputPerson.dateOfBirth,
    dateOfDeath = inputPerson.dateOfDeath,
    email = null,
    phone = null,
    backupPhone = ""
)

private fun getPersonWithUpdatedProperties(sourcePerson: VtjPersonDTO, existingPerson: PersonDTO): PersonDTO =
    existingPerson.copy(
        firstName = sourcePerson.firstName,
        lastName = sourcePerson.lastName,
        dateOfBirth = sourcePerson.dateOfBirth,
        dateOfDeath = sourcePerson.dateOfDeath,

        streetAddress = getStreetAddressByLanguage(sourcePerson),
        postalCode = sourcePerson.postalCode,
        postOffice = getPostOfficeByLanguage(sourcePerson),
        residenceCode = sourcePerson.residenceCode,

        restrictedDetailsEnabled = sourcePerson.restrictedDetailsEnabled,
        restrictedDetailsEndDate = sourcePerson.restrictedDetailsEndDate,
        nationalities = sourcePerson.nationalities.map { it.countryCode },
        language = sourcePerson.nativeLanguage?.code ?: ""
    )

private fun getStreetAddressByLanguage(vtjPerson: VtjPersonDTO): String {
    if (vtjPerson.nativeLanguage == null || vtjPerson.streetAddressSe == "") {
        return vtjPerson.streetAddress
    }

    return when (vtjPerson.nativeLanguage.code) {
        "sv" -> vtjPerson.streetAddressSe
        else -> vtjPerson.streetAddress
    }
}

private fun getPostOfficeByLanguage(vtjPerson: VtjPersonDTO): String {
    if (vtjPerson.nativeLanguage == null || vtjPerson.citySe == "") {
        return vtjPerson.city
    }

    return when (vtjPerson.nativeLanguage.code) {
        "sv" -> vtjPerson.citySe
        else -> vtjPerson.city
    }
}

private fun Database.Transaction.updateVtjDependantsQueriedTimestamp(personId: UUID) =
    createUpdate("UPDATE person SET vtj_dependants_queried = :now WHERE id = :personId")
        .bind("personId", personId)
        .bind("now", HelsinkiDateTime.now())
        .execute()

private fun Database.Transaction.updateVtjGuardiansQueriedTimestamp(personId: UUID) =
    createUpdate("UPDATE person SET vtj_guardians_queried = :now WHERE id = :personId")
        .bind("personId", personId)
        .bind("now", HelsinkiDateTime.now())
        .execute()
