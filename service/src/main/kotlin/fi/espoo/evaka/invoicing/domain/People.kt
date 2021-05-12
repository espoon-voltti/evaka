// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.mapper.PropagateNull
import java.time.LocalDate
import java.util.UUID

sealed class PersonData {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class JustId(@PropagateNull val id: UUID) : PersonData()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WithDateOfBirth(@PropagateNull val id: UUID, val dateOfBirth: LocalDate) : PersonData()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WithName(@PropagateNull val id: UUID, val firstName: String, val lastName: String) : PersonData()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Basic(
        @PropagateNull
        val id: UUID,
        val dateOfBirth: LocalDate,
        val firstName: String,
        val lastName: String,
        val ssn: String? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Detailed(
        @PropagateNull
        val id: UUID,
        val dateOfBirth: LocalDate,
        val dateOfDeath: LocalDate? = null,
        val firstName: String,
        val lastName: String,
        val ssn: String? = null,
        val streetAddress: String? = null,
        val postalCode: String? = null,
        val postOffice: String? = null,
        val residenceCode: String? = null,
        val email: String? = null,
        val phone: String? = null,
        val language: String? = null,
        val invoiceRecipientName: String = "",
        val invoicingStreetAddress: String = "",
        val invoicingPostalCode: String = "",
        val invoicingPostOffice: String = "",
        val restrictedDetailsEnabled: Boolean,
        val forceManualFeeDecisions: Boolean = false
    ) : PersonData()
}

fun addressUsable(streetAddress: String?, postalCode: String?, city: String?): Boolean =
    listOf(streetAddress, postalCode, city).none { it.isNullOrBlank() }

data class MailAddress(
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val poBox: String? = null
) {
    companion object {
        val defaultAddress = MailAddress(
            "",
            "02700",
            "Espoon kaupunki",
            "PL 30"
        )

        val defaultAddressSv = MailAddress(
            "",
            "02700",
            "Esbo",
            "PB 30"
        )

        fun fromPerson(person: PersonData.Detailed, lang: String? = "fi") = person.let {
            if (addressUsable(person.streetAddress, person.postalCode, person.postOffice)) {
                MailAddress(person.streetAddress!!, person.postalCode!!, person.postOffice!!)
            } else {
                when (lang) {
                    "sv" -> defaultAddressSv
                    else -> defaultAddress
                }
            }
        }
    }
}

data class FridgeFamily(
    val headOfFamily: PersonData.JustId,
    val partner: PersonData.JustId?,
    val children: List<PersonData.WithDateOfBirth>,
    val period: DateRange
)
