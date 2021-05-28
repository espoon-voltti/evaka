// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../client'
import { ServiceNeedOption } from '../../types/child'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { PlacementType } from 'lib-customizations/types'
import { UUID } from 'lib-common/types'

export interface ServiceNeedCreateRequest {
  placementId: UUID
  startDate: LocalDate
  endDate: LocalDate
  optionId: UUID
  shiftCare: boolean
}

export async function createServiceNeed(
  data: ServiceNeedCreateRequest
): Promise<Result<null>> {
  return client
    .post('/service-needs', data)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export interface ServiceNeedUpdateRequest {
  startDate: LocalDate
  endDate: LocalDate
  optionId: UUID
  shiftCare: boolean
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
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface ServiceNeedOptionPublicInfo {
  id: UUID
  name: string
  validPlacementType: PlacementType
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
