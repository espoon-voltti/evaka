// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.mapper

import fi.espoo.evaka.shared.domain.europeHelsinki
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VtjHenkiloMapper {
    fun mapToVtjPerson(henkilo: Henkilo) = henkilo.mapToVtjPerson()
}

// As per
// https://stackoverflow.com/questions/1650249/how-to-make-generated-classes-contain-javadoc-from-xml-schema-documentation
// it might be possible to add documentation from schema into generated code

fun rangeIncludesNow(
    from: String?,
    to: String?
): Boolean {
    val validFrom = parseLocalDateFromString(from)
    val validTo = parseLocalDateFromString(to)

    return LocalDate.now(europeHelsinki).let {
        validFrom != null && it >= validFrom && (validTo == null || it <= validTo)
    }
}

fun notAllBlanks(vararg s: String?): Boolean = listOf(*s).any { !it.isNullOrBlank() }

fun parseMailAddress(mailAddress: VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite): PersonAddress? =
    with(mailAddress) {
        val isValid =
            rangeIncludesNow(postiosoiteAlkupvm, postiosoiteLoppupvm) &&
                notAllBlanks(
                    postiosoiteS,
                    postinumero,
                    postitoimipaikkaS,
                    postiosoiteR,
                    postitoimipaikkaR
                )

        if (!isValid) return null

        PersonAddress(postiosoiteS, postinumero, postitoimipaikkaS, postiosoiteR, postitoimipaikkaR)
    }

fun parseTemporaryAddress(temporaryAddress: VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite): PersonAddress? =
    with(temporaryAddress) {
        val isValid =
            rangeIncludesNow(asuminenAlkupvm, asuminenLoppupvm) &&
                notAllBlanks(
                    lahiosoiteS,
                    postinumero,
                    postitoimipaikkaS,
                    lahiosoiteR,
                    postitoimipaikkaR
                )

        if (!isValid) return null

        PersonAddress(lahiosoiteS, postinumero, postitoimipaikkaS, lahiosoiteR, postitoimipaikkaR)
    }

fun parseRegularAddress(regularAddress: VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite): PersonAddress? =
    with(regularAddress) {
        val isValid =
            notAllBlanks(
                lahiosoiteS,
                postinumero,
                postitoimipaikkaS,
                lahiosoiteR,
                postitoimipaikkaR
            )

        if (!isValid) return null

        PersonAddress(lahiosoiteS, postinumero, postitoimipaikkaS, lahiosoiteR, postitoimipaikkaR)
    }

fun parseAddress(
    mailAddresses: List<Henkilo.KotimainenPostiosoite>,
    temporaryAddresses: List<Henkilo.TilapainenKotimainenLahiosoite>,
    regularAddress: Henkilo.VakinainenKotimainenLahiosoite
) = listOfNotNull(
    mailAddresses.mapNotNull { parseMailAddress(it) }.firstOrNull(),
    temporaryAddresses.mapNotNull { parseTemporaryAddress(it) }.firstOrNull(),
    parseRegularAddress(regularAddress)
).firstOrNull()

fun Henkilo.mapToVtjPerson(): VtjPerson {
    val address =
        parseAddress(
            kotimainenPostiosoite,
            tilapainenKotimainenLahiosoite,
            vakinainenKotimainenLahiosoite
        )

    return VtjPerson(
        firstNames = nykyisetEtunimet.etunimet,
        lastName = nykyinenSukunimi.sukunimi,
        socialSecurityNumber = henkilotunnus.value,
        nationalities = mapNationalities(),
        dependants = mapDependants(),
        guardians = mapGuardians(),
        address = address,
        residenceCode = vakinainenAsuinpaikka?.asuinpaikantunnus,
        nativeLanguage = mapNativeLanguage(),
        restrictedDetails = turvakielto.mapToRestrictedDetails(),
        dateOfDeath = parseLocalDateFromString(kuolintiedot.kuolinpvm)
    )
}

fun Henkilo.Huollettava.mapToPerson() =
    VtjPerson(
        firstNames = nykyisetEtunimet.etunimet,
        lastName = nykyinenSukunimi.sukunimi,
        socialSecurityNumber = henkilotunnus,
        restrictedDetails = null
    )

fun Henkilo.Huoltaja.mapToPerson() =
    VtjPerson(
        firstNames = nykyisetEtunimet.etunimet,
        lastName = nykyinenSukunimi.sukunimi,
        socialSecurityNumber = henkilotunnus,
        restrictedDetails = null
    )

/*
   kansalaisuuskoodi3: "ISO 3166-1-koodiston mukainen kolminumeroinen valtionimeen liittyva tunnus.
                       Muoto 3 numeroa tai voi olla tyhja."

   kansalaisuusS:       "Valtion nimi selvakielisena (lyhyt muoto) 0-100 merkkia."
*/
fun Henkilo.mapNationalities() =
    kansalaisuus
        .filter { it.kansalaisuuskoodi3.isNotBlank() && it.kansalaisuusS.isNotBlank() }
        .map { Nationality(countryName = it.kansalaisuusS, countryCode = it.kansalaisuuskoodi3) }

fun Henkilo.mapDependants() = huollettava.filter { !it.henkilotunnus.isNullOrEmpty() }.map { it.mapToPerson() }

fun Henkilo.mapGuardians() = huoltaja.filter { !it.henkilotunnus.isNullOrEmpty() }.map { it.mapToPerson() }

/*
   kieliS:     "Henkilon kielen nimi. Muoto 0-30 merkkia."

   kielikoodi: "Aidinkielen osalta ISO 639-1:n mukainen kielikoodi, mahdolliset arvot: tyhja, pieni kirjain a-z
               2 kertaa, 98=tieto selvakielisena, 99=tuntematon.
               Asiointikielen osalta mahdolliset arvot: fi (suomi) ja sv (ruotsi)."
*/
fun Henkilo.mapNativeLanguage(): NativeLanguage =
    aidinkieli.let { NativeLanguage(languageName = it.kieliS ?: "", code = it.kielikoodi ?: "") }

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
        logger.error("Error parsing $date as a date", e)
        null
    }
}
