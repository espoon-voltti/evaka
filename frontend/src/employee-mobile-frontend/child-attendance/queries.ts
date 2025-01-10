// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import { futureAbsencesOfChild } from '../generated/api-clients/absence'
import {
  cancelFullDayAbsence,
  getAttendanceStatuses,
  getExpectedAbsencesOnDepartures,
  postArrivals,
  postDepartures,
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

import { getUnitChildren, uploadChildImage } from './api'

const q = new Queries()

export const childrenQuery = q.query(getUnitChildren, {
  staleTime: 5 * 60 * 1000
})

export const childBasicInfoQuery = q.query(getBasicInfo, {
  staleTime: 5 * 60 * 1000
})

export const attendanceStatusesQuery = q.query(getAttendanceStatuses, {
  staleTime: 5 * 60 * 1000
})

export const expectedAbsencesOnDeparturesQuery = q.query(
  getExpectedAbsencesOnDepartures
)

export const confirmedDayReservationsQuery = q.query(getChildReservationsForDay)

export const confirmedDaysReservationsStatisticsQuery = q.query(
  getReservationStatisticsForConfirmedDays
)

export const getConfirmedRangeQuery = q.query(getConfirmedRangeData)

export const getFutureAbsencesByChildQuery = q.query(futureAbsencesOfChild)

export const setConfirmedRangeMutation = q.mutation(
  setConfirmedRangeReservations,
  [
    ({ childId }) => getConfirmedRangeQuery({ childId }),
    ({ childId }) => getFutureAbsencesByChildQuery({ childId })
  ]
)

export const createFullDayAbsenceMutation = q.mutation(postFullDayAbsence, [
  ({ unitId }) => attendanceStatusesQuery({ unitId })
])

export const createArrivalMutation = q.mutation(postArrivals, [
  ({ unitId }) => attendanceStatusesQuery({ unitId })
])

export const createDeparturesMutation = q.mutation(postDepartures, [
  ({ unitId }) => attendanceStatusesQuery({ unitId })
])

export const returnToPresentMutation = q.mutation(returnToPresent, [
  ({ unitId }) => attendanceStatusesQuery({ unitId })
])

export const returnToComingMutation = q.mutation(returnToComing, [
  ({ unitId }) => attendanceStatusesQuery({ unitId })
])

export const cancelAbsenceMutation = q.mutation(cancelFullDayAbsence, [
  ({ unitId }) => attendanceStatusesQuery({ unitId })
])

export const uploadChildImageMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(uploadChildImage, [({ unitId }) => childrenQuery(unitId)])

export const deleteChildImageMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(deleteImage, [({ unitId }) => childrenQuery(unitId)])
