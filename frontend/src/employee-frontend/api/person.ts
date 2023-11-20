// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { PersonApplicationSummary } from 'lib-common/generated/api-types/application'
import { ChildResponse } from 'lib-common/generated/api-types/daycare'
import { Decision } from 'lib-common/generated/api-types/decision'
import {
  EditRecipientRequest,
  Recipient
} from 'lib-common/generated/api-types/messaging'
import {
  CreatePersonBody,
  PersonJSON,
  PersonPatch,
  PersonResponse,
  PersonSummary,
  PersonWithChildrenDTO
} from 'lib-common/generated/api-types/pis'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { SearchOrder } from '../types'
import { deserializePersonJSON, SearchColumn } from '../types/person'

import { client } from './client'

export async function getPerson(id: UUID | null): Promise<Result<PersonJSON>> {
  if (id === null) {
    return Failure.of({ message: 'No person id given' })
  }
  return client
    .get<JsonOf<PersonJSON>>(`/person/identity/${id}`)
    .then((res) => res.data)
    .then(deserializePersonJSON)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getPersonDetails(
  id: UUID
): Promise<Result<PersonResponse>> {
  return client
    .get<JsonOf<PersonResponse>>(`/person/${id}`)
    .then((res) =>
      Success.of({
        person: deserializePersonJSON(res.data.person),
        permittedActions: res.data.permittedActions
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getChildDetails(
  id: UUID
): Promise<Result<ChildResponse>> {
  return client
    .get<JsonOf<ChildResponse>>(`/children/${id}`)
    .then(({ data }) => ({
      ...data,
      person: deserializePersonJSON(data.person)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function patchPersonDetails(
  id: UUID,
  data: PersonPatch
): Promise<Result<PersonJSON>> {
  return client
    .patch<JsonOf<PersonJSON>>(`/person/${id}`, data)
    .then(({ data }) => deserializePersonJSON(data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function addSsn(
  id: UUID,
  ssn: string
): Promise<Result<PersonJSON>> {
  return client
    .put<JsonOf<PersonJSON>>(`/person/${id}/ssn`, { ssn })
    .then(({ data }) => deserializePersonJSON(data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getOrCreatePersonBySsn(
  ssn: string,
  readonly = false
): Promise<Result<PersonJSON>> {
  return client
    .post<JsonOf<PersonJSON>>(`/person/details/ssn`, {
      ssn,
      readonly
    })
    .then(({ data }) => deserializePersonJSON(data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function findByNameOrAddress(
  searchTerm: string,
  orderBy: SearchColumn,
  sortDirection: SearchOrder
): Promise<Result<PersonSummary[]>> {
  return client
    .post<JsonOf<PersonSummary[]>>('/person/search', {
      searchTerm,
      orderBy,
      sortDirection
    })
    .then(({ data }) =>
      data.map((json) => ({
        ...json,
        dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
        dateOfDeath: LocalDate.parseNullableIso(json.dateOfDeath)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getGuardianApplicationSummaries(
  guardianId: UUID
): Promise<Result<PersonApplicationSummary[]>> {
  return client
    .get<JsonOf<PersonApplicationSummary[]>>(
      `/v2/applications/by-guardian/${guardianId}`
    )
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        preferredStartDate: data.preferredStartDate
          ? LocalDate.parseIso(data.preferredStartDate)
          : null,
        sentDate: LocalDate.parseNullableIso(data.sentDate)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getChildApplicationSummaries(
  childId: UUID
): Promise<Result<PersonApplicationSummary[]>> {
  return client
    .get<JsonOf<PersonApplicationSummary[]>>(
      `/v2/applications/by-child/${childId}`
    )
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        preferredStartDate: data.preferredStartDate
          ? LocalDate.parseIso(data.preferredStartDate)
          : null,
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
  data: EditRecipientRequest
): Promise<Result<void>> {
  return client
    .put<void>(`/child/${childId}/recipients/${recipientId}`, data)
    .then((v) => Success.of(v.data))
    .catch((e) => Failure.fromError(e))
}

export async function getPersonDependants(
  guardianId: UUID
): Promise<Result<PersonWithChildrenDTO[]>> {
  return client
    .get<JsonOf<PersonWithChildrenDTO[]>>(`/person/dependants/${guardianId}`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        dateOfBirth: LocalDate.parseIso(data.dateOfBirth),
        dateOfDeath: LocalDate.parseNullableIso(data.dateOfDeath),
        restrictedDetails: {
          ...data.restrictedDetails,
          endDate: LocalDate.parseNullableIso(data.restrictedDetails.endDate)
        },
        children: data.children.map((child) => ({
          ...child,
          dateOfBirth: LocalDate.parseIso(child.dateOfBirth),
          dateOfDeath: LocalDate.parseNullableIso(data.dateOfDeath),
          restrictedDetails: {
            ...data.restrictedDetails,
            endDate: LocalDate.parseNullableIso(child.restrictedDetails.endDate)
          },
          children: []
        }))
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getPersonGuardians(
  personId: UUID
): Promise<Result<PersonJSON[]>> {
  return client
    .get<JsonOf<PersonJSON[]>>(`/person/guardians/${personId}`)
    .then((res) => Success.of(res.data.map(deserializePersonJSON)))
    .catch((e) => Failure.fromError(e))
}

export interface GuardiansAndBlockedGuardians {
  blockedGuardians: PersonJSON[]
  guardians: PersonJSON[]
}

export async function getPersonBlockedGuardians(
  personId: UUID
): Promise<Result<PersonJSON[]>> {
  return client
    .get<JsonOf<PersonJSON[]>>(`/person/blocked-guardians/${personId}`)
    .then((res) => Success.of(res.data.map(deserializePersonJSON)))
    .catch((e) => Failure.fromError(e))
}

export async function getPersonGuardiansAndBlockedGuardians(
  personId: UUID,
  permittedActions: Set<Action.Child | Action.Person>
): Promise<Result<GuardiansAndBlockedGuardians>> {
  const [guardians, blockedGuardians] = await Promise.all([
    permittedActions.has('READ_GUARDIANS')
      ? getPersonGuardians(personId)
      : Success.of([]),
    permittedActions.has('READ_BLOCKED_GUARDIANS')
      ? getPersonBlockedGuardians(personId)
      : Success.of([])
  ])
  return guardians.isSuccess && blockedGuardians.isSuccess
    ? Success.of({
        guardians: guardians.value,
        blockedGuardians: blockedGuardians.value
      })
    : Failure.of(
        guardians.isFailure
          ? guardians
          : blockedGuardians.isFailure
            ? blockedGuardians
            : Failure.of({ message: 'Fetching guardians failed' })
      )
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
        sentDate: LocalDate.parseNullableIso(data.sentDate),
        requestedStartDate: LocalDate.parseNullableIso(data.requestedStartDate),
        resolved: LocalDate.parseNullableIso(data.resolved)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
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

export async function createPerson(
  person: CreatePersonBody
): Promise<Result<string>> {
  return client
    .post('person/create', person)
    .then((response) => Success.of(response.data))
    .catch((e) => Failure.fromError(e))
}

export function updateSsnAddingDisabled(
  personId: UUID,
  disabled: boolean
): Promise<Result<void>> {
  return client
    .put(`person/${personId}/ssn/disable`, { disabled })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function duplicatePerson(personId: UUID): Promise<Result<UUID>> {
  return client
    .post<UUID>(`/person/${personId}/duplicate`)
    .then((response) => Success.of(response.data))
    .catch((e) => Failure.fromError(e))
}

export async function updatePersonAndFamilyFromVtj(
  personId: UUID
): Promise<Result<PersonJSON>> {
  return client
    .post(`/person/${personId}/vtj-update`)
    .then(() => getPerson(personId))
    .catch((e) => Failure.fromError(e))
}

export async function updateEvakaRights(
  childId: UUID,
  guardianId: UUID,
  denied: boolean
): Promise<Result<void>> {
  return client
    .post(`/person/${childId}/evaka-rights`, { guardianId, denied })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
