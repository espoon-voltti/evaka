// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CitizenServiceApplication } from 'lib-common/generated/api-types/serviceneed'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ServiceApplicationCreateRequest } from 'lib-common/generated/api-types/serviceneed'
import type { ServiceApplicationId } from 'lib-common/generated/api-types/shared'
import type { ServiceNeedOptionBasics } from 'lib-common/generated/api-types/serviceneed'
import type { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonCitizenServiceApplication } from 'lib-common/generated/api-types/serviceneed'
import { deserializeJsonServiceNeedOptionBasics } from 'lib-common/generated/api-types/serviceneed'
import { deserializeJsonServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.getServiceNeedOptionPublicInfos
*/
export async function getServiceNeedOptionPublicInfos(
  request: {
    placementTypes?: PlacementType[] | null
  }
): Promise<ServiceNeedOptionPublicInfo[]> {
  const params = createUrlSearchParams(
    ...(request.placementTypes?.map((e): [string, string | null | undefined] => ['placementTypes', e.toString()]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<ServiceNeedOptionPublicInfo[]>>({
    url: uri`/citizen/public/service-needs/options`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonServiceNeedOptionPublicInfo(e))
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationControllerCitizen.createServiceApplication
*/
export async function createServiceApplication(
  request: {
    body: ServiceApplicationCreateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/service-applications`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ServiceApplicationCreateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationControllerCitizen.deleteServiceApplication
*/
export async function deleteServiceApplication(
  request: {
    id: ServiceApplicationId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/service-applications/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationControllerCitizen.getChildServiceApplications
*/
export async function getChildServiceApplications(
  request: {
    childId: PersonId
  }
): Promise<CitizenServiceApplication[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<CitizenServiceApplication[]>>({
    url: uri`/citizen/service-applications`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonCitizenServiceApplication(e))
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationControllerCitizen.getChildServiceNeedOptions
*/
export async function getChildServiceNeedOptions(
  request: {
    childId: PersonId,
    date: LocalDate
  }
): Promise<ServiceNeedOptionBasics[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId],
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<ServiceNeedOptionBasics[]>>({
    url: uri`/citizen/service-applications/options`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonServiceNeedOptionBasics(e))
}
