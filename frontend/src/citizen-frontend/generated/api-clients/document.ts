// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildDocumentCitizenDetails } from 'lib-common/generated/api-types/document'
import { ChildDocumentCitizenSummary } from 'lib-common/generated/api-types/document'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonChildDocumentCitizenDetails } from 'lib-common/generated/api-types/document'
import { deserializeJsonChildDocumentCitizenSummary } from 'lib-common/generated/api-types/document'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentControllerCitizen.getDocument
*/
export async function getDocument(
  request: {
    documentId: UUID
  }
): Promise<ChildDocumentCitizenDetails> {
  const { data: json } = await client.request<JsonOf<ChildDocumentCitizenDetails>>({
    url: uri`/citizen/child-documents/${request.documentId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildDocumentCitizenDetails(json)
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentControllerCitizen.getDocuments
*/
export async function getDocuments(
  request: {
    childId: UUID
  }
): Promise<ChildDocumentCitizenSummary[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<ChildDocumentCitizenSummary[]>>({
    url: uri`/citizen/child-documents`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonChildDocumentCitizenSummary(e))
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentControllerCitizen.getUnreadCount
*/
export async function getUnreadCount(): Promise<Record<UUID, number>> {
  const { data: json } = await client.request<JsonOf<Record<UUID, number>>>({
    url: uri`/citizen/child-documents/unread-count`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentControllerCitizen.putDocumentRead
*/
export async function putDocumentRead(
  request: {
    documentId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/child-documents/${request.documentId}/read`.toString(),
    method: 'PUT'
  })
  return json
}
