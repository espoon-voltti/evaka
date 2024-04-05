// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'

import {
  createMessagePreflightCheck,
  replyToThread
} from '../../generated/api-clients/messaging'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('messaging', {
  preflight: (arg: Arg0<typeof createMessagePreflightCheck>) => [
    'preflight',
    arg
  ]
})

export const createMessagePreflightCheckQuery = query({
  api: createMessagePreflightCheck,
  queryKey: queryKeys.preflight
})

export const replyToThreadMutation = mutation({
  api: replyToThread
})
