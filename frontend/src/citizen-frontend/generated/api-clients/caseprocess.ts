// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import type { ProcessMetadataResponse } from 'lib-common/generated/api-types/caseprocess'
import { client } from '../../api-client'
import { deserializeJsonProcessMetadataResponse } from 'lib-common/generated/api-types/caseprocess'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadataControllerCitizen.getApplicationMetadata
*/
export async function getApplicationMetadata(
  request: {
    applicationId: ApplicationId
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/citizen/process-metadata/applications/${request.applicationId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}
