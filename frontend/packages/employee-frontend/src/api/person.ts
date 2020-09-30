// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '~/api'
import {
  deserializePersonDetails,
  PersonContactInfo,
  PersonDetails,
  PersonWithChildren,
  SearchColumn
} from '~/types/person'
import { SearchOrder, UUID } from '~types'
import { client } from '~/api/client'
import { ApplicationSummary } from '~types/application'
import { Decision } from '~types/decision'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

export async function getPersonDetails(
  id: UUID
): Promise<Result<PersonDetails>> {
  return client
    .get<JsonOf<PersonDetails>>(`/person/details/${id}`)
    .then((res) => res.data)
    .then(deserializePersonDetails)
    .then(Success)
    .catch(Failure)
}

export async function fridgeHeadPerson(
  id: UUID,
  fridgeHeadPerson: PersonContactInfo
): Promise<Result<PersonContactInfo>> {
  return client
    .put<JsonOf<PersonContactInfo>>(
      `/person/${id}/contact-info`,
      fridgeHeadPerson
    )
    .then((res) => Success(res.data))
    .catch(Failure)
}

export async function patchPersonDetails(
  id: UUID,
  data: PatchPersonRequest
): Promise<Result<PersonDetails>> {
  return client
    .patch<JsonOf<PersonDetails>>(`/person/${id}`, data)
    .then(({ data }) => deserializePersonDetails(data))
    .then(Success)
    .catch(Failure)
}

export async function addSsn(
  id: UUID,
  ssn: string
): Promise<Result<PersonDetails>> {
  return client
    .put<JsonOf<PersonDetails>>(`/person/${id}/ssn`, { ssn })
    .then(({ data }) => deserializePersonDetails(data))
    .then(Success)
    .catch(Failure)
}

export async function getOrCreatePersonBySsn(
  ssn: string,
  readonly = false
): Promise<Result<PersonDetails>> {
  return client
    .get<JsonOf<PersonDetails>>(`/person/details/ssn/${ssn}`, {
      params: {
        readonly
      }
    })
    .then(({ data }) => deserializePersonDetails(data))
    .then(Success)
    .catch(Failure)
}

export async function findByNameOrAddress(
  searchTerm: string,
  orderBy: SearchColumn,
  sortDirection: SearchOrder
): Promise<Result<PersonDetails[]>> {
  return client
    .get<JsonOf<PersonDetails[]>>('/person/search', {
      params: {
        searchTerm,
        orderBy,
        sortDirection
      }
    })
    .then((res) => res.data)
    .then((results) => results.map(deserializePersonDetails))
    .then(Success)
    .catch(Failure)
}

export async function getGuardianApplicationSummaries(
  guardianId: UUID
): Promise<Result<ApplicationSummary[]>> {
  return client
    .get<JsonOf<ApplicationSummary[]>>(
      `/v2/applications/by-guardian/${guardianId}`
    )
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        startDate: LocalDate.parseIso(data.startDate),
        sentDate: LocalDate.parseNullableIso(data.sentDate)
      }))
    )
    .then(Success)
    .catch(Failure)
}

export async function getChildApplicationSummaries(
  childId: UUID
): Promise<Result<ApplicationSummary[]>> {
  return client
    .get<JsonOf<ApplicationSummary[]>>(`/v2/applications/by-child/${childId}`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        startDate: LocalDate.parseIso(data.startDate),
        sentDate: LocalDate.parseNullableIso(data.sentDate)
      }))
    )
    .then(Success)
    .catch(Failure)
}

export async function getPersonDependants(
  guardianId: UUID
): Promise<Result<PersonWithChildren[]>> {
  return client
    .get<JsonOf<PersonWithChildren[]>>(`/person/dependants/${guardianId}`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        dateOfBirth: LocalDate.parseIso(data.dateOfBirth),
        restrictedDetails: {
          ...data.restrictedDetails,
          endDate: LocalDate.parseNullableIso(data.restrictedDetails.endDate)
        },
        children: data.children.map((child) => ({
          ...child,
          dateOfBirth: LocalDate.parseIso(child.dateOfBirth),
          restrictedDetails: {
            ...data.restrictedDetails,
            endDate: LocalDate.parseNullableIso(child.restrictedDetails.endDate)
          }
        }))
      }))
    )
    .then(Success)
    .catch(Failure)
}

export async function getPersonGuardians(
  personId: UUID
): Promise<Result<PersonDetails[]>> {
  return client
    .get<JsonOf<PersonDetails[]>>(`/person/guardians/${personId}`)
    .then((res) => Success(res.data.map(deserializePersonDetails)))
    .catch(Failure)
}

interface DecisionsResponse {
  decisions: Decision[]
}

export async function getGuardianDecisions(
  guardianId: UUID
): Promise<Result<Decision[]>> {
  return client
    .get<JsonOf<DecisionsResponse>>(`/decisions2/by-guardian?id=${guardianId}`)
    .then((res) =>
      res.data.decisions.map((data) => ({
        ...data,
        startDate: LocalDate.parseIso(data.startDate),
        endDate: LocalDate.parseIso(data.endDate),
        sentDate: LocalDate.parseIso(data.sentDate)
      }))
    )
    .then(Success)
    .catch(Failure)
}

export interface PatchPersonRequest {
  firstName: string | undefined
  lastName: string | undefined
  dateOfBirth: LocalDate | undefined
  email: string | undefined
  phone: string | undefined
  streetAddress: string | undefined
  postalCode: string | undefined
  postOffice: string | undefined
}

export async function mergePeople(
  master: UUID,
  duplicate: UUID
): Promise<Result<null>> {
  return client
    .post('/person/merge', { master, duplicate })
    .then(() => Success(null))
    .catch(Failure)
}

export async function deletePerson(id: UUID): Promise<Result<null>> {
  return client
    .delete(`/person/${id}`)
    .then(() => Success(null))
    .catch(Failure)
}

export type CreatePersonBody = {
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  streetAddress: string
  postalCode: string
  postOffice: string
  phone?: string
  email?: string
}

export async function createPerson(
  person: CreatePersonBody
): Promise<Result<string>> {
  return client
    .post('person/create', person)
    .then((response) => Success(response.data))
    .catch(Failure)
}
