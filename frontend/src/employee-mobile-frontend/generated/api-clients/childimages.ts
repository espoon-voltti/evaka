// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
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
