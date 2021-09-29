import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'

export async function getPedagogicalDocuments(): Promise<
  Result<PedagogicalDocument[]>
> {
  return client
    .get<JsonOf<PedagogicalDocument[]>>('/pedagogical-document/citizen/child/')
    .then((res) => res.data.map(deserializePedagogicalDocument))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function deserializePedagogicalDocument(
  data: JsonOf<PedagogicalDocument>
): PedagogicalDocument {
  const created = new Date(data.created)
  const updated = new Date(data.updated)
  return { ...data, created, updated }
}
