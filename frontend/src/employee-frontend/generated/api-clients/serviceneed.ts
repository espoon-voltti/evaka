// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { EmployeeServiceApplication } from 'lib-common/generated/api-types/serviceneed'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceApplicationRejection } from 'lib-common/generated/api-types/serviceneed'
import { ServiceNeedCreateRequest } from 'lib-common/generated/api-types/serviceneed'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { ServiceNeedUpdateRequest } from 'lib-common/generated/api-types/serviceneed'
import { UUID } from 'lib-common/types'
import { UndecidedServiceApplicationSummary } from 'lib-common/generated/api-types/serviceneed'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonEmployeeServiceApplication } from 'lib-common/generated/api-types/serviceneed'
import { deserializeJsonServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import { deserializeJsonServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { deserializeJsonUndecidedServiceApplicationSummary } from 'lib-common/generated/api-types/serviceneed'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.deleteServiceNeed
*/
export async function deleteServiceNeed(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/service-needs/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


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
    url: uri`/employee/public/service-needs/options`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonServiceNeedOptionPublicInfo(e))
}


/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.getServiceNeedOptions
*/
export async function getServiceNeedOptions(): Promise<ServiceNeedOption[]> {
  const { data: json } = await client.request<JsonOf<ServiceNeedOption[]>>({
    url: uri`/employee/service-needs/options`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonServiceNeedOption(e))
}


/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.postServiceNeed
*/
export async function postServiceNeed(
  request: {
    body: ServiceNeedCreateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/service-needs`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ServiceNeedCreateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.putServiceNeed
*/
export async function putServiceNeed(
  request: {
    id: UUID,
    body: ServiceNeedUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/service-needs/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ServiceNeedUpdateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationController.acceptServiceApplication
*/
export async function acceptServiceApplication(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/service-applications/${request.id}/accept`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationController.getChildServiceApplications
*/
export async function getChildServiceApplications(
  request: {
    childId: UUID
  }
): Promise<EmployeeServiceApplication[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<EmployeeServiceApplication[]>>({
    url: uri`/employee/service-applications`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonEmployeeServiceApplication(e))
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationController.getUndecidedServiceApplications
*/
export async function getUndecidedServiceApplications(
  request: {
    unitId: UUID
  }
): Promise<UndecidedServiceApplicationSummary[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<UndecidedServiceApplicationSummary[]>>({
    url: uri`/employee/service-applications/undecided`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonUndecidedServiceApplicationSummary(e))
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationController.rejectServiceApplication
*/
export async function rejectServiceApplication(
  request: {
    id: UUID,
    body: ServiceApplicationRejection
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/service-applications/${request.id}/reject`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ServiceApplicationRejection>
  })
  return json
}
