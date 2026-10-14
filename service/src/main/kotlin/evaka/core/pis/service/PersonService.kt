// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.AuditContext
import evaka.core.decision.DecisionSendAddress
import evaka.core.identity.Ssn
import evaka.core.identity.getDobFromSsn
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.pdfgen.PdfGenerator
import evaka.core.pis.applyDependants
import evaka.core.pis.applyGuardians
import evaka.core.pis.getDependantGuardians
import evaka.core.pis.getGuardianDependants
import evaka.core.pis.getPersonById
import evaka.core.pis.getPersonBySSN
import evaka.core.pis.setSsnFromVtj
import evaka.core.pis.splitFirstNames
import evaka.core.pis.updateNonSsnPersonDetails
import evaka.core.pis.updatePersonNonVtjDetails
import evaka.core.pis.updatePersonSsnAddingDisabled
import evaka.core.s3.Document
import evaka.core.shared.ChildId
import evaka.core.shared.HtmlBuilder
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.Rectangle
import evaka.core.vtjclient.dto.Nationality
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.service.persondetails.VtjClient
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val vtjClient: VtjClient,
    private val vtjSyncService: VtjSyncService,
) {
    // Does a request to VTJ if the person has a SSN and updates person data
    fun getUpToDatePersonFromVtj(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        id: PersonId,
    ): PersonDTO? {
        val person = tx.getPersonById(id) ?: return null
        if (person.ssn == null) return person
        val vtjPerson = vtjClient.fetchPerson(VtjClient.DetailsQuery(user.evakaUserId, person.ssn))
        val _ = vtjSyncService.applyFromVtj(tx, now, vtjPerson)
        return tx.getPersonById(id)
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
            !person1.streetAddress.lowercase().contains("poste restante") &&
            !person2.streetAddress.lowercase().contains("poste restante") &&
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
            !person1.streetAddress.lowercase().contains("poste restante") &&
            !person2.streetAddress.lowercase().contains("poste restante") &&
            person1.postalCode.isNotBlank() &&
            person2.postalCode.isNotBlank() &&
            person1.postOffice.isNotBlank() &&
            person2.postOffice.isNotBlank() &&
            person1.streetAddress.lowercase() == person2.streetAddress.lowercase() &&
            person1.postalCode == person2.postalCode &&
            person1.postOffice == person2.postOffice
    }

    // Does a request to VTJ if SSN is present and the person hasn't had their dependants
    // initialized
    fun getPersonWithChildren(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        id: PersonId,
        forceRefresh: Boolean = false,
    ): PersonWithChildrenDTO? {
        val guardian = tx.getPersonById(id) ?: return null
        if (guardian.ssn == null) return toPersonWithChildrenDTO(guardian)

        if (forceRefresh || guardian.vtjDependantsQueried == null) {
            val snapshot =
                vtjClient.fetchWithDependants(
                    VtjClient.DetailsQuery(user.evakaUserId, guardian.ssn)
                )
            val children =
                snapshot.dependants.map {
                    vtjClient.fetchPerson(VtjClient.DetailsQuery(user.evakaUserId, it.ssn))
                }
            val guardianId = vtjSyncService.applyFromVtj(tx, now, snapshot.person)
            val childIds = children.map { vtjSyncService.applyFromVtj(tx, now, it) }.toSet()
            tx.applyDependants(guardianId, childIds, now)
        }

        val children = tx.getGuardianDependants(id).map { toPersonWithChildrenDTO(it) }
        return tx.getPersonById(id)?.let { toPersonWithChildrenDTO(it).copy(children = children) }
    }

    // Does a request to VTJ if SSN is present
    fun getGuardians(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        id: ChildId,
        forceRefresh: Boolean = false,
    ): List<PersonDTO> {
        val child = tx.getPersonById(id) ?: return emptyList()
        if (child.ssn == null) return emptyList()

        if (forceRefresh || child.vtjGuardiansQueried == null) {
            val snapshot =
                vtjClient.fetchWithGuardians(VtjClient.DetailsQuery(user.evakaUserId, child.ssn))
            val guardians =
                snapshot.guardians.map {
                    vtjClient.fetchPerson(VtjClient.DetailsQuery(user.evakaUserId, it.ssn))
                }
            val childId = vtjSyncService.applyFromVtj(tx, now, snapshot.person)
            val guardianIds = guardians.map { vtjSyncService.applyFromVtj(tx, now, it) }.toSet()
            tx.applyGuardians(childId, guardianIds, now)
        }

        return tx.getDependantGuardians(id)
    }

    // In extremely rare cases there might be more than 2 guardians, but it was agreed with product
    // management to use
    // just one of these as the other guardian.
    fun getOtherGuardian(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        otherGuardianId: PersonId,
        childId: ChildId,
    ): PersonDTO? =
        getGuardians(tx, user, now, childId).firstOrNull { guardian ->
            guardian.id != otherGuardianId
        }

    // Does a request to VTJ if person is not found in database or person data has not been
    // initialized from VTJ
    fun getOrCreatePerson(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        ssn: Ssn,
        readonly: Boolean = false,
        audit: AuditContext?,
    ): PersonDTO? {
        val person = tx.getPersonBySSN(ssn.value)
        audit?.add(listOfNotNull(person?.id))
        return if (person?.updatedFromVtj == null) {
            val vtjPerson = vtjClient.fetchPerson(VtjClient.DetailsQuery(user.evakaUserId, ssn))
            if (readonly) return toPreviewPersonDTO(vtjPerson)

            val _ = vtjSyncService.applyFromVtj(tx, now, vtjPerson)
            tx.getPersonBySSN(ssn.value).also {
                audit?.add(listOfNotNull(it?.id))?.addMeta("created", person == null)
            }
        } else {
            person
        }
    }

    @IgnorableReturnValue
    fun patchUserDetails(tx: Database.Transaction, id: PersonId, data: PersonPatch): PersonDTO {
        val person = tx.getPersonById(id) ?: throw NotFound("Person $id not found")

        // People with SSN get basic details from VTJ which should not be modified
        if (person.ssn != null) {
            tx.updatePersonNonVtjDetails(id, data)
        } else {
            tx.updateNonSsnPersonDetails(id, data)
        }

        return tx.getPersonById(id)!!
    }

    fun addSsn(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        id: PersonId,
        ssn: Ssn,
    ): PersonDTO {
        val person = tx.getPersonById(id) ?: throw NotFound("Person $id not found")

        if (person.ssn != null) {
            throw BadRequest("User already has ssn")
        }
        if (tx.getPersonBySSN(ssn.value) != null) {
            throw Conflict("User with same ssn already exists")
        }

        val vtjPerson = vtjClient.fetchPerson(VtjClient.DetailsQuery(user.evakaUserId, ssn))
        if (vtjPerson.socialSecurityNumber != ssn.value) {
            throw BadRequest("VTJ returned a different SSN than the one being added")
        }

        tx.updatePersonSsnAddingDisabled(id, false)
        val _ = tx.setSsnFromVtj(id, vtjPerson, now)
        return tx.getPersonById(id)!!
    }

    fun disableSsn(tx: Database.Transaction, personId: PersonId, disabled: Boolean) {
        val person = tx.getPersonById(personId) ?: throw NotFound("Person $personId not found")

        if (person.ssn != null) {
            throw BadRequest("Cannot disable a SSN, person $personId already has a SSN")
        }

        tx.updatePersonSsnAddingDisabled(personId, disabled)
    }

    private fun toPreviewPersonDTO(person: VtjPerson): PersonDTO =
        PersonDTO(
            id = PersonId(UUID(0, 0)),
            duplicateOf = null,
            ssn = Ssn.parse(person.socialSecurityNumber) ?: error("VTJ returned an invalid SSN"),
            ssnAddingDisabled = false,
            firstName = person.firstNames,
            lastName = person.lastName,
            preferredName = "",
            email = null,
            phone = "",
            backupPhone = "",
            language = person.nativeLanguage?.toString(),
            dateOfBirth = getDobFromSsn(person.socialSecurityNumber),
            dateOfDeath = person.dateOfDeath,
            streetAddress = person.address?.streetAddress ?: "",
            postalCode = person.address?.postalCode ?: "",
            postOffice = person.address?.postOffice ?: "",
            residenceCode = person.residenceCode ?: "",
            municipalityOfResidence = person.municipalityOfResidence ?: "",
            restrictedDetailsEnabled = person.restrictedDetails?.enabled ?: false,
            restrictedDetailsEndDate = person.restrictedDetails?.endDate,
        )

    private fun toPersonWithChildrenDTO(person: PersonDTO): PersonWithChildrenDTO =
        PersonWithChildrenDTO(
            id = person.id,
            duplicateOf = person.duplicateOf,
            socialSecurityNumber = person.ssn?.value,
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
                    postOffice = person.postOffice,
                    residenceCode = person.residenceCode,
                ),
            residenceCode = person.residenceCode,
            municipalityOfResidence = person.municipalityOfResidence,
            phone = person.phone,
            backupPhone = person.backupPhone,
            email = person.email,
            children = emptyList(),
        )
}

data class PersonDTO(
    val id: PersonId,
    val duplicateOf: PersonId?,
    val ssn: Ssn?,
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
    val municipalityOfResidence: String,
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
)

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
    val municipalityOfResidence: String = "",
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
                socialSecurityNumber = p.ssn?.value,
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
                municipalityOfResidence = p.municipalityOfResidence,
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

data class PersonBasicInfo(
    val id: PersonId,
    val duplicateOf: PersonId?,
    val hasSsn: Boolean,
    val ssnAddingDisabled: Boolean,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate?,
    val restrictedDetailsEnabled: Boolean,
) {
    companion object {
        fun from(p: PersonDTO): PersonBasicInfo =
            PersonBasicInfo(
                id = p.id,
                duplicateOf = p.duplicateOf,
                hasSsn = p.ssn != null,
                ssnAddingDisabled = p.ssnAddingDisabled,
                firstName = p.firstName,
                lastName = p.lastName,
                dateOfBirth = p.dateOfBirth,
                dateOfDeath = p.dateOfDeath,
                restrictedDetailsEnabled = p.restrictedDetailsEnabled,
            )
    }
}

data class PersonSensitiveDetails(
    val socialSecurityNumber: String?,
    val language: String?,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val residenceCode: String,
    val municipalityOfResidence: String,
    val email: String?,
    val phone: String,
    val backupPhone: String,
    val invoiceRecipientName: String,
    val invoicingStreetAddress: String,
    val invoicingPostalCode: String,
    val invoicingPostOffice: String,
    val forceManualFeeDecisions: Boolean,
    val ophPersonOid: String?,
    val updatedFromVtj: HelsinkiDateTime?,
) {
    companion object {
        fun from(p: PersonDTO, includeInvoiceAddress: Boolean, includeOphOid: Boolean) =
            PersonSensitiveDetails(
                socialSecurityNumber = p.ssn?.value,
                language = p.language,
                streetAddress = p.streetAddress,
                postalCode = p.postalCode,
                postOffice = p.postOffice,
                residenceCode = p.residenceCode,
                municipalityOfResidence = p.municipalityOfResidence,
                email = p.email,
                phone = p.phone,
                backupPhone = p.backupPhone,
                invoiceRecipientName = if (includeInvoiceAddress) p.invoiceRecipientName else "",
                invoicingStreetAddress =
                    if (includeInvoiceAddress) p.invoicingStreetAddress else "",
                invoicingPostalCode = if (includeInvoiceAddress) p.invoicingPostalCode else "",
                invoicingPostOffice = if (includeInvoiceAddress) p.invoicingPostOffice else "",
                forceManualFeeDecisions =
                    if (includeInvoiceAddress) p.forceManualFeeDecisions else false,
                ophPersonOid = if (includeOphOid) p.ophPersonOid else null,
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
    val municipalityOfResidence: String? = null,
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
    val municipalityOfResidence: String,
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
            ssn = socialSecurityNumber?.let { Ssn.parse(it) ?: error("Invalid SSN") },
            ssnAddingDisabled = false,
            dateOfBirth = dateOfBirth,
            dateOfDeath = dateOfDeath,
            firstName = firstName,
            lastName = lastName,
            preferredName = preferredName,
            residenceCode = residenceCode,
            streetAddress = address.streetAddress,
            postalCode = address.postalCode,
            postOffice = address.postOffice,
            municipalityOfResidence = municipalityOfResidence,
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
    val postOffice: String,
    val residenceCode: String,
) {
    enum class Origin {
        VTJ,
        MUNICIPAL,
        EVAKA,
    }
}

data class RestrictedDetails(val enabled: Boolean, val endDate: LocalDate? = null)

fun createAddressPagePdf(
    pdfGenerator: PdfGenerator,
    today: LocalDate,
    envelopeWindowPosition: Rectangle,
    guardian: PersonDTO,
): Document {
    val personDetails = guardian.let {
        val firstWordOfFirstName = splitFirstNames(it.firstName).firstOrNull() ?: ""

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

    val sendAddress = DecisionSendAddress.fromPerson(personDetails)
    val window = envelopeWindowPosition
    val html =
        HtmlBuilder.document(
            css =
                """
                @page {
                    size: A4 portrait;
                    margin-left: ${window.x}mm;
                    margin-top: ${window.y}mm;
                    margin-right: 15mm;
                    margin-bottom: 15mm;
                }
                body { margin: 0; }
                .address {
                    width: ${window.width}mm;
                    height: ${window.height}mm;
                    font-family: 'Open Sans', sans-serif;
                    font-weight: 400;
                    font-size: 10pt;
                    line-height: 1.4;
                }
                """
        ) {
            listOf(
                div(className = "address") {
                    listOfNotNull(
                        div("${personDetails.lastName} ${personDetails.firstName}"),
                        sendAddress?.let { addr ->
                            div {
                                listOfNotNull(
                                    div(addr.row1),
                                    div(addr.row2),
                                    addr.row3.takeIf { it.isNotEmpty() }?.let { div(it) },
                                )
                            }
                        },
                    )
                }
            )
        }

    return Document(
        name = "osoitesivu_${guardian.lastName}_$today.pdf",
        bytes = pdfGenerator.render(html),
        contentType = "application/pdf",
    )
}
