// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { EmailMessageType } from 'lib-common/generated/api-types/pis'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonalDataUpdate } from 'lib-common/generated/api-types/pis'
import { client } from '../../api-client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.getNotificationSettings
*/
export async function getNotificationSettings(): Promise<EmailMessageType[]> {
  const { data: json } = await client.request<JsonOf<EmailMessageType[]>>({
    url: uri`/citizen/personal-data/notification-settings`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.updateNotificationSettings
*/
export async function updateNotificationSettings(
  request: {
    body: EmailMessageType[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/notification-settings`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<EmailMessageType[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.updatePersonalData
*/
export async function updatePersonalData(
  request: {
    body: PersonalDataUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PersonalDataUpdate>
  })
  return json
}
