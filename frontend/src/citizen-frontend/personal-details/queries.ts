// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  deletePasskey,
  deleteWeakLoginCredentials,
  finishPasskeyRegistration,
  getEmailVerificationStatus,
  getFamily,
  getNotificationSettings,
  getPasskeys,
  getPasswordConstraints,
  sendEmailVerificationCode,
  updateNotificationSettings,
  updatePasskeyName,
  updatePersonalData,
  updateWeakLoginCredentials,
  verifyEmail
} from '../generated/api-clients/pis'

const q = new Queries()

export const emailVerificationStatusQuery = q.query(getEmailVerificationStatus)

export const familyQuery = q.query(getFamily)

export const updatePersonalDetailsMutation = q.mutation(updatePersonalData, [
  emailVerificationStatusQuery
])

export const updateWeakLoginCredentialsMutation = q.mutation(
  updateWeakLoginCredentials
)

export const deleteWeakLoginCredentialsMutation = q.mutation(
  deleteWeakLoginCredentials
)

export const notificationSettingsQuery = q.query(getNotificationSettings)

export const updateNotificationSettingsMutation = q.mutation(
  updateNotificationSettings,
  [notificationSettingsQuery]
)

export const sendEmailVerificationCodeMutation = q.mutation(
  sendEmailVerificationCode,
  [emailVerificationStatusQuery]
)

export const verifyEmailMutation = q.mutation(verifyEmail, [
  emailVerificationStatusQuery
])

export const passwordConstraintsQuery = q.query(getPasswordConstraints)

export const passkeysQuery = q.query(getPasskeys)

export const finishPasskeyRegistrationMutation = q.mutation(
  finishPasskeyRegistration,
  [passkeysQuery]
)

export const updatePasskeyNameMutation = q.mutation(updatePasskeyName, [
  passkeysQuery
])

export const deletePasskeyMutation = q.mutation(deletePasskey, [passkeysQuery])
