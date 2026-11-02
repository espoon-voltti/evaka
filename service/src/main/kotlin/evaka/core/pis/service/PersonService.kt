// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.decision.DecisionSendAddress
import evaka.core.identity.Ssn
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.pdfgen.PdfGenerator
import evaka.core.pis.getPersonById
import evaka.core.pis.splitFirstNames
import evaka.core.pis.updateNonSsnPersonDetails
import evaka.core.pis.updatePersonNonVtjDetails
import evaka.core.pis.updatePersonSsnAddingDisabled
import evaka.core.s3.Document
import evaka.core.shared.HtmlBuilder
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.Rectangle
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class PersonService {
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

    fun disableSsn(tx: Database.Transaction, personId: PersonId, disabled: Boolean) {
        val person = tx.getPersonById(personId) ?: throw NotFound("Person $personId not found")

        if (person.ssn != null) {
            throw BadRequest("Cannot disable a SSN, person $personId already has a SSN")
        }

        tx.updatePersonSsnAddingDisabled(personId, disabled)
    }
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
    val mailingStreetAddress: String,
    val mailingPostalCode: String,
    val mailingPostOffice: String,
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
    val mailingStreetAddress: String,
    val mailingPostalCode: String,
    val mailingPostOffice: String,
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
                mailingStreetAddress = p.mailingStreetAddress,
                mailingPostalCode = p.mailingPostalCode,
                mailingPostOffice = p.mailingPostOffice,
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
            mailingStreetAddress = it.mailingStreetAddress,
            mailingPostalCode = it.mailingPostalCode,
            mailingPostOffice = it.mailingPostOffice,
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
