// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A difference between two versions of the VTJ data of one person. [details] has no personal data,
 * so that the difference can be logged.
 */
data class VtjFieldDifference(val field: String, val details: Map<String, Any?> = emptyMap())

fun compareVtjPersons(soap: VtjPerson, dvv: VtjPerson): List<VtjFieldDifference> {
    val differences =
        VtjDifferences(
            restricted =
                soap.restrictedDetails?.enabled == true || dvv.restrictedDetails?.enabled == true
        )
    with(differences) {
        text("firstNames", soap.firstNames, dvv.firstNames)
        text("lastName", soap.lastName, dvv.lastName)
        text("ssn", soap.ssn.value, dvv.ssn.value)
        address("residentialAddress", soap.residentialAddress, dvv.residentialAddress)
        mailingAddressPeriods(soap.mailingAddressPeriods, dvv.mailingAddressPeriods)
        code("nativeLanguage", soap.nativeLanguage?.code, dvv.nativeLanguage?.code)
        code(
            "restrictedDetails.enabled",
            soap.restrictedDetails?.enabled ?: false,
            dvv.restrictedDetails?.enabled ?: false,
        )
        date(
            "restrictedDetails.endDate",
            soap.restrictedDetails?.endDate,
            dvv.restrictedDetails?.endDate,
        )
        date("dateOfDeath", soap.dateOfDeath, dvv.dateOfDeath)
        text("residenceCode", soap.residenceCode, dvv.residenceCode)
        code("municipalityOfResidence", soap.municipalityOfResidence, dvv.municipalityOfResidence)
        code(
            "municipalityOfResidenceSe",
            soap.municipalityOfResidenceSe,
            dvv.municipalityOfResidenceSe,
        )
    }
    return differences.list
}

fun compareVtjPersons(
    soap: VtjPersonAndGuardians,
    dvv: VtjPersonAndGuardians,
): List<VtjFieldDifference> =
    compareVtjPersons(soap.person, dvv.person) +
        listOfNotNull(ssnSetDifference("guardians", soap.guardians, dvv.guardians))

fun compareVtjPersons(
    soap: VtjPersonAndDependants,
    dvv: VtjPersonAndDependants,
): List<VtjFieldDifference> =
    compareVtjPersons(soap.person, dvv.person) +
        listOfNotNull(ssnSetDifference("dependants", soap.dependants, dvv.dependants))

fun ssnSetDifference(field: String, soap: Collection<Ssn>, dvv: Collection<Ssn>) =
    if (soap.toSet() == dvv.toSet()) null
    else
        VtjFieldDifference(
            field,
            mapOf(
                "kind" to "SET",
                "soapCount" to soap.toSet().size,
                "dvvCount" to dvv.toSet().size,
                "commonCount" to soap.toSet().intersect(dvv.toSet()).size,
            ),
        )

/**
 * Collects the differences of one person. When either version has restricted details, a difference
 * has only the field name.
 */
class VtjDifferences(private val restricted: Boolean) {
    val list = mutableListOf<VtjFieldDifference>()

    fun text(field: String, soap: String?, dvv: String?) {
        if (soap == dvv) return
        add(
            field,
            mapOf(
                "kind" to "TEXT",
                "soap" to soap?.let(::maskText),
                "dvv" to dvv?.let(::maskText),
                "onlyCase" to (soap != null && dvv != null && soap.equals(dvv, ignoreCase = true)),
                "onlyWhitespace" to
                    (soap != null &&
                        dvv != null &&
                        soap.withoutWhitespace() == dvv.withoutWhitespace()),
            ),
        )
    }

    fun code(field: String, soap: Any?, dvv: Any?) {
        if (soap == dvv) return
        add(field, mapOf("kind" to "CODE", "soap" to soap, "dvv" to dvv))
    }

    fun date(field: String, soap: LocalDate?, dvv: LocalDate?) {
        if (soap == dvv) return
        add(
            field,
            mapOf(
                "kind" to "DATE",
                "missing" to
                    when {
                        soap == null -> "SOAP"
                        dvv == null -> "DVV"
                        else -> null
                    },
                "days" to
                    if (soap != null && dvv != null) ChronoUnit.DAYS.between(soap, dvv) else null,
            ),
        )
    }

    fun address(field: String, soap: PersonAddress?, dvv: PersonAddress?) {
        val empty = PersonAddress(null, null, null)
        val soapAddress = (soap ?: empty).withoutBlanks()
        val dvvAddress = (dvv ?: empty).withoutBlanks()
        text("$field.streetAddress", soapAddress.streetAddress, dvvAddress.streetAddress)
        text("$field.postalCode", soapAddress.postalCode, dvvAddress.postalCode)
        text("$field.postOffice", soapAddress.postOffice, dvvAddress.postOffice)
        text("$field.streetAddressSe", soapAddress.streetAddressSe, dvvAddress.streetAddressSe)
        text("$field.postOfficeSe", soapAddress.postOfficeSe, dvvAddress.postOfficeSe)
    }

    fun mailingAddressPeriods(soap: List<MailingAddressPeriod>, dvv: List<MailingAddressPeriod>) {
        if (soap.toSet() == dvv.toSet()) return
        val soapByType = soap.groupBy { it.type }
        val dvvByType = dvv.groupBy { it.type }
        AddressPeriodType.entries.forEach { type ->
            val soapPeriods = soapByType[type].orEmpty().sortedBy { it.validDuring.start }
            val dvvPeriods = dvvByType[type].orEmpty().sortedBy { it.validDuring.start }
            val field = "mailingAddressPeriods.$type"
            // The periods can be paired by index only when the counts are the same
            if (soapPeriods.size != dvvPeriods.size) {
                add(
                    field,
                    mapOf(
                        "kind" to "SET",
                        "soapCount" to soapPeriods.size,
                        "dvvCount" to dvvPeriods.size,
                    ),
                )
            } else {
                soapPeriods.zip(dvvPeriods).forEachIndexed { index, (s, d) ->
                    date("$field[$index].start", s.validDuring.start, d.validDuring.start)
                    date("$field[$index].end", s.validDuring.end, d.validDuring.end)
                    address("$field[$index]", s.address, d.address)
                }
            }
        }
    }

    private fun add(field: String, details: Map<String, Any?>) {
        list += if (restricted) VtjFieldDifference(field) else VtjFieldDifference(field, details)
    }
}

/**
 * Replaces an upper case letter with `A`, a lower case letter with `a`, a letter outside Latin-1
 * with `*` and a digit with `9`, and keeps the other characters.
 */
fun maskText(text: String): String =
    text
        .map {
            when {
                it.isLetter() && it.code > 0xff -> '*'
                it.isUpperCase() -> 'A'
                it.isLowerCase() -> 'a'
                it.isLetter() -> '*'
                it.isDigit() -> '9'
                else -> it
            }
        }
        .joinToString("")

private fun String.withoutWhitespace() = filterNot { it.isWhitespace() }

private fun PersonAddress.withoutBlanks() =
    PersonAddress(
        streetAddress = streetAddress?.takeIf { it.isNotBlank() },
        postalCode = postalCode?.takeIf { it.isNotBlank() },
        postOffice = postOffice?.takeIf { it.isNotBlank() },
        streetAddressSe = streetAddressSe?.takeIf { it.isNotBlank() },
        postOfficeSe = postOfficeSe?.takeIf { it.isNotBlank() },
    )
