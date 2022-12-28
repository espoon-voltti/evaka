// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  createFullDayAbsence,
  createDeparture,
  getUnitAttendanceStatuses,
  getUnitChildren,
  createArrival,
  returnToPresent,
  returnToComing,
  deleteChildImage,
  uploadChildImage
} from './api'

export const queryKeys = createQueryKeys('childAttendance', {
  children: (unitId: string) => ['children', unitId],
  attendanceStatus: (unitId: string) => ['attendanceStatus', unitId]
})

export const childrenQuery = query({
  api: getUnitChildren,
  queryKey: queryKeys.children
})

export const attendanceStatusesQuery = query({
  api: getUnitAttendanceStatuses,
  queryKey: queryKeys.attendanceStatus
})

export const createFullDayAbsenceMutation = mutation({
  api: createFullDayAbsence,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery(unitId).queryKey
  ]
})

export const createArrivalMutation = mutation({
  api: createArrival,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery(unitId).queryKey
  ]
})

export const createDepartureMutation = mutation({
  api: createDeparture,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery(unitId).queryKey
  ]
})

export const returnToPresentMutation = mutation({
  api: returnToPresent,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery(unitId).queryKey
  ]
})

export const returnToComingMutation = mutation({
  api: returnToComing,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery(unitId).queryKey
  ]
})

export const uploadChildImageMutation = mutation({
  api: ({ childId, file }: { unitId: UUID; childId: UUID; file: File }) =>
    uploadChildImage({ childId, file }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteChildImageMutation = mutation({
  api: ({ childId }: { unitId: UUID; childId: UUID }) =>
    deleteChildImage(childId),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})
