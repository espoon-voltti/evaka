// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.person

import evaka.core.identity.Ssn
import evaka.core.shared.PersonId
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

data class Person(
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

fun Person.hideNonPermittedPersonData(includeInvoiceAddress: Boolean, includeOphOid: Boolean) =
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
        fun from(p: Person): PersonJSON =
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
        fun from(p: Person): PersonBasicInfo =
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
        fun from(p: Person, includeInvoiceAddress: Boolean, includeOphOid: Boolean) =
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
