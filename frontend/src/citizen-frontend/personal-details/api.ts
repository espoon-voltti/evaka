// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  EmailNotificationSettings,
  PersonalDataUpdate
} from 'lib-common/generated/api-types/pis'
import { JsonOf } from 'lib-common/json'

import { client } from '../api-client'

export function updatePersonalData(
  data: PersonalDataUpdate
): Promise<Result<void>> {
  return client
    .put('/citizen/personal-data', data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getNotificationSettings(): Promise<EmailNotificationSettings> {
  return client
    .get<
      JsonOf<EmailNotificationSettings>
    >('/citizen/personal-data/notification-settings')
    .then((res) => res.data)
}

export function updateNotificationSettings(
  data: EmailNotificationSettings
): Promise<void> {
  return client
    .put('/citizen/personal-data/notification-settings', data)
    .then(() => undefined)
}
