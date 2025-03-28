// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getAssistanceActionOptions } from './generated/api-clients/assistance'
import {
  getEmployees,
  getOrCreatePersonBySsn,
  getPersonIdentity,
  searchPerson
} from './generated/api-clients/pis'
import { getServiceNeedOptions } from './generated/api-clients/serviceneed'

const q = new Queries()

export const serviceNeedOptionsQuery = q.query(getServiceNeedOptions)

export const getAssistanceActionOptionsQuery = q.query(
  getAssistanceActionOptions
)

export const getEmployeesQuery = q.query(getEmployees)

export const personIdentityQuery = q.query(getPersonIdentity)

export const personBySsnQuery = q.query(getOrCreatePersonBySsn)

export const searchPersonQuery = q.query(searchPerson)
