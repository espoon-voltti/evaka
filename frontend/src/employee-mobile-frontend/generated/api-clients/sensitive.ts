// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ChildBasicInformation } from 'lib-common/generated/api-types/sensitive'
import type { ChildSensitiveInformation } from 'lib-common/generated/api-types/sensitive'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../client'
import { deserializeJsonChildBasicInformation } from 'lib-common/generated/api-types/sensitive'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.sensitive.ChildInfoController.getBasicInfo
*/
export async function getBasicInfo(
  request: {
    childId: PersonId
  }
): Promise<ChildBasicInformation> {
  const { data: json } = await client.request<JsonOf<ChildBasicInformation>>({
    url: uri`/employee-mobile/children/${request.childId}/basic-info`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildBasicInformation(json)
}


/**
* Generated from fi.espoo.evaka.sensitive.ChildInfoController.getSensitiveInfo
*/
export async function getSensitiveInfo(
  request: {
    childId: PersonId
  }
): Promise<ChildSensitiveInformation> {
  const { data: json } = await client.request<JsonOf<ChildSensitiveInformation>>({
    url: uri`/employee-mobile/children/${request.childId}/sensitive-info`.toString(),
    method: 'GET'
  })
  return json
}
