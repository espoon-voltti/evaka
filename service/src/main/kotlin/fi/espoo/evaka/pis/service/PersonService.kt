// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.decision.DecisionSendAddress
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.domain.PersonDetailed
import fi.espoo.evaka.pdfgen.Page
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pdfgen.Template
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.getDependantGuardians
import fi.espoo.evaka.pis.getGuardianDependants
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.lockPersonBySSN
import fi.espoo.evaka.pis.updateNonSsnPersonDetails
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.pis.updatePersonNonVtjDetails
import fi.espoo.evaka.pis.updatePersonSsnAddingDisabled
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.Rectangle
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

private val logger = KotlinLogging.logger {}

@Service
class PersonService(private val personDetailsService: IPersonDetailsService) {
    // Does a request to VTJ if the person has a SSN and updates person data
    fun getUpToDatePersonFromVtj(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        id: PersonId,
    ): PersonDTO? {
        val person = tx.getPersonById(id) ?: return null
        return if (person.identity is ExternalIdentifier.SSN) {
            val personDetails =
                personDetailsService.getBasicDetailsFor(
                    IPersonDetailsService.DetailsQuery(user.evakaUserId, person.identity)
                )
            upsertVtjPerson(tx, personDetails.mapToDto())
        } else {
            person
        }
    }

    fun personsLiveInTheSameAddress(
        db: Database.Read,
        person1Id: PersonId,
        person2Id: PersonId,
    ): Boolean {
        val person1 = db.getPersonById(person1Id)
        val person2 = db.getPersonById(person2Id)

        return personsLiveInTheSameAddress(person1, person2)
    }

    fun personsLiveInTheSameAddress(person1: PersonDTO?, person2: PersonDTO?): Boolean {
        return personsHaveSameResidenceCode(person1, person2) ||
            personsHaveSameAddress(person1, person2)
    }

    private fun personsHaveSameResidenceCode(person1: PersonDTO?, person2: PersonDTO?): Boolean {
        return person1 != null &&
            person2 != null &&
            !person1.restrictedDetailsEnabled &&
            !person2.restrictedDetailsEnabled &&
            person1.residenceCode.isNotBlank() &&
            person2.residenceCode.isNotBlank() &&
            person1.residenceCode == person2.residenceCode
    }

    private fun personsHaveSameAddress(person1: PersonDTO?, person2: PersonDTO?): Boolean {
        return person1 != null &&
            person2 != null &&
            !person1.restrictedDetailsEnabled &&
            !person2.restrictedDetailsEnabled &&
            person1.streetAddress.isNotBlank() &&
            person2.streetAddress.isNotBlank() &&
            person1.postalCode.isNotBlank() &&
            person2.postalCode.isNotBlank() &&
            person1.streetAddress.lowercase() == person2.streetAddress.lowercase() &&
            person1.postalCode == person2.postalCode
    }

    // Does a request to VTJ if SSN is present and the person hasn't had their dependants
    // initialized
    fun getPersonWithChildren(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        id: PersonId,
        forceRefresh: Boolean = false,
    ): PersonWithChildrenDTO? {
        val guardian = tx.getPersonById(id) ?: return null

        return when (guardian.identity) {
            is ExternalIdentifier.NoID -> toPersonWithChildrenDTO(guardian)
            is ExternalIdentifier.SSN -> {
                if (forceRefresh || guardian.vtjDependantsQueried == null) {
                    getPersonWithDependants(user, guardian.identity)
                        .let { upsertVtjChildren(tx, it) }
                        .let {
                            toPersonWithChildrenDTO(
                                it,
                                guardian.preferredName,
                                guardian.phone,
                                guardian.backupPhone,
                                guardian.email,
                            )
                        }
                } else {
                    val children = tx.getGuardianDependants(id).map { toPersonWithChildrenDTO(it) }
                    toPersonWithChildrenDTO(guardian).copy(children = children)
                }
            }
        }
    }

    // Does a request to VTJ if SSN is present
    fun getGuardians(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        id: ChildId,
        forceRefresh: Boolean = false,
    ): List<PersonDTO> {
        val child = tx.getPersonById(id) ?: return emptyList()

        return when (child.identity) {
            is ExternalIdentifier.NoID -> emptyList()
            is ExternalIdentifier.SSN -> {
                if (forceRefresh || child.vtjGuardiansQueried == null) {
                    getPersonWithGuardians(user, child.identity)
                        .let { upsertVtjGuardians(tx, it) }
                        .guardians
                        .map(::toPersonDTO)
                } else {
                    tx.getDependantGuardians(id)
                }
            }
        }
    }

    // In extremely rare cases there might be more than 2 guardians, but it was agreed with product
    // management to use
    // just one of these as the other guardian.
    fun getOtherGuardian(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        otherGuardianId: PersonId,
        childId: ChildId,
    ): PersonDTO? =
        getGuardians(tx, user, childId).firstOrNull { guardian -> guardian.id != otherGuardianId }

    // Does a request to VTJ if person is not found in database or person data has not been
    // initialized from VTJ
    fun getOrCreatePerson(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        ssn: ExternalIdentifier.SSN,
        readonly: Boolean = false,
    ): PersonDTO? {
        val person = tx.getPersonBySSN(ssn.ssn)
        return if (person?.updatedFromVtj == null) {
            val personDetails =
                personDetailsService.getBasicDetailsFor(
                    IPersonDetailsService.DetailsQuery(user.evakaUserId, ssn)
                )
            if (readonly) return toPersonDTO(personDetails.mapToDto())

            upsertVtjPerson(tx, personDetails.mapToDto())
            tx.getPersonBySSN(ssn.ssn)
        } else {
            person
        }
    }

    fun patchUserDetails(tx: Database.Transaction, id: PersonId, data: PersonPatch): PersonDTO {
        val person = tx.getPersonById(id) ?: throw NotFound("Person $id not found")

        // People with SSN get basic details from VTJ which should not be modified
        when (person.identity) {
            is ExternalIdentifier.SSN -> tx.updatePersonNonVtjDetails(id, data)
            is ExternalIdentifier.NoID -> tx.updateNonSsnPersonDetails(id, data)
        }.exhaust()

        return tx.getPersonById(id)!!
    }

    fun addSsn(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        id: PersonId,
        ssn: ExternalIdentifier.SSN,
    ): PersonDTO {
        val person = tx.getPersonById(id) ?: throw NotFound("Person $id not found")

        return when (person.identity) {
            is ExternalIdentifier.SSN -> throw BadRequest("User already has ssn")
            is ExternalIdentifier.NoID -> {
                if (tx.getPersonBySSN(ssn.ssn) != null) {
                    throw Conflict("User with same ssn already exists")
                }

                val personDetails =
                    personDetailsService.getBasicDetailsFor(
                        IPersonDetailsService.DetailsQuery(user.evakaUserId, ssn)
                    )

                val updatedPerson =
                    getPersonWithUpdatedProperties(
                        personDetails.mapToDto(),
                        person.copy(identity = ssn),
                    )

                tx.updatePersonSsnAddingDisabled(id, false)
                tx.updatePersonFromVtj(updatedPerson)
                upsertVtjPerson(tx, personDetails.mapToDto())
            }
        }.exhaust()
    }

    fun disableSsn(tx: Database.Transaction, personId: PersonId, disabled: Boolean) {
        val person = tx.getPersonById(personId) ?: throw NotFound("Person $personId not found")

        if (person.identity is ExternalIdentifier.SSN) {
            throw BadRequest("Cannot disable a SSN, person $personId already has a SSN")
        }

        tx.updatePersonSsnAddingDisabled(personId, disabled)
    }

    // Does a request to VTJ
    private fun getPersonWithDependants(
        user: AuthenticatedUser,
        ssn: ExternalIdentifier.SSN,
    ): VtjPersonDTO {
        return personDetailsService
            .getPersonWithDependants(IPersonDetailsService.DetailsQuery(user.evakaUserId, ssn))
            .mapToDto()
    }

    // Does a request to VTJ
    private fun getPersonWithGuardians(
        user: AuthenticatedUser,
        ssn: ExternalIdentifier.SSN,
    ): VtjPersonDTO {
        return personDetailsService
            .getPersonWithGuardians(IPersonDetailsService.DetailsQuery(user.evakaUserId, ssn))
            .mapToDto()
    }

    private fun toPersonDTO(person: VtjPersonDTO): PersonDTO =
        PersonDTO(
            id = PersonId(person.id),
            duplicateOf = null,
            identity = ExternalIdentifier.SSN.getInstance(person.socialSecurityNumber),
            ssnAddingDisabled = false,
            firstName = person.firstName,
            lastName = person.lastName,
            preferredName = "",
            email = null,
            phone = "",
            backupPhone = "",
            language = person.nativeLanguage?.toString(),
            dateOfBirth = person.dateOfBirth,
            dateOfDeath = person.dateOfDeath,
            streetAddress = person.streetAddress,
            postalCode = person.postalCode,
            postOffice = person.city,
            residenceCode = person.residenceCode,
            restrictedDetailsEnabled = person.restrictedDetailsEnabled,
            restrictedDetailsEndDate = person.restrictedDetailsEndDate,
        )

    private fun toPersonWithChildrenDTO(
        person: VtjPersonDTO,
        preferredName: String = "",
        phone: String = "",
        backupPhone: String = "",
        email: String? = null,
    ): PersonWithChildrenDTO =
        PersonWithChildrenDTO(
            id = PersonId(person.id),
            duplicateOf = null,
            socialSecurityNumber = person.socialSecurityNumber,
            firstName = person.firstName,
            lastName = person.lastName,
            preferredName = preferredName,
            dateOfBirth = person.dateOfBirth,
            dateOfDeath = person.dateOfDeath,
            nationalities = person.nationalities.toSet(),
            nativeLanguage = person.nativeLanguage,
            restrictedDetails =
                RestrictedDetails(
                    enabled = person.restrictedDetailsEnabled,
                    endDate = person.restrictedDetailsEndDate,
                ),
            address =
                if (hasAddress(person) && !hasRestriction(person)) {
                    PersonAddressDTO(
                        origin = PersonAddressDTO.Origin.VTJ,
                        streetAddress = person.streetAddress,
                        postalCode = person.postalCode,
                        city = person.city,
                        residenceCode = person.residenceCode,
                    )
                } else {
                    PersonAddressDTO(
                        origin = PersonAddressDTO.Origin.VTJ,
                        streetAddress = "",
                        postalCode = "",
                        city = "",
                        residenceCode = "",
                    )
                },
            residenceCode = person.residenceCode,
            phone = phone,
            backupPhone = backupPhone,
            email = email,
            children = person.children.map(::toPersonWithChildrenDTO),
        )

    private fun toPersonWithChildrenDTO(person: PersonDTO): PersonWithChildrenDTO =
        PersonWithChildrenDTO(
            id = person.id,
            duplicateOf = person.duplicateOf,
            socialSecurityNumber = (person.identity as? ExternalIdentifier.SSN)?.toString(),
            firstName = person.firstName,
            lastName = person.lastName,
            preferredName = person.preferredName,
            dateOfBirth = person.dateOfBirth,
            dateOfDeath = person.dateOfDeath,
            nationalities = emptySet(),
            nativeLanguage = person.language?.let { NativeLanguage(languageName = it, code = it) },
            restrictedDetails = RestrictedDetails(enabled = false),
            address =
                PersonAddressDTO(
                    origin = PersonAddressDTO.Origin.EVAKA,
                    streetAddress = person.streetAddress,
                    postalCode = person.postalCode,
                    city = person.postOffice,
                    residenceCode = person.residenceCode,
                ),
            residenceCode = person.residenceCode,
            phone = person.phone,
            backupPhone = person.backupPhone,
            email = person.email,
            children = emptyList(),
        )

    private fun hasRestriction(person: VtjPersonDTO) = person.restrictedDetailsEnabled

    private fun hasAddress(person: VtjPersonDTO): Boolean =
        setOf(person.streetAddress, person.postalCode, person.city).all(String::isNotBlank)
}

data class PersonDTO(
    val id: PersonId,
    val duplicateOf: PersonId?,
    val identity: ExternalIdentifier,
    val ssnAddingDisabled: Boolean,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val email: String?,
    val phone: String,
    val backupPhone: String,
    val language: String?,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate? = null,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val residenceCode: String,
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
    val forceManualFeeDecisions: Boolean = false,
    val ophPersonOid: String? = "",
) {
    fun toVtjPersonDTO() =
        VtjPersonDTO(
            id = this.id.raw,
            firstName = this.firstName,
            lastName = this.lastName,
            dateOfBirth = this.dateOfBirth,
            dateOfDeath = this.dateOfDeath,
            socialSecurityNumber = this.identity.toString(),
            restrictedDetailsEndDate = this.restrictedDetailsEndDate,
            restrictedDetailsEnabled = this.restrictedDetailsEnabled,
            nativeLanguage = NativeLanguage(code = this.language ?: ""),
            streetAddress = this.streetAddress,
            postalCode = this.postalCode,
            city = this.postOffice,
            streetAddressSe = "",
            citySe = "",
            residenceCode = this.residenceCode,
        )
}

fun PersonDTO.hideNonPermittedPersonData(includeInvoiceAddress: Boolean, includeOphOid: Boolean) =
    this.let {
            if (includeInvoiceAddress) {
                it
            } else {
                it.copy(
                    invoiceRecipientName = "",
                    invoicingStreetAddress = "",
                    invoicingPostalCode = "",
                    invoicingPostOffice = "",
                    forceManualFeeDecisions = false,
                )
            }
        }
        .let {
            if (includeOphOid) {
                it
            } else {
                it.copy(ophPersonOid = "")
            }
        }

data class PersonJSON(
    val id: PersonId,
    val duplicateOf: PersonId?,
    val socialSecurityNumber: String? = null,
    val ssnAddingDisabled: Boolean,
    val firstName: String = "",
    val lastName: String = "",
    val email: String? = null,
    val phone: String = "",
    val backupPhone: String = "",
    val language: String? = null,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate? = null,
    val streetAddress: String = "",
    val postOffice: String = "",
    val postalCode: String = "",
    val residenceCode: String = "",
    val restrictedDetailsEnabled: Boolean = false,
    val invoiceRecipientName: String = "",
    val invoicingStreetAddress: String = "",
    val invoicingPostalCode: String = "",
    val invoicingPostOffice: String = "",
    val forceManualFeeDecisions: Boolean = false,
    val ophPersonOid: String? = null,
    val updatedFromVtj: HelsinkiDateTime? = null,
) {
    companion object {
        fun from(p: PersonDTO): PersonJSON =
            PersonJSON(
                id = p.id,
                duplicateOf = p.duplicateOf,
                socialSecurityNumber = (p.identity as? ExternalIdentifier.SSN)?.ssn,
                ssnAddingDisabled = p.ssnAddingDisabled,
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
                forceManualFeeDecisions = p.forceManualFeeDecisions,
                ophPersonOid = p.ophPersonOid,
                updatedFromVtj = p.updatedFromVtj,
            )
    }
}

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
    val forceManualFeeDecisions: Boolean? = null,
    val ophPersonOid: String? = null,
)

data class PersonWithChildrenDTO(
    val id: PersonId,
    val duplicateOf: PersonId?,
    val socialSecurityNumber: String?,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate?,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val address: PersonAddressDTO,
    val residenceCode: String,
    val phone: String,
    val backupPhone: String,
    val email: String?,
    val children: List<PersonWithChildrenDTO>,
    val nationalities: Set<Nationality>,
    val nativeLanguage: NativeLanguage?,
    val restrictedDetails: RestrictedDetails,
) {
    fun toPersonDTO() =
        PersonDTO(
            id = id,
            duplicateOf = duplicateOf,
            identity =
                when (socialSecurityNumber) {
                    is String -> ExternalIdentifier.SSN.getInstance(socialSecurityNumber)
                    else -> ExternalIdentifier.NoID
                },
            ssnAddingDisabled = false,
            dateOfBirth = dateOfBirth,
            dateOfDeath = dateOfDeath,
            firstName = firstName,
            lastName = lastName,
            preferredName = preferredName,
            residenceCode = residenceCode,
            streetAddress = address.streetAddress,
            postalCode = address.postalCode,
            postOffice = address.city,
            restrictedDetailsEnabled = restrictedDetails.enabled,
            restrictedDetailsEndDate = restrictedDetails.endDate,
            language = nativeLanguage?.code,
            email = email,
            phone = phone,
            backupPhone = backupPhone,
        )
}

data class PersonAddressDTO(
    val origin: Origin,
    val streetAddress: String,
    val postalCode: String,
    val city: String,
    val residenceCode: String,
) {
    enum class Origin {
        VTJ,
        MUNICIPAL,
        EVAKA,
    }
}

data class RestrictedDetails(val enabled: Boolean, val endDate: LocalDate? = null)

private fun upsertVtjGuardians(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): VtjPersonDTO {
    val child = upsertVtjPerson(tx, vtjPersonDTO)
    initChildIfNotExists(tx, child.id)
    val guardians =
        vtjPersonDTO.guardians
            .map { upsertVtjPerson(tx, it) }
            .filterNot { tx.isGuardianBlocked(it.id, child.id) }
    createOrReplaceChildRelationships(tx, childId = child.id, guardianIds = guardians.map { it.id })
    logger.info("Created or replaced child ${child.id} guardians as ${guardians.map { it.id }}")
    return child.toVtjPersonDTO().copy(guardians = guardians.map { it.toVtjPersonDTO() })
}

private fun upsertVtjChildren(tx: Database.Transaction, vtjPersonDTO: VtjPersonDTO): VtjPersonDTO {
    val guardian = upsertVtjPerson(tx, vtjPersonDTO)
    val children =
        vtjPersonDTO.children
            .map { upsertVtjPerson(tx, it).also { child -> initChildIfNotExists(tx, child.id) } }
            .filterNot { tx.isGuardianBlocked(guardian.id, it.id) }
    createOrReplaceGuardianRelationships(
        tx,
        guardianId = guardian.id,
        childIds = children.map { it.id },
    )
    logger.info("Created or replaced guardian ${guardian.id} children as ${children.map { it.id }}")

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

private fun initChildIfNotExists(tx: Database.Transaction, childId: ChildId) {
    if (tx.getChild(childId) == null) {
        tx.createChild(Child(id = childId, additionalInformation = AdditionalInformation()))
    }
}

private fun createOrReplaceGuardianRelationships(
    tx: Database.Transaction,
    guardianId: PersonId,
    childIds: List<ChildId>,
) {
    tx.deleteGuardianChildRelationShips(guardianId)
    tx.insertGuardianChildren(guardianId, childIds)
    tx.updateVtjDependantsQueriedTimestamp(guardianId)
}

private fun createOrReplaceChildRelationships(
    tx: Database.Transaction,
    childId: ChildId,
    guardianIds: List<PersonId>,
) {
    tx.deleteChildGuardianRelationships(childId)
    tx.insertChildGuardians(childId, guardianIds)
    tx.updateVtjGuardiansQueriedTimestamp(childId)
}

private fun newPersonFromVtjData(inputPerson: VtjPersonDTO): PersonDTO =
    PersonDTO(
        id = PersonId(inputPerson.id), // This will be overwritten
        duplicateOf = null,
        identity = ExternalIdentifier.SSN.getInstance(inputPerson.socialSecurityNumber),
        ssnAddingDisabled = false,
        firstName = inputPerson.firstName,
        lastName = inputPerson.lastName,
        preferredName = "",
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
        phone = "",
        backupPhone = "",
    )

private fun getPersonWithUpdatedProperties(
    sourcePerson: VtjPersonDTO,
    existingPerson: PersonDTO,
): PersonDTO =
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
        language = sourcePerson.nativeLanguage?.code ?: "",
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

private fun Database.Transaction.updateVtjDependantsQueriedTimestamp(personId: PersonId) =
    createUpdate {
            sql(
                "UPDATE person SET vtj_dependants_queried = ${bind(HelsinkiDateTime.now())} WHERE id = ${bind(personId)}"
            )
        }
        .execute()

private fun Database.Transaction.updateVtjGuardiansQueriedTimestamp(personId: ChildId) =
    createUpdate {
            sql(
                "UPDATE person SET vtj_guardians_queried = ${bind(HelsinkiDateTime.now())} WHERE id = ${bind(personId)}"
            )
        }
        .execute()

fun createAddressPagePdf(
    pdfGenerator: PdfGenerator,
    today: LocalDate,
    envelopeWindowPosition: Rectangle,
    guardian: PersonDTO,
): Document {
    // template path hardcoded to prevent need for customization
    val template = "address-page/address-page"
    val personDetails =
        guardian.let {
            val firstWordOfFirstName = it.firstName.trim().substringBefore(' ')

            PersonDetailed(
                dateOfBirth = it.dateOfBirth,
                postOffice = it.postOffice,
                streetAddress = it.streetAddress,
                postalCode = it.postalCode,
                firstName = firstWordOfFirstName,
                lastName = it.lastName,
                restrictedDetailsEnabled = it.restrictedDetailsEnabled,
                id = it.id,
            )
        }

    val page =
        Page(
            Template(template),
            Context().apply {
                setVariable("window", envelopeWindowPosition)
                setVariable("guardian", personDetails)
                setVariable("sendAddress", DecisionSendAddress.fromPerson(personDetails))
            },
        )

    return Document(
        name = "osoitesivu_${guardian.lastName}_$today.pdf",
        bytes = pdfGenerator.render(page),
        contentType = "application/pdf",
    )
}
