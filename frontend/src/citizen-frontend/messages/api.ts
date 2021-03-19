// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { client } from '../api-client'
import {
  deserializeReceivedBulletin,
  ReceivedBulletin
} from '../messages/types'

export async function getBulletins(
  page: number,
  pageSize = 50
): Promise<Result<Paged<ReceivedBulletin>>> {
  return client
    .get<JsonOf<Paged<ReceivedBulletin>>>('/citizen/bulletins', {
      params: { page, pageSize }
    })
    .then((res) =>
      Success.of({
        ...res.data,
        data: res.data.data.map(deserializeReceivedBulletin)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function markBulletinRead(id: string): Promise<void> {
  return client.put(`/citizen/bulletins/${id}/read`)
}

export async function getUnreadBulletinsCount(): Promise<Result<number>> {
  return client
    .get(`/citizen/bulletins/unread/`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
