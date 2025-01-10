// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getPushSettings,
  setPushSettings
} from '../generated/api-clients/webpush'

const q = new Queries()

export const pushSettingsQuery = q.query(getPushSettings)

export const pushSettingsMutation = q.mutation(setPushSettings, [
  pushSettingsQuery
])
