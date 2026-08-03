// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import evaka.core.vtjclient.dto.Nationality
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.dto.VtjPerson
import java.time.LocalDate
import tools.jackson.databind.JsonNode

/**
 * POC mapper: DVV REST `/perustiedot` response -> eVaka [VtjPerson].
 *
 * Purpose is to demonstrate that the REST muutosrajapinta reproduces everything the SOAP VTJkysely
 * mapping (`VtjHenkiloMapper`) feeds into `VtjPerson`. It maps only the tietoryhmät eVaka actually
 * consumes; the ~90 other groups are ignored.
 *
 * Two deliberate differences from a production mapper:
 * 1. `municipalityOfResidence` is filled with the DVV **kuntakoodi** (e.g. "091"), not the
 *    plaintext name the SOAP path stored — REST does not carry the name. A production mapper needs
 *    a kuntakoodi->name lookup or a decision to store the code.
 * 2. Guardians/dependants are **stubs** (name + SSN), exactly like the SOAP first hop. Full
 *    hydration is a second `/perustiedot` call on the guardian/dependant SSNs, mirroring how
 *    `VTJPersonDetailsService` already re-queries each relative over SOAP.
 *
 * `today` is passed in rather than read from the clock so address validity is deterministic and
 * testable (the SOAP equivalent used `LocalDate.now(europeHelsinki)`).
 */
fun DvvPerustieto.toVtjPerson(today: LocalDate): VtjPerson {
    val byGroup: Map<String, List<JsonNode>> = tietoryhmat.groupBy {
        it.path("tietoryhma").asString("")
    }
    fun all(name: String): List<JsonNode> = byGroup[name] ?: emptyList()
    fun first(name: String): JsonNode? = all(name).firstOrNull()

    val nimi = first("HENKILON_NIMI")
    val death = first("KUOLINPAIVA")

    return VtjPerson(
        // HENKILON_NIMI uses singular etunimi; the related-person Henkilo uses plural etunimet
        firstNames = nimi?.text("etunimiUTF8") ?: nimi?.text("etunimi") ?: "",
        lastName = nimi?.text("sukunimiUTF8") ?: nimi?.text("sukunimi") ?: "",
        socialSecurityNumber = henkilotunnus,
        address =
            parseAddress(
                mail = all("KOTIMAINEN_POSTIOSOITE"),
                temporary = all("TILAPAINEN_KOTIMAINEN_OSOITE"),
                regular = first("VAKINAINEN_KOTIMAINEN_OSOITE"),
                today = today,
            ),
        residenceCode = first("VAKINAINEN_KOTIMAINEN_ASUINPAIKKATUNNUS")?.text("asuinpaikantunnus"),
        // DVV documents that former nationalities reach perustiedot for some customers, marked
        // voimassaolo=PASSIIVI; the field is optional, so only an explicit PASSIIVI is dropped.
        nationalities =
            first("KANSALAISUUS")
                ?.path("henkilonKansalaisuudet")
                ?.filter { it.text("voimassaolo") != "PASSIIVI" }
                ?.mapNotNull {
                    it.text("kansalaisuuskoodi")?.let { code -> Nationality(countryCode = code) }
                } ?: emptyList(),
        // REST carries `nimi` only when the code itself is unknown (98/mis); normally code-only
        nativeLanguage =
            first("AIDINKIELI")?.let {
                NativeLanguage(
                    languageName = it.text("nimi") ?: "",
                    code = it.text("kielikoodi") ?: "",
                )
            },
        restrictedDetails = first("TURVAKIELTO").toRestrictedDetails(),
        // `kuollut` is required by DVV's schema, so the default is unreachable in a conforming
        // response; false matches how DvvModificationsService.handleDeath reads the same group.
        dateOfDeath = death?.takeIf { it.path("kuollut").asBoolean(false) }?.dateAt("kuolinpv"),
        municipalityOfResidence = first("KOTIKUNTA")?.text("kuntakoodi"),
        municipalityOfResidenceSe = null,
        dependants =
            all("HUOLLETTAVA")
                .filter { it.guardianshipValidNow(today) }
                .mapNotNull { it.path("huollettava").toRelatedStub() },
        guardians =
            all("HUOLTAJA")
                .filter { it.guardianshipValidNow(today) }
                .mapNotNull { it.path("huoltaja").toRelatedStub() },
    )
}

private fun parseAddress(
    mail: List<JsonNode>,
    temporary: List<JsonNode>,
    regular: JsonNode?,
    today: LocalDate,
): PersonAddress? =
    // Same priority as VtjHenkiloMapper.parseAddress: mail > temporary > regular
    listOfNotNull(
            mail.firstNotNullOfOrNull { parseMailAddress(it, today) },
            temporary.firstNotNullOfOrNull { parseStreetAddress(it, today, checkValidity = true) },
            regular?.let { parseStreetAddress(it, today, checkValidity = false) },
        )
        .firstOrNull()

/** VAKINAINEN_KOTIMAINEN_OSOITE / TILAPAINEN_KOTIMAINEN_OSOITE — structured street components. */
private fun parseStreetAddress(
    node: JsonNode,
    today: LocalDate,
    checkValidity: Boolean,
): PersonAddress? {
    if (checkValidity && !node.validNow(today)) return null
    // SOAP delivers this pre-joined including the apartment ("Kauppa Puistikko 6 B 23"); REST
    // splits it into components, so every part has to be re-joined.
    val tail =
        listOfNotNull(
            node.text("katunumero"),
            node.text("huoneistokirjain"),
            node.text("huoneistonumero"),
            node.text("jakokirjain"),
        )
    val streetFi = joinStreet(node.text("katunimi", "fi"), tail)
    val streetSv = joinStreet(node.text("katunimi", "sv"), tail)
    val postalCode = node.text("postinumero")
    val officeFi = node.text("postitoimipaikka", "fi")
    val officeSv = node.text("postitoimipaikka", "sv")
    if (listOf(streetFi, postalCode, officeFi, streetSv, officeSv).all { it.isNullOrBlank() })
        return null
    return PersonAddress(streetFi, postalCode, officeFi, streetSv, officeSv)
}

/**
 * KOTIMAINEN_POSTIOSOITE — mailing address; carries a single `postiosoite` line rather than the
 * katunimi/katunumero split. Per DVV's OpenAPI schema `postiosoite` and `postitoimipaikka` are
 * always {fi,sv} objects, never plain strings.
 */
private fun parseMailAddress(node: JsonNode, today: LocalDate): PersonAddress? {
    if (!node.validNow(today)) return null
    val streetFi = node.text("postiosoite", "fi")
    val streetSv = node.text("postiosoite", "sv")
    val postalCode = node.text("postinumero")
    val officeFi = node.text("postitoimipaikka", "fi")
    val officeSv = node.text("postitoimipaikka", "sv")
    if (listOf(streetFi, postalCode, officeFi, streetSv, officeSv).all { it.isNullOrBlank() })
        return null
    return PersonAddress(streetFi, postalCode, officeFi, streetSv, officeSv)
}

private fun JsonNode?.toRelatedStub(): VtjPerson? {
    if (this == null || isMissingNode) return null
    val ssn = text("henkilotunnus") ?: return null
    return VtjPerson(
        firstNames = text("etunimetUTF8") ?: text("etunimet") ?: "",
        lastName = text("sukunimiUTF8") ?: text("sukunimi") ?: "",
        socialSecurityNumber = ssn,
        restrictedDetails = null,
    )
}

private fun JsonNode?.toRestrictedDetails(): RestrictedDetails {
    if (this == null) return RestrictedDetails(enabled = false, endDate = null)
    val enabled = path("turvakieltoAktiivinen").asBoolean(false)
    return RestrictedDetails(
        enabled = enabled,
        endDate = if (enabled) dateAt("turvaLoppuPv") else null,
    )
}

/**
 * HUOLTAJA/HUOLLETTAVA state their period in `huoltosuhteenAlkupv`/`huoltosuhteenLoppupv`, both
 * optional. Whether DVV also returns already-ended relations in perustiedot depends on the product
 * configuration (it documents historical entries as configuration-dependent for the analogous
 * EDUNVALVONTA and KANSALAISUUS groups), so the period is checked here rather than assumed.
 *
 * Unlike [validNow], a missing bound is not treated as invalid: a relation is dropped only on
 * positive evidence that today lies outside a stated bound, since dropping a real guardian is worse
 * than the alternative for a group that drives access rights.
 */
private fun JsonNode.guardianshipValidNow(today: LocalDate): Boolean {
    val from = dateAt("huoltosuhteenAlkupv")
    val to = dateAt("huoltosuhteenLoppupv")
    return (from == null || !today.isBefore(from)) && (to == null || !today.isAfter(to))
}

/**
 * Mirrors VtjHenkiloMapper.rangeIncludesNow: requires alkupv present, today within [alku, loppu].
 */
private fun JsonNode.validNow(today: LocalDate): Boolean {
    val from = dateAt("alkupv") ?: return false
    val to = dateAt("loppupv")
    return !today.isBefore(from) && (to == null || !today.isAfter(to))
}

private fun joinStreet(name: String?, parts: List<String>): String? =
    (listOfNotNull(name) + parts)
        .mapNotNull { it.ifBlank { null } }
        .joinToString(" ")
        .ifBlank { null }

private fun JsonNode.text(field: String): String? =
    path(field).let { if (it.isMissingNode || it.isNull) null else it.asString() }

private fun JsonNode.text(field: String, lang: String): String? =
    path(field).path(lang).let { if (it.isMissingNode || it.isNull) null else it.asString() }

/** DVV Paivamaara is `{ "arvo": "YYYY-MM-DD", "tarkkuus": "PAIVA" }`. */
private fun JsonNode.dateAt(field: String): LocalDate? =
    path(field).path("arvo").let {
        if (it.isMissingNode || it.isNull) null
        else runCatching { LocalDate.parse(it.asString()) }.getOrNull()
    }

@JsonIgnoreProperties(ignoreUnknown = true)
data class DvvPerustiedotResponse(val perustiedot: List<DvvPerustieto> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class DvvPerustieto(val henkilotunnus: String, val tietoryhmat: List<JsonNode> = emptyList())
