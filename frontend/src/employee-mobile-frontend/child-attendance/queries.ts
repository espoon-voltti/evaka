// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { NonReservableReservation } from 'lib-common/generated/api-types/reservations'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  createArrival,
  createDeparture,
  createFullDayAbsence,
  deleteChildImage,
  getChildDeparture,
  getFutureAbsencesByChildPlain,
  getNonReservableReservations,
  getUnitAttendanceStatuses,
  getUnitChildren,
  putNonReservableReservationservations,
  returnToComing,
  returnToPresent,
  uploadChildImage,
  deleteChildImage,
  uploadChildImage,
  getChildDeparture,
  getUnitConfirmedDaysReservations
} from './api'

const queryKeys = createQueryKeys('childAttendance', {
  children: (unitId: string) => ['children', unitId],
  nonReservableReservations: (childId: string) => [
    'nonReservableReservations',
    childId
  ],
  futureAbsencesByChild: (childId: string) => [
    'futureAbsencesByChild',
    childId
  ],
  attendanceStatuses: (unitId: string) => ['attendanceStatuses', unitId],
  childDeparture: ({ unitId, childId }: { unitId: UUID; childId: UUID }) => [
    'childDeparture',
    unitId,
    childId
  ],
  confirmedDaysReservations: (unitId: string) => [
    'confirmedDaysReservations',
    unitId
  ]
})

export const childrenQuery = query({
  api: getUnitChildren,
  queryKey: queryKeys.children,
  options: {
    staleTime: 5 * 60 * 1000
  }
})

export const attendanceStatusesQuery = query({
  api: getUnitAttendanceStatuses,
  queryKey: queryKeys.attendanceStatuses,
  options: {
    staleTime: 5 * 60 * 1000
  }
})

export const childDepartureQuery = query({
  api: getChildDeparture,
  queryKey: ({ unitId, childId }) =>
    queryKeys.childDeparture({ unitId, childId })
})

export const confirmedDaysReservationsQuery = query({
  api: getUnitConfirmedDaysReservations,
  queryKey: queryKeys.confirmedDaysReservations
})

export const getNonReservableReservationsQuery = query({
  api: getNonReservableReservations,
  queryKey: queryKeys.nonReservableReservations
})

export const setNonReservableReservationsMutation = mutation({
  api: ({
    childId,
    body
  }: {
    childId: UUID
    body: NonReservableReservation[]
  }) => putNonReservableReservationservations(childId, body),
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.nonReservableReservations(childId),
    queryKeys.futureAbsencesByChild(childId)
  ]
})

export const getFutureAbsencesByChildQuery = query({
  api: getFutureAbsencesByChildPlain,
  queryKey: queryKeys.futureAbsencesByChild
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
