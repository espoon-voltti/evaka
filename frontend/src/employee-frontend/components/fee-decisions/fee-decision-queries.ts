// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'

import { getFeeDecisionMetadata } from '../../generated/api-clients/process'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('feeDecisions', {
  feeDecisionMetadata: (args: Arg0<typeof getFeeDecisionMetadata>) => [
    'feeDecisionMetadata',
    args
  ]
})

export const feeDecisionMetadataQuery = query({
  api: getFeeDecisionMetadata,
  queryKey: queryKeys.feeDecisionMetadata
})
