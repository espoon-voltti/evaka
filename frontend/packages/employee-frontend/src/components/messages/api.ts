// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { JsonOf } from '@evaka/lib-common/src/json'
import { client } from '~api/client'
import { UUID } from '~types'
import {
  Bulletin,
  IdAndName,
  deserializeBulletin,
  SentBulletin,
  deserializeSentBulletin
} from './types'

export async function initNewBulletin(): Promise<Result<Bulletin>> {
  return client
    .post<JsonOf<Bulletin>>('/bulletins')
    .then((res) => Success.of(deserializeBulletin(res.data)))
    .catch((e) => Failure.fromError(e))
}

export async function deleteDraftBulletin(id: UUID): Promise<void> {
  return client.delete(`/bulletins/${id}`)
}

export async function updateDraftBulletin(
  id: UUID,
  groupId: UUID | null,
  title: string,
  content: string
): Promise<void> {
  return client.put(`/bulletins/${id}`, {
    groupId,
    title,
    content
  })
}

export async function sendBulletin(id: UUID): Promise<void> {
  return client.post(`/bulletins/${id}/send`)
}

export async function getUnits(): Promise<Result<IdAndName[]>> {
  return client
    .get<JsonOf<IdAndName[]>>('/daycares')
    .then((res) => Success.of(res.data.map(({ id, name }) => ({ id, name }))))
    .catch((e) => Failure.fromError(e))
}

export async function getGroups(unitId: UUID): Promise<Result<IdAndName[]>> {
  return client
    .get<JsonOf<IdAndName[]>>(`/daycares/${unitId}/groups`)
    .then((res) => Success.of(res.data.map(({ id, name }) => ({ id, name }))))
}

export async function getSentBulletins(
  unitId: UUID
): Promise<Result<SentBulletin[]>> {
  return client
    .get<JsonOf<SentBulletin[]>>('/bulletins/sent', { params: { unitId } })
    .then((res) => Success.of(res.data.map(deserializeSentBulletin)))
    .catch((e) => Failure.fromError(e))
}

export async function getDraftBulletins(
  unitId: UUID
): Promise<Result<Bulletin[]>> {
  return client
    .get<JsonOf<Bulletin[]>>('/bulletins/draft', { params: { unitId } })
    .then((res) => Success.of(res.data.map(deserializeBulletin)))
    .catch((e) => Failure.fromError(e))
}
