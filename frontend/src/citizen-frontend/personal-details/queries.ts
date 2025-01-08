// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getNotificationSettings,
  updateNotificationSettings,
  updatePassword,
  updatePersonalData
} from '../generated/api-clients/pis'

const q = new Queries()

export const updatePersonalDetailsMutation = q.mutation(updatePersonalData)

export const updatePasswordMutation = q.mutation(updatePassword)

export const notificationSettingsQuery = q.query(getNotificationSettings)

export const updateNotificationSettingsMutation = q.mutation(
  updateNotificationSettings,
  [() => notificationSettingsQuery()]
)
