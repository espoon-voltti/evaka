// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getAttendancesByUnit,
  getEmployeeAttendances,
  getOpenGroupAttendance,
  markArrival,
  markDeparture,
  markExternalArrival,
  markExternalDeparture,
  setAttendances
} from '../generated/api-clients/attendance'

const q = new Queries()

export const staffAttendanceQuery = q.query(getAttendancesByUnit)

export const staffAttendancesByEmployeeQuery = q.query(getEmployeeAttendances)

export const staffArrivalMutation = q.mutation(markArrival, [
  staffAttendanceQuery.prefix,
  staffAttendancesByEmployeeQuery.prefix
])

export const staffDepartureMutation = q.mutation(markDeparture, [
  staffAttendanceQuery.prefix,
  staffAttendancesByEmployeeQuery.prefix
])

export const externalStaffArrivalMutation = q.mutation(markExternalArrival, [
  staffAttendanceQuery.prefix,
  staffAttendancesByEmployeeQuery.prefix
])

export const externalStaffDepartureMutation = q.mutation(
  markExternalDeparture,
  [staffAttendanceQuery.prefix, staffAttendancesByEmployeeQuery.prefix]
)

export const staffAttendanceMutation = q.mutation(setAttendances, [
  staffAttendanceQuery.prefix,
  staffAttendancesByEmployeeQuery.prefix
])

export const openAttendanceQuery = q.query(getOpenGroupAttendance)
