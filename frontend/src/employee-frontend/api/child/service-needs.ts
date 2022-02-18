// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import {
  ServiceNeedCreateRequest,
  ServiceNeedOption,
  ServiceNeedOptionPublicInfo,
  ServiceNeedUpdateRequest
} from 'lib-common/generated/api-types/serviceneed'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function createServiceNeed(
  data: ServiceNeedCreateRequest
): Promise<Result<null>> {
  return client
    .post('/service-needs', data)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function updateServiceNeed(
  id: UUID,
  data: ServiceNeedUpdateRequest
): Promise<Result<null>> {
  return client
    .put(`/service-needs/${id}`, data)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deleteServiceNeed(id: UUID): Promise<Result<null>> {
  return client
    .delete(`/service-needs/${id}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function getServiceNeedOptions(): Promise<
  Result<ServiceNeedOption[]>
> {
  return client
    .get<JsonOf<ServiceNeedOption[]>>('/service-needs/options')
    .then((res) => Success.of(deserializeServiceNeedOptions(res.data)))
    .catch((e) => Failure.fromError(e))
}

const deserializeServiceNeedOptions = (
  options: JsonOf<ServiceNeedOption[]>
): ServiceNeedOption[] => {
  return options.map((option) => ({
    ...option,
    updated: new Date(option.updated)
  }))
}

export async function getServiceNeedOptionPublicInfos(
  placementTypes: PlacementType[]
): Promise<Result<ServiceNeedOptionPublicInfo[]>> {
  return client
    .get<JsonOf<ServiceNeedOptionPublicInfo[]>>(
      '/public/service-needs/options',
      { params: { placementTypes: placementTypes.join() } }
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
