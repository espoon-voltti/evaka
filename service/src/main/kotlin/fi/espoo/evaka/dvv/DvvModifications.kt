// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

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
    JsonSubTypes.Type(value = DeathDvvInfoGroup::class, name = "KUOLINPAIVA"),
    JsonSubTypes.Type(value = RestrictedInfoDvvInfoGroup::class, name = "TURVAKIELTO"),
    JsonSubTypes.Type(value = SsnDvvInfoGroup::class, name = "HENKILOTUNNUS_KORJAUS"),
    JsonSubTypes.Type(value = CaretakerLimitedDvvInfoGroup::class, name = "HUOLTAJA_SUPPEA"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "HENKILON_NIMI"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "NIMENMUUTOS"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "NIMENMUUTOS_LAAJA"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "VAKINAINEN_KOTIMAINEN_OSOITE"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "HUOLLETTAVA_SUPPEA"),
    JsonSubTypes.Type(
        value = DefaultDvvInfoGroup::class,
        name = "VAKINAINEN_KOTIMAINEN_ASUINPAIKKATUNNUS"
    ),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "KOTIKUNTA"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "LAPSI"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "AIDINKIELI"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "TILAPAINEN_ULKOMAINEN_OSOITE"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "KOTIMAINEN_POSTIOSOITE"),
    JsonSubTypes.Type(value = DefaultDvvInfoGroup::class, name = "VAKINAINEN_ULKOMAINEN_OSOITE")
)
interface DvvInfoGroup {
    val tietoryhma: String
}

data class DefaultDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?
) : DvvInfoGroup

data class RestrictedInfoDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val turvakieltoAktiivinen: Boolean,
    val turvaLoppuPv: DvvDate?
) : DvvInfoGroup

data class CaretakerLimitedDvvInfoGroup(
    override val tietoryhma: String,
    val muutosattribuutti: String?,
    val huoltaja: DvvSsn,
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
    fun asLocalDate(): LocalDate = LocalDate.parse(arvo)
}
