// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildImageId } from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { Uri } from 'lib-common/uri'
import { client } from '../../client'
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
* Generated from fi.espoo.evaka.childimages.ChildImageController.getImage
*/
export function getImage(
  request: {
    imageId: ChildImageId
  }
): { url: Uri } {
  return {
    url: uri`${client.defaults.baseURL ?? ''}/employee-mobile/child-images/${request.imageId}`
  }
}
