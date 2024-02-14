// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import { ChildSensitiveInformation } from 'lib-common/generated/api-types/sensitive'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { deserializeJsonChildSensitiveInformation } from 'lib-common/generated/api-types/sensitive'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.sensitive.ChildSensitiveInfoController.getSensitiveInfo
*/
export async function getSensitiveInfo(
  request: {
    childId: UUID
  }
): Promise<ChildSensitiveInformation> {
  const { data: json } = await client.request<JsonOf<ChildSensitiveInformation>>({
    url: uri`/children/${request.childId}/sensitive-info`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildSensitiveInformation(json)
}
