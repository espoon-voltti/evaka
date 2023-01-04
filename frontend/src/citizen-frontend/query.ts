// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { QueryClient } from '@tanstack/react-query'

import { queryKeysNamespace } from 'lib-common/query'

export type QueryKeyPrefix =
  | 'applications'
  | 'applicationDecisions'
  | 'assistanceDecisions'
  | 'calendar'
  | 'childConsents'
  | 'children'
  | 'holidayPeriods'
  | 'pedagogicalDocuments'
  | 'vasuAndLeops'

export const queryClient = new QueryClient()
export { QueryClientProvider } from '@tanstack/react-query'

export const createQueryKeys = queryKeysNamespace<QueryKeyPrefix>()
