// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { client } from '../api-client'
import { deserializeMessageThread, MessageThread } from './types'

export async function getReceivedMessages(
  page: number,
  pageSize = 20
): Promise<Result<Paged<MessageThread>>> {
  return client
    .get<JsonOf<Paged<MessageThread>>>('/citizen/messages/received', {
      params: { page, pageSize }
    })
    .then((res) =>
      Success.of({
        ...res.data,
        data: res.data.data.map(deserializeMessageThread)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function markThreadRead(id: string): Promise<void> {
  return client.put(`/citizen/messages/threads/${id}/read`)
}

export async function getUnreadMessagesCount(): Promise<Result<number>> {
  return client
    .get(`/citizen/messages/unread-count`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
