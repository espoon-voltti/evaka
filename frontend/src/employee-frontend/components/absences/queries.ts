// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  addPresences,
  deleteHolidayReservations,
  groupMonthCalendar,
  upsertAbsences
} from '../../generated/api-clients/absence'
import {
  getStaffAttendancesByGroup,
  upsertStaffAttendance
} from '../../generated/api-clients/daycare'

const q = new Queries()

export const groupMonthCalendarQuery = q.query(groupMonthCalendar)

export const getStaffAttendancesByGroupQuery = q.query(
  getStaffAttendancesByGroup
)

export const upsertAbsencesMutation = q.mutation(upsertAbsences, [
  groupMonthCalendarQuery.prefix
])

export const addPresencesMutation = q.mutation(addPresences, [
  groupMonthCalendarQuery.prefix
])

export const deleteHolidayReservationsMutation = q.mutation(
  deleteHolidayReservations,
  [groupMonthCalendarQuery.prefix]
)

export const upsertStaffAttendanceMutation = q.mutation(upsertStaffAttendance, [
  // No automatic invalidation of queries as currently it's not desired in the
  // only place where the mutation is used (StaffAttendance component)
])
