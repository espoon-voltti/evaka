// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getSettings, putSettings } from '../../generated/api-clients/setting'

const q = new Queries()

export const settingsQuery = q.query(getSettings)

export const putSettingsMutation = q.mutation(putSettings, [
  settingsQuery.prefix
])
