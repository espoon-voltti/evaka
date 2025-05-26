// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getAssistanceActionOptions } from './generated/api-clients/assistance'
import { deleteAttachment } from './generated/api-clients/attachment'
import { getAreas, getUnits } from './generated/api-clients/daycare'
import {
  getEmployees,
  getFinanceDecisionHandlers,
  getOrCreatePersonBySsn,
  getPersonDependants,
  getPersonIdentity,
  searchPerson
} from './generated/api-clients/pis'
import { getChildDocumentDecisionsReportNotificationCount } from './generated/api-clients/reports'
import { getServiceNeedOptions } from './generated/api-clients/serviceneed'

const q = new Queries()

// These are common queries of semi-static data that generally do not need to be invalidated after mutations

export const serviceNeedOptionsQuery = q.query(getServiceNeedOptions)

export const getAssistanceActionOptionsQuery = q.query(
  getAssistanceActionOptions
)

export const getEmployeesQuery = q.query(getEmployees)

export const personIdentityQuery = q.query(getPersonIdentity)

export const personBySsnQuery = q.query(getOrCreatePersonBySsn)

export const searchPersonQuery = q.query(searchPerson)

export const personDependantsQuery = q.query(getPersonDependants)

export const areasQuery = q.query(getAreas)

export const unitsQuery = q.query(getUnits)

export const financeDecisionHandlersQuery = q.query(getFinanceDecisionHandlers)

export const childDocumentDecisionsReportNotificationCountQuery = q.query(
  getChildDocumentDecisionsReportNotificationCount
)

export const deleteAttachmentMutation = q.mutation(deleteAttachment, [])
