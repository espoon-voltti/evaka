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
  upsertApplicationPlacementDraft,
  updateServiceWorkerNote,
  deleteApplicationPlacementDraft
} from '../../generated/api-clients/application'
import { getServiceNeedOptionPublicInfos } from '../../generated/api-clients/serviceneed'

const q = new Queries()

export const getApplicationSummariesQuery = q.query(getApplicationSummaries, {
  refetchOnWindowFocus: false
})

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

export const upsertApplicationPlacementDraftMutation = q.parametricMutation<{
  previousUnitId: DaycareId | null
}>()(upsertApplicationPlacementDraft, [
  ({ body: { unitId } }) =>
    unitId ? getPlacementDesktopDaycareQuery({ unitId }) : undefined,
  ({ previousUnitId, body }) =>
    previousUnitId && previousUnitId !== body.unitId
      ? getPlacementDesktopDaycareQuery({ unitId: previousUnitId })
      : undefined
])

export const deleteApplicationPlacementDraftMutation = q.parametricMutation<{
  previousUnitId: DaycareId
}>()(deleteApplicationPlacementDraft, [
  ({ previousUnitId }) =>
    getPlacementDesktopDaycareQuery({ unitId: previousUnitId })
])

export const getPlacementDesktopDaycareQuery = q.prefixedQuery(
  getPlacementDesktopDaycare,
  ({ unitId }) => unitId,
  {
    refetchOnMount: false,
    refetchOnWindowFocus: false
  }
)

export const getPlacementDesktopDaycaresQuery = q.query(
  getPlacementDesktopDaycares,
  {
    refetchOnWindowFocus: false
  }
)
