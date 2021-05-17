// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api/client'
import { UUID } from '../../types'
import {
  deserializeMessageThread,
  deserializeReceiverChild,
  IdAndName,
  MessageAccount,
  MessageThread,
  MessageType,
  ReceiverGroup,
  ReceiverTriplet
} from './types'

export async function deleteDraftBulletin(id: UUID): Promise<void> {
  return client.delete(`/bulletins/${id}`)
}

export async function updateDraftBulletin(
  id: UUID,
  receivers: ReceiverTriplet[] | null,
  title: string,
  content: string,
  sender: string
): Promise<void> {
  return client.put(`/bulletins/${id}`, {
    receivers,
    title,
    content,
    sender
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
    .catch((e) => Failure.fromError(e))
}

export async function getReceivers(
  unitId: UUID
): Promise<Result<ReceiverGroup[]>> {
  return client
    .get<JsonOf<ReceiverGroup[]>>('/bulletins/receivers', {
      params: { unitId }
    })
    .then((res) =>
      Success.of(
        res.data.map((receiverGroup) => ({
          ...receiverGroup,
          receivers: receiverGroup.receivers.map(deserializeReceiverChild)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getSenderOptions(
  unitId: UUID
): Promise<Result<string[]>> {
  return client
    .get<JsonOf<string[]>>('/bulletins/sender-options', {
      params: { unitId }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getMessagingAccounts(): Promise<
  Result<MessageAccount[]>
> {
  return client
    .get<JsonOf<MessageAccount[]>>('/messages/my-accounts')
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function getReceivedMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<Result<Paged<MessageThread>>> {
  return client
    .get<JsonOf<Paged<MessageThread>>>(`/messages/${accountId}/received`, {
      params: { page, pageSize }
    })
    .then(({ data }) =>
      Success.of({
        ...data,
        data: data.data.map(deserializeMessageThread)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function createNewMessage(
  title: string,
  content: string,
  type: MessageType,
  senderAccountId: UUID,
  recipientAccountIds: Set<UUID>
): Promise<Result<void>> {
  return client
    .post(`/messages`, {
      title,
      content,
      type,
      senderAccountId,
      recipientAccountIds
    })
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function replyToThread(
  messageId: UUID,
  content: string,
  senderAccountId: UUID,
  recipientAccountIds: Set<UUID>
): Promise<Result<void>> {
  return client
    .post(`/messages/${messageId}/reply`, {
      content,
      senderAccountId,
      recipientAccountIds
    })
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function markThreadRead(accountId: UUID, id: UUID): Promise<void> {
  return client.put(`/messages/${accountId}/threads/${id}/read`)
}
