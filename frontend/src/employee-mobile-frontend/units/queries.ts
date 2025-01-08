// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getUnitInfo, getUnitStats } from '../generated/api-clients/attendance'

const q = new Queries()

export const unitStatsQuery = q.query(getUnitStats)

export const unitInfoQuery = q.query(getUnitInfo, {
  staleTime: 5 * 60 * 1000
})
