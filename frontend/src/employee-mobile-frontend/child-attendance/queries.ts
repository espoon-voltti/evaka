// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ConfirmedRangeDateUpdate } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  createArrival,
  createDeparture,
  createFullDayAbsence,
  getFutureAbsencesByChildPlain,
  getConfirmedRange,
  getUnitAttendanceStatuses,
  getUnitChildren,
  putConfirmedRange,
  returnToComing,
  returnToPresent,
  deleteChildImage,
  uploadChildImage,
  getChildExpectedAbsencesOnDeparture,
  getUnitConfirmedDayReservations,
  getUnitConfirmedDaysReservationStatistics
} from './api'

const queryKeys = createQueryKeys('childAttendance', {
  children: (unitId: string) => ['children', unitId],
  confirmedRangeReservations: (childId: string) => [
    'confirmedRangeReservations',
    childId
  ],
  futureAbsencesByChild: (childId: string) => [
    'futureAbsencesByChild',
    childId
  ],
  attendanceStatuses: (unitId: string) => ['attendanceStatuses', unitId],
  childDeparture: ({
    unitId,
    childId,
    departed
  }: {
    unitId: UUID
    childId: UUID
    departed: LocalTime
  }) => ['childDeparture', unitId, childId, departed],
  confirmedDayReservations: (unitId: string, examinationDate: LocalDate) => [
    'confirmedDayReservations',
    unitId,
    examinationDate
  ],
  confirmedDaysReservationStatistics: (unitId: string) => [
    'confirmedDaysReservationStatistics',
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

export const childExpectedAbsencesOnDepartureQuery = query({
  api: getChildExpectedAbsencesOnDeparture,
  queryKey: ({ unitId, childId, departed }) =>
    queryKeys.childDeparture({ unitId, childId, departed })
})

export const confirmedDayReservationsQuery = query({
  api: getUnitConfirmedDayReservations,
  queryKey: queryKeys.confirmedDayReservations
})

export const confirmedDaysReservationsStatisticsQuery = query({
  api: getUnitConfirmedDaysReservationStatistics,
  queryKey: queryKeys.confirmedDaysReservationStatistics
})

export const getConfirmedRangeQuery = query({
  api: getConfirmedRange,
  queryKey: queryKeys.confirmedRangeReservations
})

export const setConfirmedRangeMutation = mutation({
  api: ({
    childId,
    body
  }: {
    childId: UUID
    body: ConfirmedRangeDateUpdate[]
  }) => putConfirmedRange(childId, body),
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.confirmedRangeReservations(childId),
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
