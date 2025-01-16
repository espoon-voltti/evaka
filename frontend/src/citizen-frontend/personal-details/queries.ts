// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getEmailVerificationStatus,
  getNotificationSettings,
  getPasswordConstraints,
  sendEmailVerificationCode,
  updateNotificationSettings,
  updatePersonalData,
  updateWeakLoginCredentials,
  verifyEmail
} from '../generated/api-clients/pis'

const q = new Queries()

export const emailVerificationStatusQuery = q.query(getEmailVerificationStatus)

export const updatePersonalDetailsMutation = q.mutation(updatePersonalData, [
  emailVerificationStatusQuery
])

export const updateWeakLoginCredentialsMutation = q.mutation(
  updateWeakLoginCredentials
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
