// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.domain.DateRange
import evaka.core.vtj.soap.VTJHenkiloVastaussanoma
import evaka.core.vtj.soap.VTJHenkiloVastaussanoma.Henkilo
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger {}

// As per
// https://stackoverflow.com/questions/1650249/how-to-make-generated-classes-contain-javadoc-from-xml-schema-documentation
// it might be possible to add documentation from schema into generated code

private fun allBlank(vararg s: String?): Boolean = s.all { it.isNullOrBlank() }

private fun parseValidity(from: String?, to: String?): DateRange? {
    val start = parseLocalDateFromString(from) ?: return null
    val end = parseLocalDateFromString(to)
    return if (end == null || start <= end) DateRange(start, end) else null
}

fun parseMailAddress(
    mailAddress: VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite
): MailingAddressPeriod? =
    with(mailAddress) {
        val validDuring = parseValidity(postiosoiteAlkupvm, postiosoiteLoppupvm) ?: return null
        if (
            allBlank(
                postiosoiteS,
                postinumero,
                postitoimipaikkaS,
                postiosoiteR,
                postitoimipaikkaR,
            )
        )
            return null

        MailingAddressPeriod(
            AddressPeriodType.MAILING,
            validDuring,
            PersonAddress(
                postiosoiteS,
                postinumero,
                postitoimipaikkaS,
                postiosoiteR,
                postitoimipaikkaR,
            ),
        )
    }

fun parseTemporaryAddress(
    temporaryAddress: VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite
): MailingAddressPeriod? =
    with(temporaryAddress) {
        val validDuring = parseValidity(asuminenAlkupvm, asuminenLoppupvm) ?: return null
        if (
            allBlank(
                lahiosoiteS,
                postinumero,
                postitoimipaikkaS,
                lahiosoiteR,
                postitoimipaikkaR,
            )
        )
            return null

        MailingAddressPeriod(
            AddressPeriodType.TEMPORARY,
            validDuring,
            PersonAddress(
                lahiosoiteS,
                postinumero,
                postitoimipaikkaS,
                lahiosoiteR,
                postitoimipaikkaR,
            ),
        )
    }

fun parseRegularAddress(
    regularAddress: VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite
): PersonAddress? =
    with(regularAddress) {
        if (
            allBlank(
                lahiosoiteS,
                postinumero,
                postitoimipaikkaS,
                lahiosoiteR,
                postitoimipaikkaR,
            )
        )
            return null

        PersonAddress(lahiosoiteS, postinumero, postitoimipaikkaS, lahiosoiteR, postitoimipaikkaR)
    }

fun Henkilo.mapToVtjPerson(): VtjPerson =
    VtjPerson(
        firstNames = nykyisetEtunimet.etunimet,
        lastName = nykyinenSukunimi.sukunimi,
        ssn = Ssn.parse(henkilotunnus.value) ?: error("VTJ returned an invalid SSN"),
        residentialAddress = parseRegularAddress(vakinainenKotimainenLahiosoite),
        mailingAddressPeriods =
            kotimainenPostiosoite.mapNotNull { parseMailAddress(it) } +
                tilapainenKotimainenLahiosoite.mapNotNull { parseTemporaryAddress(it) },
        residenceCode = vakinainenAsuinpaikka?.asuinpaikantunnus,
        nativeLanguage = mapNativeLanguage(),
        restrictedDetails = turvakielto.mapToRestrictedDetails(),
        dateOfDeath = parseLocalDateFromString(kuolintiedot.kuolinpvm),
        municipalityOfResidence = kotikunta?.kuntaS,
        municipalityOfResidenceSe = kotikunta?.kuntaR,
    )

fun Henkilo.mapDependants(): List<Ssn> = huollettava.mapNotNull { relativeSsn(it.henkilotunnus) }

fun Henkilo.mapGuardians(): List<Ssn> = huoltaja.mapNotNull { relativeSsn(it.henkilotunnus) }

private fun relativeSsn(henkilotunnus: String?): Ssn? =
    if (henkilotunnus.isNullOrEmpty()) null
    else Ssn.parse(henkilotunnus) ?: error("VTJ returned an invalid SSN")

/*
   kieliS:     "Henkilon kielen nimi. Muoto 0-30 merkkia."

   kielikoodi: "Aidinkielen osalta ISO 639-1:n mukainen kielikoodi, mahdolliset arvot: tyhja, pieni kirjain a-z
               2 kertaa, 98=tieto selvakielisena, 99=tuntematon.
               Asiointikielen osalta mahdolliset arvot: fi (suomi) ja sv (ruotsi)."
*/
fun Henkilo.mapNativeLanguage(): NativeLanguage = aidinkieli.let {
    NativeLanguage(languageName = it.kieliS ?: "", code = it.kielikoodi ?: "")
}

/*  Possible values for turvakieltoTieto:
    empty -> no restriction
    1 -> restriction enabled
    0 -> no restriction
*/
private const val RESTRICTION_ON: String = "1"

fun Henkilo.Turvakielto.mapToRestrictedDetails(): RestrictedDetails {
    val enabled = turvakieltoTieto?.let { RESTRICTION_ON.contentEquals(it) } ?: false
    val endDate = if (enabled) parseLocalDateFromString(turvakieltoPaattymispvm) else null

    return RestrictedDetails(enabled = enabled, endDate = endDate)
}

fun parseLocalDateFromString(date: String?): LocalDate? {
    if (date.isNullOrBlank()) return null

    return try {
        LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
    } catch (e: DateTimeParseException) {
        logger.error(e) { "Error parsing $date as a date" }
        null
    }
}
