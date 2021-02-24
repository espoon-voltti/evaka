// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { JsonOf } from '@evaka/lib-common/src/json'
import { client } from '~api-client'
import { deserializeReceivedBulletin, ReceivedBulletin } from '~messages/types'

export async function getBulletins(): Promise<Result<ReceivedBulletin[]>> {
  return client
    .get<JsonOf<ReceivedBulletin[]>>('/citizen/bulletins')
    .then((res) => Success.of(res.data.map(deserializeReceivedBulletin)))
    .catch((e) => Failure.fromError(e))
}

export async function markBulletinRead(id: string): Promise<void> {
  return client.put(`/citizen/bulletins/${id}/read`)
}
