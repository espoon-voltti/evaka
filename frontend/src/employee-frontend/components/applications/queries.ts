// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  cancelApplication,
  getApplicationSummaries,
  simpleApplicationAction,
  simpleBatchAction,
  updateServiceWorkerNote
} from '../../generated/api-clients/application'
import { getServiceNeedOptionPublicInfos } from '../../generated/api-clients/serviceneed'

const q = new Queries()

export const getApplicationSummariesQuery = q.query(getApplicationSummaries)

export const simpleApplicationActionMutation = q.mutation(
  simpleApplicationAction,
  [getApplicationSummariesQuery.prefix]
)

export const simpleBatchActionMutation = q.mutation(simpleBatchAction, [
  getApplicationSummariesQuery.prefix
])

export const cancelApplicationMutation = q.mutation(cancelApplication, [
  getApplicationSummariesQuery.prefix
])

export const updateServiceWorkerNoteMutation = q.mutation(
  updateServiceWorkerNote,
  [getApplicationSummariesQuery.prefix]
)

export const serviceNeedPublicInfosQuery = q.query(
  getServiceNeedOptionPublicInfos
)
