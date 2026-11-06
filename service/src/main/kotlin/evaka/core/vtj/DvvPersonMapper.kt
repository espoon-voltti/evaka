// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.dvv.Aidinkieli
import evaka.core.dvv.DvvBasicInfo
import evaka.core.dvv.DvvDate
import evaka.core.dvv.HenkilonNimi
import evaka.core.dvv.HenkilotunnusKorjaus
import evaka.core.dvv.HuollettavaSuppea
import evaka.core.dvv.HuoltajaSuppea
import evaka.core.dvv.Huoltosuhde
import evaka.core.dvv.Kotikunta
import evaka.core.dvv.KotimainenOsoite
import evaka.core.dvv.KotimainenPostiosoite
import evaka.core.dvv.Kuolinpaiva
import evaka.core.dvv.Tietoryhma
import evaka.core.dvv.Turvakielto
import evaka.core.dvv.VakinainenKotimainenAsuinpaikkatunnus
import evaka.core.identity.Ssn
import evaka.core.shared.domain.DateRange
import java.time.LocalDate
import org.springframework.core.io.ClassPathResource

const val VAKINAINEN_KOTIMAINEN_OSOITE = "VAKINAINEN_KOTIMAINEN_OSOITE"
const val TILAPAINEN_KOTIMAINEN_OSOITE = "TILAPAINEN_KOTIMAINEN_OSOITE"
const val KOTIMAINEN_POSTIOSOITE = "KOTIMAINEN_POSTIOSOITE"

/** The info groups of a person in the DVV product VAKA, without the guardianships */
val dvvPersonInfoGroups =
    listOf(
        "HENKILOTUNNUS_KORJAUS",
        "HENKILON_NIMI",
        "AIDINKIELI",
        "KUOLINPAIVA",
        "TURVAKIELTO",
        VAKINAINEN_KOTIMAINEN_OSOITE,
        TILAPAINEN_KOTIMAINEN_OSOITE,
        KOTIMAINEN_POSTIOSOITE,
        "KOTIKUNTA",
        "VAKINAINEN_KOTIMAINEN_ASUINPAIKKATUNNUS",
    )

const val HUOLTAJA_SUPPEA = "HUOLTAJA_SUPPEA"
const val HUOLLETTAVA_SUPPEA = "HUOLLETTAVA_SUPPEA"

/** eVaka treats only these guardian types as guardians */
val dvvGuardianTypes = setOf("LAKISAATEINEN_HUOLTAJA", "MAARATTY_HUOLTAJA")

/**
 * The active SSN of a basic info row that DVV returns for a corrected SSN. Such a row has only the
 * SSN correction, so the data must be fetched again with the active SSN.
 */
fun DvvBasicInfo.correctedToSsn(): Ssn? =
    tietoryhmat
        .filterIsInstance<HenkilotunnusKorjaus>()
        .firstNotNullOfOrNull { it.aktiivinenHenkilotunnus }
        ?.takeIf { it != henkilotunnus }
        ?.let(Ssn::parse)

fun mapDvvPerson(ssn: Ssn, groups: List<Tietoryhma>, today: LocalDate): VtjPerson {
    val name = groups.filterIsInstance<HenkilonNimi>().firstOrNull()
    val restriction = groups.filterIsInstance<Turvakielto>().firstOrNull()
    val restricted = restriction?.turvakieltoAktiivinen == true
    val domesticAddresses =
        groups.filterIsInstance<KotimainenOsoite>().filter { it.turvakiellonAlaisetKentat == null }
    // VTJ does not censor the home municipality of a person with restricted details, so eVaka
    // leaves it out itself, like DvvModificationsService.handleRestrictedInfo
    val municipality =
        groups
            .filterIsInstance<Kotikunta>()
            .firstNotNullOfOrNull { it.kuntakoodi }
            ?.let { municipalities[it] }
            .takeUnless { restricted }
    return VtjPerson(
        firstNames = name?.etunimi ?: "",
        lastName = name?.sukunimi ?: "",
        ssn = ssn,
        residentialAddress =
            domesticAddresses
                .firstOrNull {
                    it.tietoryhma == VAKINAINEN_KOTIMAINEN_OSOITE &&
                        isValidOn(it.alkupv, it.loppupv, today)
                }
                ?.toPersonAddress(),
        mailingAddressPeriods =
            groups
                .filterIsInstance<KotimainenPostiosoite>()
                .mapNotNull(KotimainenPostiosoite::toMailingAddressPeriod) +
                domesticAddresses
                    .filter { it.tietoryhma == TILAPAINEN_KOTIMAINEN_OSOITE }
                    .mapNotNull(KotimainenOsoite::toTemporaryAddressPeriod),
        nativeLanguage =
            groups
                .filterIsInstance<Aidinkieli>()
                .firstOrNull { isValidOn(it.alkupv, it.loppupv, today) }
                ?.kielikoodi
                ?.let { NativeLanguage(code = it) },
        restrictedDetails =
            RestrictedDetails(
                enabled = restricted,
                endDate = restriction?.turvaLoppuPv?.asLocalDate()?.takeIf { restricted },
            ),
        dateOfDeath =
            groups
                .filterIsInstance<Kuolinpaiva>()
                .firstOrNull { it.kuollut }
                ?.kuolinpv
                ?.asLocalDate(),
        residenceCode =
            groups
                .filterIsInstance<VakinainenKotimainenAsuinpaikkatunnus>()
                .firstOrNull {
                    it.turvakiellonAlaisetKentat == null && isValidOn(it.alkupv, it.loppupv, today)
                }
                ?.asuinpaikantunnus,
        municipalityOfResidence = municipality?.fi,
        municipalityOfResidenceSe = municipality?.sv,
    )
}

fun mapDvvGuardians(groups: List<Tietoryhma>, today: LocalDate): List<Ssn> =
    groups.filterIsInstance<HuoltajaSuppea>().guardianshipSsns(today)

fun mapDvvDependants(groups: List<Tietoryhma>, today: LocalDate): List<Ssn> =
    groups.filterIsInstance<HuollettavaSuppea>().guardianshipSsns(today)

private fun List<Huoltosuhde>.guardianshipSsns(today: LocalDate): List<Ssn> = filter {
    it.huoltajanLaji in dvvGuardianTypes &&
        isValidOn(it.huoltosuhteenAlkupv, it.huoltosuhteenLoppupv, today)
}
    .mapNotNull { it.relative.henkilotunnus?.let(Ssn::parse) }
    .distinct()

fun isValidOn(start: DvvDate?, end: DvvDate?, date: LocalDate): Boolean =
    (start?.asLocalDate()?.let { it <= date } ?: true) &&
        (end?.asLocalDate()?.let { it >= date } ?: true)

/**
 * For example `Lauttasaarentie 32 B 18`: the apartment number has no leading zeros, `000` means no
 * apartment, and the division letter follows the apartment number.
 */
private fun KotimainenOsoite.streetAddress(streetName: String?): String? {
    if (streetName.isNullOrBlank()) return null
    val apartment = (huoneistonumero?.trimStart('0') ?: "") + (jakokirjain ?: "")
    return listOf(streetName, katunumero, huoneistokirjain, apartment)
        .filterNot { it.isNullOrBlank() }
        .joinToString(" ")
}

private fun KotimainenOsoite.toPersonAddress(): PersonAddress? =
    PersonAddress(
            streetAddress = streetAddress(katunimi?.fi ?: katunimi?.sv),
            postalCode = postinumero,
            postOffice = postitoimipaikka?.fi ?: postitoimipaikka?.sv,
            streetAddressSe = streetAddress(katunimi?.sv),
            postOfficeSe = postitoimipaikka?.sv,
        )
        .takeUnless { it.isBlank() }

private fun KotimainenOsoite.toTemporaryAddressPeriod(): MailingAddressPeriod? {
    val validDuring = validity(alkupv, loppupv) ?: return null
    val address = toPersonAddress() ?: return null
    return MailingAddressPeriod(AddressPeriodType.TEMPORARY, validDuring, address)
}

private fun KotimainenPostiosoite.toMailingAddressPeriod(): MailingAddressPeriod? {
    if (tietoryhma != KOTIMAINEN_POSTIOSOITE || turvakiellonAlaisetKentat != null) return null
    val validDuring = validity(alkupv, loppupv) ?: return null
    val address =
        PersonAddress(
                streetAddress = postiosoite?.fi ?: postiosoite?.sv,
                postalCode = postinumero,
                postOffice = postitoimipaikka?.fi ?: postitoimipaikka?.sv,
                streetAddressSe = postiosoite?.sv,
                postOfficeSe = postitoimipaikka?.sv,
            )
            .takeUnless { it.isBlank() } ?: return null
    return MailingAddressPeriod(AddressPeriodType.MAILING, validDuring, address)
}

private fun validity(start: DvvDate?, end: DvvDate?): DateRange? {
    val startDate = start?.asLocalDate() ?: return null
    val endDate = end?.asLocalDate()
    return if (endDate == null || startDate <= endDate) DateRange(startDate, endDate) else null
}

private fun PersonAddress.isBlank() =
    listOf(streetAddress, postalCode, postOffice, streetAddressSe, postOfficeSe).all {
        it.isNullOrBlank()
    }

private data class Municipality(val fi: String, val sv: String)

/**
 * The municipality classification 2026 of Statistics Finland. When municipalities merge, update
 * `vtj/municipalities.csv` with `bin/update-municipalities.sh`.
 */
private val municipalities: Map<String, Municipality> by lazy {
    ClassPathResource("vtj/municipalities.csv").inputStream.bufferedReader().useLines { lines ->
        lines
            .drop(1)
            .map { it.split(';') }
            .associate { (code, fi, sv) -> code to Municipality(fi, sv) }
    }
}
