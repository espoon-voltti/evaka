// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.Instant
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

data class VardaDecisionResponse(@JsonProperty("id") val vardaDecisionId: Long)

data class VardaPlacement(
    val varhaiskasvatuspaatos: String,
    val toimipaikka_oid: String,
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate?,
    val lahdejarjestelma: String
)

data class VardaPlacementResponse(@JsonProperty("id") val vardaPlacementId: Long)

internal val vardaPlacementTypes =
    arrayOf(
        PlacementType.DAYCARE,
        PlacementType.DAYCARE_PART_TIME,
        PlacementType.DAYCARE_FIVE_YEAR_OLDS,
        PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
        PlacementType.PRESCHOOL_DAYCARE,
        PlacementType.PREPARATORY_DAYCARE
    )
internal val vardaTemporaryPlacementTypes =
    arrayOf(PlacementType.TEMPORARY_DAYCARE, PlacementType.TEMPORARY_DAYCARE_PART_DAY)

enum class FeeBasisCode(val code: String) {
    FIVE_YEAR_OLDS_DAYCARE("MP02"),
    DAYCARE("MP03")
}

data class VardaGuardian(
    @JsonInclude(JsonInclude.Include.NON_NULL) val henkilotunnus: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL) val henkilo_oid: String? = null,
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

@JsonIgnoreProperties(ignoreUnknown = true) data class VardaFeeDataResponse(val id: Long)

data class VardaGuardianWithId(
    val id: PersonId,
    val henkilotunnus: String?,
    val henkilo_oid: String?,
    val etunimet: String,
    val sukunimi: String,
    val asuinpaikantunnus: String?
) {
    fun toVardaGuardian(): VardaGuardian =
        VardaGuardian(
            henkilotunnus = henkilotunnus,
            henkilo_oid = henkilo_oid,
            etunimet = etunimet,
            sukunimi = sukunimi
        )
}

data class VardaChildCalculatedServiceNeedChanges(
    val childId: ChildId,
    val additions: List<ServiceNeedId>,
    val updates: List<ServiceNeedId>,
    val deletes: List<ServiceNeedId>
)

data class VardaServiceNeed(
    val evakaChildId: ChildId,
    val evakaServiceNeedId: ServiceNeedId,
    var evakaServiceNeedUpdated: HelsinkiDateTime? = null,
    var vardaChildId: Long? = null,
    var vardaDecisionId: Long? = null,
    var vardaPlacementId: Long? = null,
    var vardaFeeDataIds: List<Long> = listOf(),
    var updateFailed: Boolean = false,
    val errors: MutableList<String> = mutableListOf()
)

data class ChangedChildServiceNeed(
    val evakaChildId: ChildId,
    val evakaServiceNeedId: ServiceNeedId
)

data class FeeDataByServiceNeed(
    val evakaChildId: ChildId,
    val serviceNeedId: ServiceNeedId,
    val feeDecisionIds: List<FeeDecisionId> = emptyList(),
    val voucherValueDecisionIds: List<VoucherValueDecisionId> = emptyList()
) {
    fun hasFeeData() = feeDecisionIds.isNotEmpty() || voucherValueDecisionIds.isNotEmpty()
}

data class EvakaServiceNeedInfoForVarda(
    val id: ServiceNeedId,
    val serviceNeedUpdated: Instant,
    val childId: ChildId,
    val applicationDate: LocalDate,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val urgent: Boolean,
    val hoursPerWeek: Double,
    val temporary: Boolean,
    val daily: Boolean,
    val shiftCare: Boolean,
    val providerType: ProviderType,
    val ophOrganizerOid: String?,
    val ophUnitOid: String?
) {
    private val providerTypeCode = VardaUnitProviderType.valueOf(providerType.toString()).vardaCode

    val asPeriod = DateRange(startDate, endDate)

    fun toVardaDecisionForChild(vardaChildUrl: String, sourceSystem: String): VardaDecision =
        VardaDecision(
            lapsi = vardaChildUrl,
            hakemus_pvm = this.applicationDate,
            alkamis_pvm = this.startDate,
            paattymis_pvm = this.endDate,
            pikakasittely_kytkin = this.urgent,
            tuntimaara_viikossa = this.hoursPerWeek,
            tilapainen_vaka_kytkin = this.temporary,
            paivittainen_vaka_kytkin = this.daily,
            vuorohoito_kytkin = this.shiftCare,
            jarjestamismuoto_koodi = this.providerTypeCode,
            lahdejarjestelma = sourceSystem
        )

    fun toVardaPlacement(vardaDecisionUrl: String, sourceSystem: String): VardaPlacement =
        VardaPlacement(
            varhaiskasvatuspaatos = vardaDecisionUrl,
            toimipaikka_oid =
                this.ophUnitOid
                    ?: error(
                        "VardaUpdate: varda placement cannot be created for service need ${this.id}: unitOid cannot be null"
                    ),
            alkamis_pvm = this.startDate,
            paattymis_pvm = this.endDate,
            lahdejarjestelma = sourceSystem
        )

    fun toVardaServiceNeed(): VardaServiceNeed =
        VardaServiceNeed(
            evakaChildId = this.childId,
            evakaServiceNeedId = this.id,
            evakaServiceNeedUpdated = HelsinkiDateTime.from(this.serviceNeedUpdated)
        )
}
