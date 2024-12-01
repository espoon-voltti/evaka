// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { EmailMessageType } from 'lib-common/generated/api-types/pis'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonalDataUpdate } from 'lib-common/generated/api-types/pis'
import { UpdatePasswordRequest } from 'lib-common/generated/api-types/pis'
import { client } from '../../api-client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.getNotificationSettings
*/
export async function getNotificationSettings(
  headers?: AxiosHeaders
): Promise<EmailMessageType[]> {
  const { data: json } = await client.request<JsonOf<EmailMessageType[]>>({
    url: uri`/citizen/personal-data/notification-settings`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.updateNotificationSettings
*/
export async function updateNotificationSettings(
  request: {
    body: EmailMessageType[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/notification-settings`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<EmailMessageType[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.updatePassword
*/
export async function updatePassword(
  request: {
    body: UpdatePasswordRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/password`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<UpdatePasswordRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.updatePersonalData
*/
export async function updatePersonalData(
  request: {
    body: PersonalDataUpdate
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<PersonalDataUpdate>
  })
  return json
}
