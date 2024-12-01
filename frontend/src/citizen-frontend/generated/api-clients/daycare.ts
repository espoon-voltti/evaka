// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import { ApplicationUnitType } from 'lib-common/generated/api-types/daycare'
import { AxiosHeaders } from 'axios'
import { ClubTerm } from 'lib-common/generated/api-types/daycare'
import { JsonOf } from 'lib-common/json'
import { PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonClubTerm } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonPreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonPublicUnit } from 'lib-common/generated/api-types/daycare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.daycare.controllers.LocationControllerCitizen.getAllApplicableUnits
*/
export async function getAllApplicableUnits(
  request: {
    applicationType: ApplicationType
  },
  headers?: AxiosHeaders
): Promise<PublicUnit[]> {
  const { data: json } = await client.request<JsonOf<PublicUnit[]>>({
    url: uri`/citizen/public/units/${request.applicationType}`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonPublicUnit(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.LocationControllerCitizen.getApplicationUnits
*/
export async function getApplicationUnits(
  request: {
    type: ApplicationUnitType,
    date: LocalDate,
    shiftCare?: boolean | null
  },
  headers?: AxiosHeaders
): Promise<PublicUnit[]> {
  const params = createUrlSearchParams(
    ['type', request.type.toString()],
    ['date', request.date.formatIso()],
    ['shiftCare', request.shiftCare?.toString()]
  )
  const { data: json } = await client.request<JsonOf<PublicUnit[]>>({
    url: uri`/citizen/units`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json.map(e => deserializeJsonPublicUnit(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.getClubTerms
*/
export async function getClubTerms(
  headers?: AxiosHeaders
): Promise<ClubTerm[]> {
  const { data: json } = await client.request<JsonOf<ClubTerm[]>>({
    url: uri`/citizen/public/club-terms`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonClubTerm(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.getPreschoolTerms
*/
export async function getPreschoolTerms(
  headers?: AxiosHeaders
): Promise<PreschoolTerm[]> {
  const { data: json } = await client.request<JsonOf<PreschoolTerm[]>>({
    url: uri`/citizen/public/preschool-terms`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonPreschoolTerm(e))
}
