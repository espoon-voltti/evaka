// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildDocumentCitizenDetails } from 'lib-common/generated/api-types/document'
import { ChildDocumentCitizenSummary } from 'lib-common/generated/api-types/document'
import { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
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
    documentId: ChildDocumentId
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
    childId: PersonId
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
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentControllerCitizen.getUnreadDocumentsCount
*/
export async function getUnreadDocumentsCount(): Promise<Partial<Record<PersonId, number>>> {
  const { data: json } = await client.request<JsonOf<Partial<Record<PersonId, number>>>>({
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
    documentId: ChildDocumentId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/child-documents/${request.documentId}/read`.toString(),
    method: 'PUT'
  })
  return json
}
