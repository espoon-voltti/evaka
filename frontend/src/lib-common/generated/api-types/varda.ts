// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import LocalDate from '../../local-date'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint.DecisionPeriod
*/
export interface DecisionPeriod {
  alkamis_pvm: LocalDate
  id: number
  paattymis_pvm: LocalDate
}

/**
* Generated from fi.espoo.evaka.varda.VardaChildRequest
*/
export interface VardaChildRequest {
  henkilo: string | null
  henkilo_oid: string | null
  id: UUID
  lahdejarjestelma: string
  oma_organisaatio_oid: string | null
  paos_organisaatio_oid: string | null
  vakatoimija_oid: string | null
}

/**
* Generated from fi.espoo.evaka.varda.VardaDecision
*/
export interface VardaDecision {
  alkamis_pvm: LocalDate
  hakemus_pvm: LocalDate
  jarjestamismuoto_koodi: string
  kokopaivainen_vaka_kytkin: boolean
  lahdejarjestelma: string
  lapsi: string
  paattymis_pvm: LocalDate
  paivittainen_vaka_kytkin: boolean
  pikakasittely_kytkin: boolean
  tilapainen_vaka_kytkin: boolean
  tuntimaara_viikossa: number
  vuorohoito_kytkin: boolean
}

/**
* Generated from fi.espoo.evaka.varda.VardaFeeData
*/
export interface VardaFeeData {
  alkamis_pvm: LocalDate
  asiakasmaksu: number
  huoltajat: VardaGuardian[]
  lahdejarjestelma: string
  lapsi: string
  maksun_peruste_koodi: string
  paattymis_pvm: LocalDate | null
  palveluseteli_arvo: number
  perheen_koko: number
}

/**
* Generated from fi.espoo.evaka.varda.VardaGuardian
*/
export interface VardaGuardian {
  etunimet: string
  henkilo_oid: string | null
  henkilotunnus: string | null
  sukunimi: string
}

/**
* Generated from fi.espoo.evaka.varda.VardaPersonRequest
*/
export interface VardaPersonRequest {
  etunimet: string
  henkilo_oid: string | null
  henkilotunnus: string | null
  id: UUID
  kutsumanimi: string
  sukunimi: string
}

/**
* Generated from fi.espoo.evaka.varda.VardaPlacement
*/
export interface VardaPlacement {
  alkamis_pvm: LocalDate
  lahdejarjestelma: string
  paattymis_pvm: LocalDate | null
  toimipaikka_oid: string
  varhaiskasvatuspaatos: string
}

/**
* Generated from fi.espoo.evaka.varda.VardaUnitRequest
*/
export interface VardaUnitRequest {
  alkamis_pvm: string | null
  asiointikieli_koodi: string[]
  id: number | null
  jarjestamismuoto_koodi: string[]
  kasvatusopillinen_jarjestelma_koodi: string | null
  kayntiosoite: string | null
  kayntiosoite_postinumero: string | null
  kayntiosoite_postitoimipaikka: string | null
  kielipainotus_kytkin: boolean
  kunta_koodi: string | null
  lahdejarjestelma: string | null
  nimi: string | null
  organisaatio_oid: string | null
  paattymis_pvm: string | null
  postinumero: string | null
  postiosoite: string | null
  postitoimipaikka: string | null
  puhelinnumero: string | null
  sahkopostiosoite: string | null
  toiminnallinenpainotus_kytkin: boolean | null
  toimintamuoto_koodi: string | null
  vakajarjestaja: string | null
  varhaiskasvatuspaikat: number
}
