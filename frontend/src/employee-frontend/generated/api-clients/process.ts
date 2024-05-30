// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildDocumentMetadata } from 'lib-common/generated/api-types/process'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonChildDocumentMetadata } from 'lib-common/generated/api-types/process'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.getChildDocumentMetadata
*/
export async function getChildDocumentMetadata(
  request: {
    childDocumentId: UUID
  }
): Promise<ChildDocumentMetadata | null> {
  const { data: json } = await client.request<JsonOf<ChildDocumentMetadata | null>>({
    url: uri`/employee/process-metadata/child-documents/${request.childDocumentId}`.toString(),
    method: 'GET'
  })
  return (json != null) ? deserializeJsonChildDocumentMetadata(json) : null
}
