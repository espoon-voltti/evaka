import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { deserializeReceivedBulletin, ReceivedBulletin } from '~messages/types'
import { client } from '~api-client'
import { JsonOf } from '@evaka/lib-common/src/json'

export async function getBulletins(): Promise<Result<ReceivedBulletin[]>> {
  return client
    .get<JsonOf<ReceivedBulletin[]>>('/citizen/bulletins')
    .then((res) => Success.of(res.data.map(deserializeReceivedBulletin)))
    .catch((e) => Failure.fromError(e))
}

export function markBulletinRead(id: string) {
  void client.put(`/citizen/bulletins/${id}/read`)
}
