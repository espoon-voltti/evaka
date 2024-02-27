// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { getTimeline } from '../../generated/api-clients/timeline'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('timeline', {
  byAdult: (personId: UUID) => [personId]
})

export const timelineQuery = query({
  api: getTimeline,
  queryKey: (arg) => queryKeys.byAdult(arg.personId)
})
