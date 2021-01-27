// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'types'
import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { client } from 'api/client'
import { ServiceNeed } from 'types/child'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

export interface ServiceNeedRequest {
  startDate: LocalDate
  endDate: LocalDate | null
  hoursPerWeek: number
  partDay: boolean
  partWeek: boolean
  shiftCare: boolean
  notes: string
}

export async function createServiceNeed(
  childId: UUID,
  serviceNeedData: ServiceNeedRequest
): Promise<Result<ServiceNeed>> {
  return client
    .post<JsonOf<ServiceNeed>>(`/children/${childId}/service-needs`, {
      ...serviceNeedData,
      // TODO: remove once backend no longer requires them
      preschool: false,
      freeForFiveYearOlds: false
    })
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseNullableIso(data.endDate),
      updated: new Date(data.updated)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getServiceNeeds(
  childId: UUID
): Promise<Result<ServiceNeed[]>> {
  return client
    .get<JsonOf<ServiceNeed[]>>(`/children/${childId}/service-needs`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        startDate: LocalDate.parseIso(data.startDate),
        endDate: LocalDate.parseNullableIso(data.endDate),
        updated: new Date(data.updated)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateServiceNeed(
  id: UUID,
  serviceNeedData: ServiceNeedRequest
): Promise<Result<ServiceNeed>> {
  return client
    .put<JsonOf<ServiceNeed>>(`/service-needs/${id}`, serviceNeedData)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseNullableIso(data.endDate),
      updated: new Date(data.updated)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function removeServiceNeed(id: UUID): Promise<Result<null>> {
  return client
    .delete(`/service-needs/${id}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
