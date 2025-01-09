// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { EmailMessageType } from 'lib-common/generated/api-types/pis'
import { EmailVerificationRequest } from 'lib-common/generated/api-types/pis'
import { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PasswordConstraints } from 'lib-common/generated/api-types/shared'
import { PersonalDataUpdate } from 'lib-common/generated/api-types/pis'
import { UpdateWeakLoginCredentialsRequest } from 'lib-common/generated/api-types/pis'
import { client } from '../../api-client'
import { deserializeJsonEmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.getEmailVerificationStatus
*/
export async function getEmailVerificationStatus(): Promise<EmailVerificationStatusResponse> {
  const { data: json } = await client.request<JsonOf<EmailVerificationStatusResponse>>({
    url: uri`/citizen/personal-data/email-verification`.toString(),
    method: 'GET'
  })
  return deserializeJsonEmailVerificationStatusResponse(json)
}


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
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.getPasswordConstraints
*/
export async function getPasswordConstraints(): Promise<PasswordConstraints> {
  const { data: json } = await client.request<JsonOf<PasswordConstraints>>({
    url: uri`/citizen/personal-data/password-constraints`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.sendEmailVerificationCode
*/
export async function sendEmailVerificationCode(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/email-verification-code`.toString(),
    method: 'POST'
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


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.updateWeakLoginCredentials
*/
export async function updateWeakLoginCredentials(
  request: {
    body: UpdateWeakLoginCredentialsRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/weak-login-credentials`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpdateWeakLoginCredentialsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.verifyEmail
*/
export async function verifyEmail(
  request: {
    body: EmailVerificationRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/email-verification`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<EmailVerificationRequest>
  })
  return json
}
