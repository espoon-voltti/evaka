// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosProgressEvent } from 'axios'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../client'
import { createFormData } from 'lib-common/api'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.childimages.ChildImageController.deleteImage
*/
export async function deleteImage(
  request: {
    childId: PersonId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/children/${request.childId}/image`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.childimages.ChildImageController.putImage
*/
export async function putImage(
  request: {
    childId: PersonId,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<void> {
  const data = createFormData(
    ['file', request.file]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/children/${request.childId}/image`.toString(),
    method: 'PUT',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    data
  })
  return json
}
