// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../client'
import { ServiceNeedOption } from '../../types/child'
import { JsonOf } from 'lib-common/json'
import LocalDate from '../../../lib-common/local-date'
import { UUID } from '../../types'

export interface NewServiceNeedCreateRequest {
  placementId: UUID
  startDate: LocalDate
  endDate: LocalDate
  optionId: UUID
  shiftCare: boolean
}

export async function createNewServiceNeed(
  data: NewServiceNeedCreateRequest
): Promise<Result<null>> {
  return client
    .post('/new-service-needs', {
      ...data,
      startDate: data.startDate.toJSON(),
      endDate: data.endDate.toJSON()
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export interface NewServiceNeedUpdateRequest {
  startDate: LocalDate
  endDate: LocalDate
  optionId: UUID
  shiftCare: boolean
}

export async function updateNewServiceNeed(
  id: UUID,
  data: NewServiceNeedUpdateRequest
): Promise<Result<null>> {
  return client
    .put(`/new-service-needs/${id}`, {
      ...data,
      startDate: data.startDate.toJSON(),
      endDate: data.endDate.toJSON()
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deleteNewServiceNeed(id: UUID): Promise<Result<null>> {
  return client
    .delete(`/new-service-needs/${id}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function getServiceNeedOptions(): Promise<
  Result<ServiceNeedOption[]>
> {
  return client
    .get<JsonOf<ServiceNeedOption[]>>('/new-service-needs/options')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
