// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  deserializePersonDetails,
  PersonContactInfo,
  PersonDetails,
  PersonWithChildren,
  SearchColumn
} from '../types/person'
import { SearchOrder, UUID } from '../types'
import { client } from '../api/client'
import { ApplicationSummary } from '../types/application'
import { Decision } from '../types/decision'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { Recipient } from '../../employee-frontend/components/messages/types'

export async function getPersonDetails(
  id: UUID
): Promise<Result<PersonDetails>> {
  return client
    .get<JsonOf<PersonDetails>>(`/person/details/${id}`)
    .then((res) => res.data)
    .then(deserializePersonDetails)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
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
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function patchPersonDetails(
  id: UUID,
  data: PatchPersonRequest
): Promise<Result<PersonDetails>> {
  return client
    .patch<JsonOf<PersonDetails>>(`/person/${id}`, data)
    .then(({ data }) => deserializePersonDetails(data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function addSsn(
  id: UUID,
  ssn: string
): Promise<Result<PersonDetails>> {
  return client
    .put<JsonOf<PersonDetails>>(`/person/${id}/ssn`, { ssn })
    .then(({ data }) => deserializePersonDetails(data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
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
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
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
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
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
        preferredStartDate: LocalDate.parseIso(data.preferredStartDate),
        sentDate: LocalDate.parseNullableIso(data.sentDate)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getChildApplicationSummaries(
  childId: UUID
): Promise<Result<ApplicationSummary[]>> {
  return client
    .get<JsonOf<ApplicationSummary[]>>(`/v2/applications/by-child/${childId}`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        preferredStartDate: LocalDate.parseIso(data.preferredStartDate),
        sentDate: LocalDate.parseNullableIso(data.sentDate)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getChildRecipients(
  childId: UUID
): Promise<Result<Recipient[]>> {
  return client
    .get<JsonOf<Recipient[]>>(`/child/${childId}/recipients`)
    .then((v) => Success.of(v.data))
    .catch((e) => Failure.fromError(e))
}

export async function updateChildRecipient(
  childId: UUID,
  recipientId: UUID,
  blocklisted: boolean
): Promise<Result<void>> {
  return client
    .put<void>(`/child/${childId}/recipients/${recipientId}`, {
      blocklisted: blocklisted
    })
    .then((v) => Success.of(v.data))
    .catch((e) => Failure.fromError(e))
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
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getPersonGuardians(
  personId: UUID
): Promise<Result<PersonDetails[]>> {
  return client
    .get<JsonOf<PersonDetails[]>>(`/person/guardians/${personId}`)
    .then((res) => Success.of(res.data.map(deserializePersonDetails)))
    .catch((e) => Failure.fromError(e))
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
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
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
  backupPhone: string | undefined
}

export async function mergePeople(
  master: UUID,
  duplicate: UUID
): Promise<Result<null>> {
  return client
    .post('/person/merge', { master, duplicate })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deletePerson(id: UUID): Promise<Result<null>> {
  return client
    .delete(`/person/${id}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
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
    .then((response) => Success.of(response.data))
    .catch((e) => Failure.fromError(e))
}
