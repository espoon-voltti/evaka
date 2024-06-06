// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildDocumentMetadataResponse } from 'lib-common/generated/api-types/process'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonChildDocumentMetadataResponse } from 'lib-common/generated/api-types/process'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getChildDocumentMetadata
*/
export async function getChildDocumentMetadata(
  request: {
    childDocumentId: UUID
  }
): Promise<ChildDocumentMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ChildDocumentMetadataResponse>>({
    url: uri`/employee/process-metadata/child-documents/${request.childDocumentId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildDocumentMetadataResponse(json)
}
