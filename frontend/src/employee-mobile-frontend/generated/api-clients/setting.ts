// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { SettingType } from 'lib-common/generated/api-types/setting'
import { client } from '../../client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.setting.SettingController.getSettings
*/
export async function getSettings(): Promise<Record<SettingType, string>> {
  const { data: json } = await client.request<JsonOf<Record<SettingType, string>>>({
    url: uri`/settings`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.setting.SettingController.setSettings
*/
export async function setSettings(
  request: {
    body: Record<SettingType, string>
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/settings`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<Record<SettingType, string>>
  })
  return json
}
