// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
data class DvvUpdateInfoResponse(
    @JsonProperty("viimeisinKirjausavain")
    val updateToken: String,
    @JsonProperty("muutokset")
    val updateInfos: List<DvvUpdateInfo>
)

data class DvvUpdateInfo(
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
    JsonSubTypes.Type(value = RestrictedInfo::class, name = "TURVAKIELTO")
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
    val startDate: Any?,
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
    val startDate: Any?,
    @JsonProperty("loppupv")
    val endDate: Any?
) : DvvInfoGroup

data class RestrictedInfo(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("turvakieltoAktiivinen")
    val restrictedActive: Boolean,
    @JsonProperty("turvaLoppuPv")
    val restrictedEndDate: DvvDate?
) : DvvInfoGroup

data class CaretakerLimitedDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String
) : DvvInfoGroup

data class CustodianLimitedDvvInfoGroup(
    @JsonProperty("tietoryhma")
    override val type: String,
    @JsonProperty("muutosattribuutti")
    val changeAttribute: String?,
    @JsonProperty("huollettava")
    val custodian: CustodianInfo,
    @JsonProperty("huoltajanRooli")
    val caretakersRole: String,
    @JsonProperty("huoltajanLaji")
    val caretakersKind: String
) : DvvInfoGroup

data class CustodianInfo(
    @JsonProperty("henkilotunnus")
    val ssn: String?
)

/*
          "turvaLoppuPv": {
            "arvo": "2019-09-25",
            "tarkkuus": "PAIVA"
          },
 */
data class DvvDate(
    @JsonProperty("arvo")
    val date: String,
    @JsonProperty("tarkkuus")
    val granularity: String
)