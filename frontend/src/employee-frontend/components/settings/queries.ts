// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  getSettings,
  putSettings
} from 'employee-frontend/generated/api-clients/setting'
import { Queries } from 'lib-common/query'

const q = new Queries()

export const settingsQuery = q.query(getSettings)

export const putSettingsMutation = q.mutation(putSettings, [
  settingsQuery.prefix
])
