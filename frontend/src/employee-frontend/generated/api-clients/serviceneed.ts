// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AcceptServiceApplicationBody } from 'lib-common/generated/api-types/serviceneed'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { EmployeeServiceApplication } from 'lib-common/generated/api-types/serviceneed'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ServiceApplicationId } from 'lib-common/generated/api-types/shared'
import type { ServiceApplicationRejection } from 'lib-common/generated/api-types/serviceneed'
import type { ServiceNeedCreateRequest } from 'lib-common/generated/api-types/serviceneed'
import type { ServiceNeedId } from 'lib-common/generated/api-types/shared'
import type { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import type { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import type { ServiceNeedUpdateRequest } from 'lib-common/generated/api-types/serviceneed'
import type { UndecidedServiceApplicationSummary } from 'lib-common/generated/api-types/serviceneed'
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
    id: ServiceNeedId
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
    id: ServiceNeedId,
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
    id: ServiceApplicationId,
    body: AcceptServiceApplicationBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/service-applications/${request.id}/accept`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AcceptServiceApplicationBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationController.getChildServiceApplications
*/
export async function getChildServiceApplications(
  request: {
    childId: PersonId
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
    unitId: DaycareId
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
    id: ServiceApplicationId,
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
