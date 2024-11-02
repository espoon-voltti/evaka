// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import { futureAbsencesOfChild } from '../generated/api-clients/absence'
import {
  cancelFullDayAbsence,
  getAttendanceStatuses,
  getChildExpectedAbsencesOnDeparture,
  postArrivals,
  postDeparture,
  postFullDayAbsence,
  returnToComing,
  returnToPresent
} from '../generated/api-clients/attendance'
import { deleteImage } from '../generated/api-clients/childimages'
import {
  getChildReservationsForDay,
  getConfirmedRangeData,
  getReservationStatisticsForConfirmedDays,
  setConfirmedRangeReservations
} from '../generated/api-clients/reservations'
import { getBasicInfo } from '../generated/api-clients/sensitive'
import { createQueryKeys } from '../query'

import { getUnitChildren, uploadChildImage } from './api'

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
  ],
  childBasicInfo: (childId: string) => ['childBasicInfo', childId]
})

export const childrenQuery = query({
  api: getUnitChildren,
  queryKey: queryKeys.children,
  options: {
    staleTime: 5 * 60 * 1000
  }
})

export const childBasicInfoQuery = query({
  api: getBasicInfo,
  queryKey: ({ childId }) => queryKeys.childBasicInfo(childId),
  options: {
    staleTime: 5 * 60 * 1000
  }
})

export const attendanceStatusesQuery = query({
  api: getAttendanceStatuses,
  queryKey: ({ unitId }) => queryKeys.attendanceStatuses(unitId),
  options: {
    staleTime: 5 * 60 * 1000
  }
})

export const childExpectedAbsencesOnDepartureQuery = query({
  api: getChildExpectedAbsencesOnDeparture,
  queryKey: ({ unitId, childId, body: { departed } }) =>
    queryKeys.childDeparture({ unitId, childId, departed })
})

export const confirmedDayReservationsQuery = query({
  api: getChildReservationsForDay,
  queryKey: ({ unitId, examinationDate }) =>
    queryKeys.confirmedDayReservations(unitId, examinationDate)
})

export const confirmedDaysReservationsStatisticsQuery = query({
  api: getReservationStatisticsForConfirmedDays,
  queryKey: ({ unitId }) => queryKeys.confirmedDaysReservationStatistics(unitId)
})

export const getConfirmedRangeQuery = query({
  api: getConfirmedRangeData,
  queryKey: ({ childId }) => queryKeys.confirmedRangeReservations(childId)
})

export const setConfirmedRangeMutation = mutation({
  api: setConfirmedRangeReservations,
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.confirmedRangeReservations(childId),
    queryKeys.futureAbsencesByChild(childId)
  ]
})

export const getFutureAbsencesByChildQuery = query({
  api: futureAbsencesOfChild,
  queryKey: ({ childId }) => queryKeys.futureAbsencesByChild(childId)
})

export const createFullDayAbsenceMutation = mutation({
  api: postFullDayAbsence,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery({ unitId }).queryKey
  ]
})

export const createArrivalMutation = mutation({
  api: postArrivals,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery({ unitId }).queryKey
  ]
})

export const createDepartureMutation = mutation({
  api: postDeparture,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery({ unitId }).queryKey
  ]
})

export const returnToPresentMutation = mutation({
  api: returnToPresent,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery({ unitId }).queryKey
  ]
})

export const returnToComingMutation = mutation({
  api: returnToComing,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery({ unitId }).queryKey
  ]
})

export const cancelAbsenceMutation = mutation({
  api: cancelFullDayAbsence,
  invalidateQueryKeys: ({ unitId }) => [
    attendanceStatusesQuery({ unitId }).queryKey
  ]
})

export const uploadChildImageMutation = mutation({
  api: ({ childId, file }: { unitId: UUID; childId: UUID; file: File }) =>
    uploadChildImage({ childId, file }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteChildImageMutation = mutation({
  api: (arg: Arg0<typeof deleteImage> & { unitId: UUID }) => deleteImage(arg),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})
