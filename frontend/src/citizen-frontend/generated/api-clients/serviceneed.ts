// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonOf } from 'lib-common/json'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.getServiceNeedOptionPublicInfos
*/
export async function getServiceNeedOptionPublicInfos(
  request: {
    placementTypes?: PlacementType[] | null
  }
): Promise<ServiceNeedOptionPublicInfo[]> {
  const params = createUrlSearchParams(
    ...(request.placementTypes?.map((e): [string, string | null | undefined] => ['placementTypes', e.toString()]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<ServiceNeedOptionPublicInfo[]>>({
    url: uri`/public/service-needs/options`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonServiceNeedOptionPublicInfo(e))
}
