// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { getUnitInfo, getUnitStats } from '../generated/api-clients/attendance'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('units', {
  info: (unitId: UUID) => ['info', unitId],
  stats: (unitIds: UUID[]) => ['stats', unitIds]
})

export const unitStatsQuery = query({
  api: getUnitStats,
  queryKey: ({ unitIds }) => queryKeys.stats(unitIds)
})

export const unitInfoQuery = query({
  api: getUnitInfo,
  queryKey: ({ unitId }) => queryKeys.info(unitId),
  options: {
    staleTime: 5 * 60 * 1000
  }
})
