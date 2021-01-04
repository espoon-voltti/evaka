// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.addSSNToPerson
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.updatePersonContactInfo
import fi.espoo.evaka.pis.updatePersonDetails
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.PersonDetails
import fi.espoo.evaka.vtjclient.service.persondetails.PersonStorageService
import fi.espoo.evaka.vtjclient.usecases.dto.PersonResult
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class PersonService(
    private val personDetailsService: IPersonDetailsService,
    private val personStorageService: PersonStorageService
) {
    private val forceRefreshIntervalSeconds = 1 * 24 * 60 * 60 // 1 day

    // Does a request to VTJ if data is stale
    fun getUpToDatePerson(tx: Database.Transaction, user: AuthenticatedUser, id: UUID): PersonDTO? {
        val person = tx.handle.getPersonById(id) ?: return null
        return if (person.identity is ExternalIdentifier.SSN && vtjDataIsStale(person)) {
            val personDetails =
                personDetailsService.getBasicDetailsFor(
                    IPersonDetailsService.DetailsQuery(user, person.identity)
                )
            if (personDetails is PersonDetails.Result) {
                val personResult = PersonResult.Result(personDetails.vtjPerson.mapToDto())
                personStorageService.upsertVtjPerson(tx, personResult)
                tx.handle.getPersonById(id)
            } else {
                hideNonDisclosureInfo(person)
            }
        } else {
            person
        }
    }

    fun personsLiveInTheSameAddress(db: Database.Read, user: AuthenticatedUser, person1Id: UUID, person2Id: UUID): Boolean {
        val person1 = db.handle.getPersonById(person1Id)
        val person2 = db.handle.getPersonById(person2Id)

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
            person1.streetAddress.toLowerCase().equals(person2.streetAddress.toLowerCase()) &&
            person1.postalCode.equals(person2.postalCode)
    }

    // Does a request to VTJ if SSN is present
    fun getUpToDatePersonWithChildren(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        id: UUID
    ): PersonWithChildrenDTO? {
        val guardian = tx.handle.getPersonById(id) ?: return null

        return when (guardian.identity) {
            is ExternalIdentifier.NoID -> toPersonWithChildrenDTO(guardian)
            is ExternalIdentifier.SSN ->
                getPersonWithDependants(user, guardian.identity)
                    ?.let {
                        personStorageService.upsertVtjGuardianAndChildren(
                            tx,
                            PersonResult.Result(it)
                        )
                    }
                    ?.let {
                        when (it) {
                            is PersonResult.Error -> throw IllegalStateException(it.msg)
                            is PersonResult.NotFound -> null
                            is PersonResult.Result -> toPersonWithChildrenDTO(it.vtjPersonDTO)
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

    // Does a request to VTJ if SSN is present
    fun getGuardians(tx: Database.Transaction, user: AuthenticatedUser, id: UUID): List<PersonDTO> {
        val child = tx.handle.getPersonById(id) ?: return emptyList()

        return when (child.identity) {
            is ExternalIdentifier.NoID -> emptyList()
            is ExternalIdentifier.SSN ->
                getPersonWithGuardians(user, child.identity)
                    ?.let { personStorageService.upsertVtjChildAndGuardians(tx, PersonResult.Result(it)) }
                    ?.let {
                        when (it) {
                            is PersonResult.Error -> throw IllegalStateException(it.msg)
                            is PersonResult.NotFound -> emptyList()
                            is PersonResult.Result -> it.vtjPersonDTO.guardians.map(::toPersonDTO)
                        }
                    } ?: emptyList()
        }
    }

    val SECONDS_IN_30_DAYS: Long = 60 * 60 * 24 * 30

    // If 1 or more evaka guardians is found, return those, otherwise get from VTJ
    fun getEvakaOrVtjGuardians(tx: Database.Transaction, user: AuthenticatedUser, id: UUID): List<PersonDTO> {
        val child = tx.handle.getPersonById(id) ?: return emptyList()

        return when (child.identity) {
            is ExternalIdentifier.NoID -> emptyList()
            is ExternalIdentifier.SSN -> {
                val evakaGuardians = getChildGuardians(tx.handle, id).mapNotNull { guardianId -> tx.handle.getPersonById(guardianId) }

                if (evakaGuardians.size > 1 && evakaGuardians.all { guardian ->
                    if (guardian.updatedFromVtj != null) guardian.updatedFromVtj.isAfter(Instant.now().minusSeconds(SECONDS_IN_30_DAYS)) else false
                }
                )
                    evakaGuardians
                else
                    getPersonWithGuardians(user, child.identity)
                        ?.let { personStorageService.upsertVtjChildAndGuardians(tx, PersonResult.Result(it)) }
                        ?.let {
                            when (it) {
                                is PersonResult.Error -> throw IllegalStateException(it.msg)
                                is PersonResult.NotFound -> emptyList()
                                is PersonResult.Result -> it.vtjPersonDTO.guardians.map(::toPersonDTO)
                            }
                        } ?: emptyList()
            }
        }
    }

    // Does a request to VTJ if data is stale
    fun getOrCreatePerson(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        ssn: ExternalIdentifier.SSN,
        updateStale: Boolean = true
    ): PersonDTO? {
        val person = tx.handle.getPersonBySSN(ssn.ssn)
        return if (person == null || (updateStale && vtjDataIsStale(person))) {
            val personDetails = personDetailsService.getBasicDetailsFor(
                IPersonDetailsService.DetailsQuery(user, ssn)
            )
            if (personDetails is PersonDetails.Result) {
                val personResult = PersonResult.Result(personDetails.vtjPerson.mapToDto())
                personStorageService.upsertVtjPerson(tx, personResult)
                tx.handle.getPersonBySSN(ssn.ssn)
            } else {
                hideNonDisclosureInfo(person)
            }
        } else {
            person
        }
    }

    // Expensive VTJ request
    fun getPersonFromVTJ(user: AuthenticatedUser, ssn: ExternalIdentifier.SSN): PersonDTO? {
        val personDetails = personDetailsService.getBasicDetailsFor(
            IPersonDetailsService.DetailsQuery(user, ssn)
        )
        return if (personDetails is PersonDetails.Result) {
            personDetails.vtjPerson.mapToDto().let { toPersonDTO(it) }
        } else null
    }

    fun patchUserDetails(tx: Database.Transaction, id: UUID, data: PersonPatch): PersonDTO {
        val person = tx.handle.getPersonById(id) ?: throw NotFound("Person $id not found")

        when (person.identity) {
            is ExternalIdentifier.SSN -> tx.handle.updatePersonContactInfo(
                id,
                ContactInfo(
                    email = data.email ?: person.email ?: "",
                    phone = data.phone ?: person.phone ?: "",
                    invoiceRecipientName = data.invoiceRecipientName ?: person.invoiceRecipientName,
                    invoicingStreetAddress = data.invoicingStreetAddress ?: person.invoicingStreetAddress,
                    invoicingPostalCode = data.invoicingPostalCode ?: person.invoicingPostalCode,
                    invoicingPostOffice = data.invoicingPostOffice ?: person.invoicingPostOffice,
                    forceManualFeeDecisions = data.forceManualFeeDecisions ?: person.forceManualFeeDecisions
                )
            )
            is ExternalIdentifier.NoID -> tx.handle.updatePersonDetails(id, data)
        }.exhaust()

        return tx.handle.getPersonById(id)!!
    }

    fun addSsn(tx: Database.Transaction, user: AuthenticatedUser, id: UUID, ssn: ExternalIdentifier.SSN) {
        val person = tx.handle.getPersonById(id) ?: throw NotFound("Person $id not found")

        when (person.identity) {
            is ExternalIdentifier.SSN -> throw BadRequest("User already has ssn")
            is ExternalIdentifier.NoID -> {
                if (tx.handle.getPersonBySSN(ssn.ssn) != null) {
                    throw Conflict("User with same ssn already exists")
                }
                tx.handle.addSSNToPerson(id, ssn.toString())
            }
        }.exhaust()
    }

    private fun vtjDataIsStale(person: PersonDTO): Boolean {
        return person.updatedFromVtj
            ?.let { it < Instant.now().minusSeconds(forceRefreshIntervalSeconds.toLong()) } ?: true
    }

    // Does a request to VTJ
    fun getPersonWithDependants(user: AuthenticatedUser, ssn: ExternalIdentifier.SSN): VtjPersonDTO? {
        return personDetailsService.getPersonWithDependants(IPersonDetailsService.DetailsQuery(user, ssn))
            .let { it as? PersonDetails.Result }
            ?.let { result -> result.vtjPerson.mapToDto() }
    }

    // Does a request to VTJ
    private fun getPersonWithGuardians(user: AuthenticatedUser, ssn: ExternalIdentifier.SSN): VtjPersonDTO? {
        return personDetailsService.getPersonWithGuardians(IPersonDetailsService.DetailsQuery(user, ssn))
            .let { it as? PersonDetails.Result }
            ?.let { result -> result.vtjPerson.mapToDto() }
    }

    private fun hideNonDisclosureInfo(person: PersonDTO?): PersonDTO? {
        return person?.copy(
            streetAddress = "",
            postalCode = "",
            postOffice = ""
        )
    }

    private fun toPersonDTO(person: VtjPersonDTO): PersonDTO =
        PersonDTO(
            id = person.id,
            identity = ExternalIdentifier.SSN.getInstance(person.socialSecurityNumber),
            customerId = null,
            firstName = person.firstName,
            lastName = person.lastName,
            email = null,
            phone = null,
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
            children = person.children.map(::toPersonWithChildrenDTO),
            source = person.source
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
            children = emptyList(),
            source = PersonDataSource.DATABASE
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
    val customerId: Long?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
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
    val updatedFromVtj: Instant? = null,
    val invoiceRecipientName: String = "",
    val invoicingStreetAddress: String = "",
    val invoicingPostalCode: String = "",
    val invoicingPostOffice: String = "",
    val forceManualFeeDecisions: Boolean = false
)

data class PersonJSON(
    val id: UUID,
    val customerId: Long? = null,
    val socialSecurityNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
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
            customerId = p.customerId,
            socialSecurityNumber = (p.identity as? ExternalIdentifier.SSN)?.ssn,
            firstName = p.firstName,
            lastName = p.lastName,
            email = p.email,
            phone = p.phone,
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

data class PersonIdentityRequest(
    val identity: ExternalIdentifier.SSN,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val language: String?
)

data class ContactInfo(
    val email: String,
    val phone: String,
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
    val restrictedDetails: RestrictedDetails,
    val source: PersonDataSource
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
