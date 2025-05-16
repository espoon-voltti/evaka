// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { SettingType } from 'lib-common/generated/api-types/setting'
import { client } from '../../api/client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.setting.SettingController.getSettings
*/
export async function getSettings(): Promise<Partial<Record<SettingType, string>>> {
  const { data: json } = await client.request<JsonOf<Partial<Record<SettingType, string>>>>({
    url: uri`/employee/settings`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.setting.SettingController.putSettings
*/
export async function putSettings(
  request: {
    body: Partial<Record<SettingType, string>>
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/settings`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<Partial<Record<SettingType, string>>>
  })
  return json
}
