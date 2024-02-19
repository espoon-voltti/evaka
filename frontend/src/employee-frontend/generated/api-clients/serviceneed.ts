// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceNeedCreateRequest } from 'lib-common/generated/api-types/serviceneed'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { ServiceNeedUpdateRequest } from 'lib-common/generated/api-types/serviceneed'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
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
    url: uri`/service-needs/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.getServiceNeedOptionPublicInfos
*/
export async function getServiceNeedOptionPublicInfos(
  request: {
    placementTypes: PlacementType[]
  }
): Promise<ServiceNeedOptionPublicInfo[]> {
  const params = createUrlSearchParams(
    ...(request.placementTypes.map((e): [string, string | null | undefined] => ['placementTypes', e.toString()]))
  )
  const { data: json } = await client.request<JsonOf<ServiceNeedOptionPublicInfo[]>>({
    url: uri`/public/service-needs/options`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.getServiceNeedOptions
*/
export async function getServiceNeedOptions(): Promise<ServiceNeedOption[]> {
  const { data: json } = await client.request<JsonOf<ServiceNeedOption[]>>({
    url: uri`/service-needs/options`.toString(),
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
    url: uri`/service-needs`.toString(),
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
    url: uri`/service-needs/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ServiceNeedUpdateRequest>
  })
  return json
}
