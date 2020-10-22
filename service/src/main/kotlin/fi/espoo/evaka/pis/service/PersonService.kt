// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.VolttiIdentifier
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.dao.PersonDAO
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.withSpringTx
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
import org.springframework.transaction.PlatformTransactionManager
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class PersonService(
    private val txManager: PlatformTransactionManager,
    private val personDAO: PersonDAO,
    private val personDetailsService: IPersonDetailsService,
    private val personStorageService: PersonStorageService
) {
    private val forceRefreshIntervalSeconds = 1 * 24 * 60 * 60 // 1 day

    fun createEmpty() = withSpringTx(txManager) { personDAO.createEmpty() }

    fun createNoSsnPerson(person: CreatePersonBody): UUID = withSpringTx(txManager) {
        personDAO.createPerson(person)
    }

    fun getOrCreatePersonIdentity(person: PersonIdentityRequest): PersonDTO = withSpringTx(txManager) {
        personDAO.getOrCreatePersonIdentity(person)
    }

    fun getPerson(id: VolttiIdentifier): PersonDTO? = withSpringTx(txManager) {
        personDAO.getPersonByVolttiId(id)
    }

    fun getUpToDatePerson(user: AuthenticatedUser, id: VolttiIdentifier): PersonDTO? {
        val person = personDAO.getPersonByVolttiId(id) ?: return null
        if (person.identity is ExternalIdentifier.SSN && vtjDataIsStale(person)) {
            val personDetails =
                personDetailsService.getBasicDetailsFor(
                    IPersonDetailsService.DetailsQuery(user, person.identity)
                )
            if (personDetails is PersonDetails.Result) {
                val personResult = PersonResult.Result(personDetails.vtjPerson.mapToDto())
                personStorageService.upsertVtjPerson(personResult)
                return personDAO.getPersonByVolttiId(id)
            } else {
                return hideNonDisclosureInfo(person)
            }
        } else {
            return person
        }
    }

    fun personsLiveInTheSameAddress(user: AuthenticatedUser, person1Id: UUID, person2Id: UUID): Boolean =
        withSpringTx(txManager) {
            val person1 = personDAO.getPersonByVolttiId(person1Id)
            val person2 = personDAO.getPersonByVolttiId(person2Id)

            personsHaveSameResidenceCode(person1, person2) || personsHaveSameAddress(person1, person2)
        }

    private fun personsHaveSameResidenceCode(person1: PersonDTO?, person2: PersonDTO?): Boolean {
        return person1 != null && person2 != null &&
            !person1.restrictedDetailsEnabled && !person2.restrictedDetailsEnabled &&
            person1.residenceCode != null && person2.residenceCode != null &&
            person1.residenceCode == person2.residenceCode
    }

    private fun personsHaveSameAddress(person1: PersonDTO?, person2: PersonDTO?): Boolean {
        return person1 != null && person2 != null &&
            !person1.restrictedDetailsEnabled && !person2.restrictedDetailsEnabled &&
            person1.streetAddress != null && person2.streetAddress != null &&
            person1.postalCode != null && person2.postalCode != null &&
            person1.streetAddress.toLowerCase().equals(person2.streetAddress.toLowerCase()) &&
            person1.postalCode.equals(person2.postalCode)
    }

    fun getDeceased(sinceDate: LocalDate): List<PersonDTO> =
        withSpringTx(txManager) { personDAO.getDeceased(sinceDate) }

    fun getUpToDatePersonWithChildren(user: AuthenticatedUser, id: VolttiIdentifier): PersonWithChildrenDTO? =
        withSpringTx(txManager) {
            val guardian = personDAO.getPersonByVolttiId(id) ?: return@withSpringTx null

            when (guardian.identity) {
                is ExternalIdentifier.NoID -> toPersonWithChildrenDTO(guardian)
                is ExternalIdentifier.SSN ->
                    getPersonWithDependants(user, guardian.identity)
                        ?.let { personStorageService.upsertVtjGuardianAndChildren(PersonResult.Result(it)) }
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
        user: AuthenticatedUser,
        otherGuardianId: VolttiIdentifier,
        childId: VolttiIdentifier
    ): PersonDTO? = withSpringTx(txManager) {
        getGuardians(user, childId).filter { guardian -> guardian.id != otherGuardianId }.firstOrNull()
    }

    fun getGuardians(user: AuthenticatedUser, id: VolttiIdentifier): List<PersonDTO> = withSpringTx(txManager) {
        val child = personDAO.getPersonByVolttiId(id) ?: return@withSpringTx emptyList()

        when (child.identity) {
            is ExternalIdentifier.NoID -> emptyList()
            is ExternalIdentifier.SSN ->
                getPersonWithGuardians(user, child.identity)
                    ?.let { personStorageService.upsertVtjChildAndGuardians(PersonResult.Result(it)) }
                    ?.let {
                        when (it) {
                            is PersonResult.Error -> throw IllegalStateException(it.msg)
                            is PersonResult.NotFound -> emptyList()
                            is PersonResult.Result -> it.vtjPersonDTO.guardians.map(::toPersonDTO)
                        }
                    } ?: emptyList()
        }
    }

    fun getOrCreatePerson(
        user: AuthenticatedUser,
        ssn: ExternalIdentifier.SSN,
        updateStale: Boolean = true
    ): PersonDTO? = withSpringTx(txManager) {
        val person = personDAO.getPersonByExternalId(ssn)
        if (person == null || (updateStale && vtjDataIsStale(person))) {
            val personDetails = personDetailsService.getBasicDetailsFor(
                IPersonDetailsService.DetailsQuery(user, ssn)
            )
            if (personDetails is PersonDetails.Result) {
                val personResult = PersonResult.Result(personDetails.vtjPerson.mapToDto())
                personStorageService.upsertVtjPerson(personResult)
                personDAO.getPersonByExternalId(ssn)
            } else {
                hideNonDisclosureInfo(person)
            }
        } else {
            person
        }
    }

    fun getPersonFromVTJ(user: AuthenticatedUser, ssn: ExternalIdentifier.SSN): PersonDTO? = withSpringTx(txManager) {
        val personDetails = personDetailsService.getBasicDetailsFor(
            IPersonDetailsService.DetailsQuery(user, ssn)
        )
        if (personDetails is PersonDetails.Result) {
            personDetails.vtjPerson.mapToDto().let { toPersonDTO(it) }
        } else null
    }

    fun updateEndUsersContactInfo(id: VolttiIdentifier, contactInfo: ContactInfo): Boolean = withSpringTx(txManager) {
        personDAO.updateEndUsersContactInfo(id, contactInfo)
    }

    fun patchUserDetails(id: VolttiIdentifier, data: PersonPatch): PersonDTO = withSpringTx(txManager) {
        val person = getPerson(id) ?: throw NotFound("Person $id not found")

        when (person.identity) {
            is ExternalIdentifier.SSN -> personDAO.updateEndUsersContactInfo(
                id,
                ContactInfo(
                    email = data.email ?: person.email ?: "",
                    phone = data.phone ?: person.phone ?: "",
                    invoiceRecipientName = data.invoiceRecipientName ?: person.invoiceRecipientName ?: "",
                    invoicingStreetAddress = data.invoicingStreetAddress ?: person.invoicingStreetAddress ?: "",
                    invoicingPostalCode = data.invoicingPostalCode ?: person.invoicingPostalCode ?: "",
                    invoicingPostOffice = data.invoicingPostOffice ?: person.invoicingPostOffice ?: "",
                    forceManualFeeDecisions = data.forceManualFeeDecisions ?: person.forceManualFeeDecisions ?: false
                )
            )
            is ExternalIdentifier.NoID -> personDAO.updateEndUserDetails(id, data)
        }.exhaust()

        getPerson(id)!!
    }

    fun addSsn(user: AuthenticatedUser, id: VolttiIdentifier, ssn: ExternalIdentifier.SSN): PersonDTO =
        withSpringTx(txManager) {
            val person = getPerson(id) ?: throw NotFound("Person $id not found")

            when (person.identity) {
                is ExternalIdentifier.SSN -> throw BadRequest("User already has ssn")
                is ExternalIdentifier.NoID -> {
                    if (personDAO.getPersonByExternalId(ssn) != null) {
                        throw Conflict("User with same ssn already exists")
                    }
                    personDAO.addSsn(id, ssn.toString())
                }
            }.exhaust()

            getUpToDatePerson(user, id)!!
        }

    fun findBySearchTerms(searchTerms: String, orderBy: String, sortDirection: String): List<PersonDTO> =
        withSpringTx(txManager) {
            personDAO.findBySearchTerms(searchTerms, orderBy, sortDirection)
        }

    private fun vtjDataIsStale(person: PersonDTO): Boolean {
        return person.updatedFromVtj
            ?.let { it < Instant.now().minusSeconds(forceRefreshIntervalSeconds.toLong()) } ?: true
    }

    fun getPersonWithDependants(user: AuthenticatedUser, ssn: ExternalIdentifier.SSN): VtjPersonDTO? {
        return personDetailsService.getPersonWithDependants(IPersonDetailsService.DetailsQuery(user, ssn))
            .let { it as? PersonDetails.Result }
            ?.let { result -> result.vtjPerson.mapToDto() }
    }

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
    val id: VolttiIdentifier,
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
    val id: VolttiIdentifier,
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
