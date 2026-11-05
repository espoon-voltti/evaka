// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { EmailVerificationRequest } from 'lib-common/generated/api-types/person'
import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/person'
import type { FamilyMembers } from 'lib-common/generated/api-types/family'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { NotificationSettings } from 'lib-common/generated/api-types/person'
import type { PasswordConstraints } from 'lib-common/generated/api-types/shared'
import type { PersonalDataUpdate } from 'lib-common/generated/api-types/person'
import type { UpdatePreferredUiLanguageRequest } from 'lib-common/generated/api-types/person'
import type { UpdateWeakLoginCredentialsRequest } from 'lib-common/generated/api-types/person'
import { client } from '../../api-client'
import { deserializeJsonEmailVerificationStatusResponse } from 'lib-common/generated/api-types/person'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.deleteWeakLoginCredentials
*/
export async function deleteWeakLoginCredentials(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/weak-login-credentials`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.getEmailVerificationStatus
*/
export async function getEmailVerificationStatus(): Promise<EmailVerificationStatusResponse> {
  const { data: json } = await client.request<JsonOf<EmailVerificationStatusResponse>>({
    url: uri`/citizen/personal-data/email-verification`.toString(),
    method: 'GET'
  })
  return deserializeJsonEmailVerificationStatusResponse(json)
}


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.getFamily
*/
export async function getFamily(): Promise<FamilyMembers> {
  const { data: json } = await client.request<JsonOf<FamilyMembers>>({
    url: uri`/citizen/personal-data/family`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.getNotificationSettings
*/
export async function getNotificationSettings(): Promise<NotificationSettings> {
  const { data: json } = await client.request<JsonOf<NotificationSettings>>({
    url: uri`/citizen/personal-data/notification-settings`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.getPasswordConstraints
*/
export async function getPasswordConstraints(): Promise<PasswordConstraints> {
  const { data: json } = await client.request<JsonOf<PasswordConstraints>>({
    url: uri`/citizen/personal-data/password-constraints`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.sendEmailVerificationCode
*/
export async function sendEmailVerificationCode(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/email-verification-code`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.updateNotificationSettings
*/
export async function updateNotificationSettings(
  request: {
    body: NotificationSettings
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/notification-settings`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<NotificationSettings>
  })
  return json
}


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.updatePersonalData
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
* Generated from evaka.core.person.PersonalDataControllerCitizen.updatePreferredUiLanguage
*/
export async function updatePreferredUiLanguage(
  request: {
    body: UpdatePreferredUiLanguageRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/personal-data/preferred-ui-language`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpdatePreferredUiLanguageRequest>
  })
  return json
}


/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.updateWeakLoginCredentials
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
* Generated from evaka.core.person.PersonalDataControllerCitizen.verifyEmail
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
