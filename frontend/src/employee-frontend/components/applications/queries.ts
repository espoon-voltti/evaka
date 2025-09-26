// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  cancelApplication,
  getApplicationSummaries,
  getPlacementDesktopDaycare,
  getPlacementDesktopDaycares,
  simpleApplicationAction,
  simpleBatchAction,
  updateApplicationPlacementDraft,
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

export const updateApplicationPlacementDraftMutation = q.parametricMutation<{
  previousUnitId: DaycareId | null
}>()(updateApplicationPlacementDraft, [
  ({ body: { unitId } }) =>
    unitId ? getPlacementDesktopDaycareQuery({ unitId }) : undefined,
  ({ previousUnitId }) =>
    previousUnitId
      ? getPlacementDesktopDaycareQuery({ unitId: previousUnitId })
      : undefined
])

export const getPlacementDesktopDaycareQuery = q.prefixedQuery(
  getPlacementDesktopDaycare,
  ({ unitId }) => unitId,
  {
    refetchOnMount: false
  }
)

export const getPlacementDesktopDaycaresQuery = q.query(
  getPlacementDesktopDaycares
)
