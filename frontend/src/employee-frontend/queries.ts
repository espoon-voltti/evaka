// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import { getAssistanceActionOptions } from './generated/api-clients/assistance'
import { getEmployees } from './generated/api-clients/pis'
import { getServiceNeedOptions } from './generated/api-clients/serviceneed'
import { createQueryKeys } from './query'

const queryKeys = createQueryKeys('common', {
  serviceNeeds: () => ['serviceNeeds'],
  assistanceActionOptions: () => ['assistanceActionOptions'],
  employees: () => ['employees']
})

export const serviceNeedsQuery = query({
  api: getServiceNeedOptions,
  queryKey: queryKeys.serviceNeeds
})

export const getAssistanceActionOptionsQuery = query({
  api: getAssistanceActionOptions,
  queryKey: queryKeys.assistanceActionOptions
})

export const getEmployeesQuery = query({
  api: getEmployees,
  queryKey: queryKeys.employees
})
