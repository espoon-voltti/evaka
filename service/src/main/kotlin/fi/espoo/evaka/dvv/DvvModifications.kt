// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.format.DateTimeParseException

@JsonIgnoreProperties(ignoreUnknown = true)
data class DvvModificationsResponse(
    val viimeisinKirjausavain: String,
    val muutokset: List<DvvModification>,
    val ajanTasalla: Boolean
)

data class DvvModification(
    val muutospv: String,
    val henkilotunnus: String,
    val tietoryhmat: List<DvvInfoGroup>,
    val ajanTasalla: Boolean
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "tietoryhma",
    defaultImpl = DefaultDvvInfoGroup::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PersonNameDvvInfoGroup::class, name = "HENKILON_NIMI"),
    JsonSubTypes.Type(value = PersonNameChangeDvvInfoGroup::class, name = "NIMENMUUTOS"),
    JsonSubTypes.Type(value = PersonNameChangeDvvInfoGroup::class, name = "NIMENMUUTOS_LAAJA"),
    JsonSubTypes.Type(value = RestrictedInfoDvvInfoGroup::class, name = "TURVAKIELTO"),
    JsonSubTypes.Type(value = AddressDvvInfoGroup::class, name = "VAKINAINEN_KOTIMAINEN_OSOITE"),
    JsonSubTypes.Type(value = DeathDvvInfoGroup::class, name = "KUOLINPAIVA"),
    JsonSubTypes.Type(value = CustodianLimitedDvvInfoGroup::class, name = "HUOLLETTAVA_SUPPEA"),
    JsonSubTypes.Type(value = CaretakerLimitedDvvInfoGroup::class, name = "HUOLTAJA_SUPPEA"),
    JsonSubTypes.Type(value = SsnDvvInfoGroup::class, name = "HENKILOTUNNUS_KORJAUS"),
    JsonSubTypes.Type(value = ResidenceCodeDvvInfoGroup::class, name = "VAKINAINEN_KOTIMAINEN_ASUINPAIKKATUNNUS"),
    JsonSubTypes.Type(value = HomeMunicipalityDvvInfoGroup::class, name = "KOTIKUNTA")
)

interface DvvInfoGroup {
    val tietoryhma: String
}

data class DefaultDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?
) : DvvInfoGroup

data class PersonNameDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val etunimi: String?,
    val sukunimi: String?,
    val alkupv: DvvDate?,
    val lisatieto: String?
) : DvvInfoGroup

data class PersonNameChangeDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val nimilaji: String?,
    val nimi: String?,
    val alkupv: DvvDate?,
    val loppupv: DvvDate?
) : DvvInfoGroup

data class RestrictedInfoDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val turvakieltoAktiivinen: Boolean,
    val turvaLoppuPv: DvvDate?
) : DvvInfoGroup

data class AddressDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val katunimi: DvvFiSVValue?,
    val katunumero: String?,
    val huoneistonumero: String?,
    val huoneistokirjain: String?,
    val osoitenumero: String?,
    val postinumero: String?,
    val postitoimipaikka: DvvFiSVValue?,
    val rakennustunnus: String?,
    val alkupv: DvvDate?,
    val loppupv: DvvDate?
) : DvvInfoGroup {
    fun katuosoite(): String {
        val streetAddress = (katunimi?.fi ?: "") +
            (if (katunumero != null) " $katunumero" else "") +
            (if (huoneistokirjain != null) " $huoneistokirjain" else "") +
            (if (huoneistonumero != null) " ${huoneistonumero.toInt()}" else "")
        return streetAddress.trim()
    }
}

data class ResidenceCodeDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val rakennustunnus: String?,
    val osoitenumero: String?,
    val huoneistonumero: String?,
    val huoneistokirjain: String?,
    val kuntakoodi: String?,
    val alkupv: DvvDate?,
    val asuinpaikantunnus: String?
) : DvvInfoGroup

data class HomeMunicipalityDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val kuntakoodi: String?,
    val kuntaanMuuttopv: DvvDate?
) : DvvInfoGroup

data class DvvFiSVValue(
    val fi: String?,
    val sv: String?
)

data class CaretakerLimitedDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val huoltaja: DvvSsn,
    val huoltajanRooli: String,
    val huoltajanLaji: String,
    val huoltosuhteenAlkupv: DvvDate?,
    val huoltosuhteenLoppupv: DvvDate?
) : DvvInfoGroup

data class CustodianLimitedDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val huollettava: DvvSsn,
    val huoltajanRooli: String,
    val huoltajanLaji: String,
    val huoltosuhteenAlkupv: DvvDate?,
    val huoltosuhteenLoppupv: DvvDate?
) : DvvInfoGroup

data class DvvSsn(
    val henkilotunnus: String?
)

data class DeathDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val kuollut: Boolean?,
    val kuolinpv: DvvDate?
) : DvvInfoGroup

data class SsnDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val voimassaolo: String,
    val muutettuHenkilotunnus: String,
    val aktiivinenHenkilotunnus: String,
    val edellisetHenkilotunnukset: List<String>
) : DvvInfoGroup

data class DvvDate(
    val arvo: String,
    val tarkkuus: String
) {
    fun asLocalDate(): LocalDate? {
        try {
            return LocalDate.parse(arvo)
        } catch (e: DateTimeParseException) {
            return null
        }
    }
}
