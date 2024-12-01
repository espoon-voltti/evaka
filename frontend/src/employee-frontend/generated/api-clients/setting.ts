// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { SettingType } from 'lib-common/generated/api-types/setting'
import { client } from '../../api/client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.setting.SettingController.getSettings
*/
export async function getSettings(
  headers?: AxiosHeaders
): Promise<Record<SettingType, string>> {
  const { data: json } = await client.request<JsonOf<Record<SettingType, string>>>({
    url: uri`/employee/settings`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.setting.SettingController.putSettings
*/
export async function putSettings(
  request: {
    body: Record<SettingType, string>
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/settings`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<Record<SettingType, string>>
  })
  return json
}
