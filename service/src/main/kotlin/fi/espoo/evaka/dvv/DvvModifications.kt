// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.format.DateTimeParseException

@JsonIgnoreProperties(ignoreUnknown = true)
data class DvvModificationsResponse(
    @JsonProperty("viimeisinKirjausavain")
    val modificationToken: String,
    @JsonProperty("muutokset")
    val modifications: List<DvvModification>,
    @JsonProperty("ajanTasalla")
    val upToDate: Boolean
)

data class DvvModification(
    @JsonProperty("muutospv")
    val changed: String,
    @JsonProperty("henkilotunnus")
    val ssn: String,
    @JsonProperty("tietoryhmat")
    val infoGroups: List<DvvInfoGroup>,
    @JsonProperty("ajanTasalla")
    val upToDate: Boolean?
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
    JsonSubTypes.Type(value = SsnDvvInfoGroup::class, name = "HENKILOTUNNUS_KORJAUS")
)

interface DvvInfoGroup {
    val type: String
}

data class DefaultDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?
) : DvvInfoGroup

data class PersonNameDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("etunimi")
    val firstName: String?,
    @JsonProperty("sukunimi")
    val lastName: String?,
    @JsonProperty("alkupv")
    val startDate: DvvDate?,
    @JsonProperty("lisatieto")
    val additionalInfo: String?
) : DvvInfoGroup

data class PersonNameChangeDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("nimilaji")
    val nameType: String?,
    @JsonProperty("nimi")
    val name: String?,
    @JsonProperty("alkupv")
    val startDate: DvvDate?,
    @JsonProperty("loppupv")
    val endDate: DvvDate?
) : DvvInfoGroup

data class RestrictedInfoDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("turvakieltoAktiivinen")
    val restrictedActive: Boolean,
    @JsonProperty("turvaLoppuPv")
    val restrictedEndDate: DvvDate?
) : DvvInfoGroup

data class AddressDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("katunimi")
    val streetName: DvvFiSVValue?,
    @JsonProperty("katunumero")
    val streetNumber: String?,
    @JsonProperty("huoneistonumero")
    val appartmentNumber: String?,
    @JsonProperty("huoneistokirjain")
    val houseLetter: String?,
    @JsonProperty("postinumero")
    val postalCode: String?,
    @JsonProperty("postitoimipaikka")
    val postOffice: DvvFiSVValue?,
    @JsonProperty("alkupv")
    val startDate: DvvDate?,
    @JsonProperty("loppupv")
    val endDate: DvvDate?
) : DvvInfoGroup {
    fun streetAddress(): String {
        val streetAddress = (streetName?.fi ?: "") +
            (if (streetNumber != null) " $streetNumber" else "") +
            (if (houseLetter != null) " $houseLetter" else "") +
            (if (appartmentNumber != null) " ${appartmentNumber.toInt()}" else "")
        return streetAddress.trim()
    }

// TODO: calculate residence code
// Asuinpaikan tunnus pitää sisällään VTJ-PRT (pysyvän rakennustunnuksen), osoitenron, portaan, numeron ja jakokirjaimen.
// VAKITUINEN_KOTIMAINEN_OSOITE tietoryhmässä on nämä kaikki tiedot jo mukana.
}

data class DvvFiSVValue(
    @JsonProperty("fi")
    val fi: String?,
    @JsonProperty("sv")
    val sv: String?
)

data class CaretakerLimitedDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("huoltaja")
    val caretaker: DvvSsn,
    @JsonProperty("huoltajanRooli")
    val caretakersRole: String,
    @JsonProperty("huoltajanLaji")
    val caretakersKind: String,
    @JsonProperty("huoltosuhteenAlkupv")
    val caretakingStartDate: DvvDate?,
    @JsonProperty("huoltosuhteenLoppupv")
    val caretakingEndDate: DvvDate?
) : DvvInfoGroup

data class CustodianLimitedDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("huollettava")
    val custodian: DvvSsn,
    @JsonProperty("huoltajanRooli")
    val caretakersRole: String,
    @JsonProperty("huoltajanLaji")
    val caretakersKind: String,
    @JsonProperty("huoltosuhteenAlkupv")
    val caretakingStartDate: DvvDate?,
    @JsonProperty("huoltosuhteenLoppupv")
    val caretakingEndDate: DvvDate?
) : DvvInfoGroup

data class DvvSsn(
    @JsonProperty("henkilotunnus")
    val ssn: String?
)

data class DeathDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("kuollut")
    val dead: Boolean?,
    @JsonProperty("kuolinpv")
    val dateOfDeath: DvvDate?
) : DvvInfoGroup

data class SsnDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("voimassaolo")
    val activeState: String,
    @JsonProperty("muutettuHenkilotunnus")
    val modifiedSsn: String,
    @JsonProperty("aktiivinenHenkilotunnus")
    val activeSsn: String,
    @JsonProperty("edellisetHenkilotunnukset")
    val previousSsns: List<String>
) : DvvInfoGroup

data class DvvDate(
    @JsonProperty("arvo")
    val date: String,
    @JsonProperty("tarkkuus")
    val granularity: String
) {

    fun asLocalDate(): LocalDate? {
        try {
            return LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            return null
        }
    }
}
