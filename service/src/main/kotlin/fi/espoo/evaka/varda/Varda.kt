// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.placement.PlacementType
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaDecision(
    val lapsi: String,
    val hakemus_pvm: LocalDate,
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate,
    val pikakasittely_kytkin: Boolean,
    val tuntimaara_viikossa: Double,
    val tilapainen_vaka_kytkin: Boolean,
    val paivittainen_vaka_kytkin: Boolean,
    val vuorohoito_kytkin: Boolean,
    val jarjestamismuoto_koodi: String,
    val lahdejarjestelma: String
) {
    val kokopaivainen_vaka_kytkin: Boolean = tuntimaara_viikossa >= 25
}

data class VardaDecisionResponse(
    @JsonProperty("id")
    val vardaDecisionId: Long
)

data class VardaPlacement(
    val varhaiskasvatuspaatos: String,
    val toimipaikka_oid: String,
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate?,
    val lahdejarjestelma: String
)

data class VardaPlacementResponse(
    @JsonProperty("id")
    val vardaPlacementId: Long
)

internal val vardaPlacementTypes = arrayOf(
    PlacementType.DAYCARE,
    PlacementType.DAYCARE_PART_TIME,
    PlacementType.DAYCARE_FIVE_YEAR_OLDS,
    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
    PlacementType.PRESCHOOL_DAYCARE,
    PlacementType.PREPARATORY_DAYCARE
)
internal val vardaTemporaryPlacementTypes = arrayOf(
    PlacementType.TEMPORARY_DAYCARE,
    PlacementType.TEMPORARY_DAYCARE_PART_DAY
)

enum class FeeBasisCode(val code: String) {
    FIVE_YEAR_OLDS_DAYCARE("MP02"),
    DAYCARE("MP03")
}

data class VardaGuardian(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val henkilotunnus: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val henkilo_oid: String? = null,
    val etunimet: String,
    val sukunimi: String
)

data class VardaFeeData(
    val huoltajat: List<VardaGuardian>,
    val lapsi: String,
    val maksun_peruste_koodi: String,
    val palveluseteli_arvo: Double,
    val asiakasmaksu: Double,
    val perheen_koko: Int,
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate?,
    val lahdejarjestelma: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaFeeDataResponse(
    val id: Long
)
