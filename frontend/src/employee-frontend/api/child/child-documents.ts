import DateRange from 'lib-common/date-range'
import {
  ChildDocument,
  ChildDocumentCreateRequest
} from 'lib-common/generated/api-types/document'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getChildDocuments(
  childId: UUID
): Promise<ChildDocument[]> {
  return client
    .get<JsonOf<ChildDocument[]>>('/child-documents', { params: { childId } })
    .then((res) =>
      res.data.map((document) => ({
        ...document,
        template: {
          ...document.template,
          validity: DateRange.parseJson(document.template.validity)
        }
      }))
    )
}

export async function postChildDocument(
  body: ChildDocumentCreateRequest
): Promise<UUID> {
  return client
    .post<JsonOf<UUID>>('/child-documents', body)
    .then((res) => res.data)
}
