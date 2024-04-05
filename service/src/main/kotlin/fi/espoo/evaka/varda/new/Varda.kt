// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.new

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.varda.VardaUnitProviderType
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate

data class Henkilo(
    val etunimet: String,
    val sukunimi: String,
    val henkilotunnus: String?,
    val henkilo_oid: String?,
) {
    companion object {
        fun fromEvaka(data: VardaPerson): Henkilo =
            Henkilo(
                etunimet = data.firstName,
                sukunimi = data.lastName,
                henkilotunnus = data.socialSecurityNumber,
                henkilo_oid = data.ophPersonOid,
            )
    }

    fun toVarda() =
        VardaWriteClient.CreateHenkiloRequest(
            etunimet = etunimet,
            kutsumanimi = etunimet.split(" ").first(),
            sukunimi = sukunimi,
            // Avoid sending both henkilotunnus and henkilo_oid (error code HE004)
            henkilotunnus = henkilotunnus.takeIf { henkilo_oid == null },
            henkilo_oid = henkilo_oid,
        )
}

data class Lapsi(
    val vakatoimija_oid: String?,
    val oma_organisaatio_oid: String?,
    val paos_organisaatio_oid: String?,
) {
    companion object {
        fun fromEvaka(data: VardaServiceNeed, omaOrganisaatioOid: String): Lapsi =
            if (data.providerType == ProviderType.PRIVATE_SERVICE_VOUCHER) {
                Lapsi(
                    vakatoimija_oid = null,
                    oma_organisaatio_oid = omaOrganisaatioOid,
                    paos_organisaatio_oid = data.ophOrganizerOid,
                )
            } else {
                Lapsi(
                    vakatoimija_oid = data.ophOrganizerOid,
                    oma_organisaatio_oid = null,
                    paos_organisaatio_oid = null
                )
            }

        fun fromVarda(data: VardaReadClient.LapsiResponse): Lapsi =
            Lapsi(
                vakatoimija_oid = data.vakatoimija_oid,
                oma_organisaatio_oid = data.oma_organisaatio_oid,
                paos_organisaatio_oid = data.paos_organisaatio_oid,
            )
    }

    fun toVarda(lahdejarjestelma: String, henkilo: URI) =
        VardaWriteClient.CreateLapsiRequest(
            lahdejarjestelma = lahdejarjestelma,
            henkilo = henkilo,
            vakatoimija_oid = vakatoimija_oid,
            oma_organisaatio_oid = oma_organisaatio_oid,
            paos_organisaatio_oid = paos_organisaatio_oid
        )
}

data class Varhaiskasvatuspaatos(
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate?,
    val hakemus_pvm: LocalDate,
    val vuorohoito_kytkin: Boolean,
    val tilapainen_vaka_kytkin: Boolean,
    val tuntimaara_viikossa: Double,
    val paivittainen_vaka_kytkin: Boolean,
    val kokopaivainen_vaka_kytkin: Boolean,
    val jarjestamismuoto_koodi: String,
) {
    companion object {
        fun fromEvaka(data: VardaServiceNeed): Varhaiskasvatuspaatos =
            Varhaiskasvatuspaatos(
                hakemus_pvm = data.applicationDate,
                alkamis_pvm = data.range.start,
                paattymis_pvm = data.range.end,
                tuntimaara_viikossa = data.hoursPerWeek,
                tilapainen_vaka_kytkin = data.temporary,
                paivittainen_vaka_kytkin = data.daily,
                kokopaivainen_vaka_kytkin = data.hoursPerWeek >= 25,
                vuorohoito_kytkin = data.shiftCare,
                jarjestamismuoto_koodi =
                    VardaUnitProviderType.fromEvakaProviderType(data.providerType).vardaCode,
            )

        fun fromVarda(data: VardaReadClient.VarhaiskasvatuspaatosResponse) =
            Varhaiskasvatuspaatos(
                hakemus_pvm = data.hakemus_pvm,
                alkamis_pvm = data.alkamis_pvm,
                paattymis_pvm = data.paattymis_pvm,
                tuntimaara_viikossa = data.tuntimaara_viikossa,
                tilapainen_vaka_kytkin = data.tilapainen_vaka_kytkin,
                paivittainen_vaka_kytkin = data.paivittainen_vaka_kytkin,
                kokopaivainen_vaka_kytkin = data.kokopaivainen_vaka_kytkin,
                vuorohoito_kytkin = data.vuorohoito_kytkin,
                jarjestamismuoto_koodi = data.jarjestamismuoto_koodi,
            )
    }

    fun toVarda(lahdejarjestelma: String, lapsi: URI) =
        VardaWriteClient.CreateVarhaiskasvatuspaatosRequest(
            lahdejarjestelma = lahdejarjestelma,
            lapsi = lapsi,
            alkamis_pvm = alkamis_pvm,
            paattymis_pvm = paattymis_pvm,
            hakemus_pvm = hakemus_pvm,
            vuorohoito_kytkin = vuorohoito_kytkin,
            tilapainen_vaka_kytkin = tilapainen_vaka_kytkin,
            tuntimaara_viikossa = tuntimaara_viikossa,
            paivittainen_vaka_kytkin = paivittainen_vaka_kytkin,
            kokopaivainen_vaka_kytkin = kokopaivainen_vaka_kytkin,
            jarjestamismuoto_koodi = jarjestamismuoto_koodi,
        )
}

data class Varhaiskasvatussuhde(
    val toimipaikka_oid: String,
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate?,
) {
    companion object {
        fun fromEvaka(data: VardaServiceNeed): Varhaiskasvatussuhde =
            Varhaiskasvatussuhde(
                toimipaikka_oid = data.ophUnitOid,
                alkamis_pvm = data.range.start,
                paattymis_pvm = data.range.end,
            )

        fun fromVarda(data: VardaReadClient.VarhaiskasvatussuhdeResponse) =
            Varhaiskasvatussuhde(
                toimipaikka_oid = data.toimipaikka_oid,
                alkamis_pvm = data.alkamis_pvm,
                paattymis_pvm = data.paattymis_pvm,
            )
    }

    fun toVarda(lahdejarjestelma: String, varhaiskasvatuspaatos: URI) =
        VardaWriteClient.CreateVarhaiskasvatussuhdeRequest(
            lahdejarjestelma = lahdejarjestelma,
            varhaiskasvatuspaatos = varhaiskasvatuspaatos,
            toimipaikka_oid = toimipaikka_oid,
            alkamis_pvm = alkamis_pvm,
            paattymis_pvm = paattymis_pvm,
        )
}

enum class MaksunPerusteKoodi(val code: String) {
    FIVE_YEAR_OLDS_DAYCARE("MP02"),
    DAYCARE("MP03")
}

data class Maksutieto(
    val huoltajat: List<Huoltaja>,
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate?,
    val perheen_koko: Int?,
    val maksun_peruste_koodi: String,
    val asiakasmaksu: BigDecimal,
    val palveluseteli_arvo: BigDecimal?
) {
    companion object {
        fun fromEvaka(guardians: List<PersonDTO>, data: VardaFeeData): Maksutieto? {
            val huoltajat =
                guardians
                    .filter {
                        it.identity is ExternalIdentifier.SSN &&
                            (it.id == data.headOfFamilyId || it.id == data.partnerId)
                    }
                    .map {
                        Huoltaja(
                            henkilotunnus = (it.identity as ExternalIdentifier.SSN).ssn,
                            henkilo_oid = it.ophPersonOid?.takeIf { oid -> oid.isNotBlank() },
                            etunimet = it.firstName,
                            sukunimi = it.lastName,
                        )
                    }
            if (huoltajat.isEmpty()) {
                return null
            }

            return Maksutieto(
                huoltajat = huoltajat,
                alkamis_pvm = data.validDuring.start,
                paattymis_pvm = data.validDuring.end,
                maksun_peruste_koodi =
                    if (
                            data.placementType == PlacementType.DAYCARE_FIVE_YEAR_OLDS ||
                                data.placementType == PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
                        ) {
                            MaksunPerusteKoodi.FIVE_YEAR_OLDS_DAYCARE
                        } else {
                            MaksunPerusteKoodi.DAYCARE
                        }
                        .code,
                perheen_koko = data.familySize,
                asiakasmaksu = BigDecimal(data.totalFee).divide(BigDecimal(100)),
                palveluseteli_arvo =
                    data.voucherValue?.let { BigDecimal(it).divide(BigDecimal(100)) },
            )
        }

        fun fromVarda(data: VardaReadClient.MaksutietoResponse): Maksutieto =
            Maksutieto(
                huoltajat = data.huoltajat,
                alkamis_pvm = data.alkamis_pvm,
                paattymis_pvm = data.paattymis_pvm,
                perheen_koko = data.perheen_koko,
                maksun_peruste_koodi = data.maksun_peruste_koodi,
                asiakasmaksu = data.asiakasmaksu,
                palveluseteli_arvo = data.palveluseteli_arvo,
            )
    }

    fun toVarda(lahdejarjestelma: String, lapsi: URI) =
        VardaWriteClient.CreateMaksutietoRequest(
            lahdejarjestelma = lahdejarjestelma,
            lapsi = lapsi,
            huoltajat =
                huoltajat.map {
                    // Avoid sending both henkilotunnus and henkilo_oid, which would cause Varda to
                    // reject the request with error HE004
                    if (it.henkilotunnus != null && it.henkilo_oid != null) {
                        it.copy(henkilo_oid = null)
                    } else {
                        it
                    }
                },
            alkamis_pvm = alkamis_pvm,
            paattymis_pvm = paattymis_pvm,
            maksun_peruste_koodi = maksun_peruste_koodi,
            palveluseteli_arvo = palveluseteli_arvo,
            asiakasmaksu = asiakasmaksu,
            perheen_koko = perheen_koko,
        )
}
