// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonOf } from 'lib-common/json'
import { ProcessMetadataResponse } from 'lib-common/generated/api-types/process'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonProcessMetadataResponse } from 'lib-common/generated/api-types/process'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getAssistanceNeedDecisionMetadata
*/
export async function getAssistanceNeedDecisionMetadata(
  request: {
    decisionId: UUID
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/assistance-need-decisions/${request.decisionId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getAssistanceNeedPreschoolDecisionMetadata
*/
export async function getAssistanceNeedPreschoolDecisionMetadata(
  request: {
    decisionId: UUID
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/assistance-need-preschool-decisions/${request.decisionId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getChildDocumentMetadata
*/
export async function getChildDocumentMetadata(
  request: {
    childDocumentId: UUID
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/child-documents/${request.childDocumentId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}
