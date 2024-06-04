// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { QueryClient } from '@tanstack/react-query'

import { queryKeysNamespace } from 'lib-common/query'

export type QueryKeyPrefix =
  | 'applications'
  | 'applicationDecisions'
  | 'assistanceDecisions'
  | 'assistancePreschoolDecisions'
  | 'calendar'
  | 'childDocuments'
  | 'children'
  | 'incomeStatements'
  | 'login'
  | 'map'
  | 'messages'
  | 'pedagogicalDocuments'
  | 'personalDetails'
  | 'serviceNeedAndDailyServiceTime'
  | 'vasuAndLeops'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      networkMode: 'always'
    },
    mutations: {
      networkMode: 'always'
    }
  }
})
export { QueryClientProvider } from '@tanstack/react-query'

export const createQueryKeys = queryKeysNamespace<QueryKeyPrefix>()
