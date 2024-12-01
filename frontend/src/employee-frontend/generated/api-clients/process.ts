// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { JsonOf } from 'lib-common/json'
import { ProcessMetadataResponse } from 'lib-common/generated/api-types/process'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonProcessMetadataResponse } from 'lib-common/generated/api-types/process'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getApplicationMetadata
*/
export async function getApplicationMetadata(
  request: {
    applicationId: UUID
  },
  headers?: AxiosHeaders
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/applications/${request.applicationId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getAssistanceNeedDecisionMetadata
*/
export async function getAssistanceNeedDecisionMetadata(
  request: {
    decisionId: UUID
  },
  headers?: AxiosHeaders
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/assistance-need-decisions/${request.decisionId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getAssistanceNeedPreschoolDecisionMetadata
*/
export async function getAssistanceNeedPreschoolDecisionMetadata(
  request: {
    decisionId: UUID
  },
  headers?: AxiosHeaders
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/assistance-need-preschool-decisions/${request.decisionId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getChildDocumentMetadata
*/
export async function getChildDocumentMetadata(
  request: {
    childDocumentId: UUID
  },
  headers?: AxiosHeaders
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/child-documents/${request.childDocumentId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonProcessMetadataResponse(json)
}
