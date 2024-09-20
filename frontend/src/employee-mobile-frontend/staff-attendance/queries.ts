// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

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
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('staffAttendance', {
  unit: (unitId: UUID) => ['unit', unitId],
  unitDate: (unitId: UUID, date: LocalDate | null | undefined) => [
    'unit',
    unitId,
    'date',
    date
  ],
  unitEmployee: (unitId: UUID, employeeId: UUID) => [
    'unit',
    unitId,
    'employeeId',
    employeeId
  ],
  unitEmployeeRange: (
    unitId: UUID,
    employeeId: UUID,
    from: LocalDate,
    to: LocalDate
  ) => ['unit', unitId, 'employeeId', employeeId, 'range', from, to],

  openGroupAttendance: (arg: Arg0<typeof getOpenGroupAttendance>) => [
    'openGroupAttendance',
    arg
  ]
})

export const staffAttendanceQuery = query({
  api: getAttendancesByUnit,
  queryKey: (arg) => queryKeys.unitDate(arg.unitId, arg.date)
})

export const staffAttendancesByEmployeeQuery = query({
  api: getEmployeeAttendances,
  queryKey: (arg) =>
    queryKeys.unitEmployeeRange(arg.unitId, arg.employeeId, arg.from, arg.to)
})

export const staffArrivalMutation = mutation({
  api: (arg: Arg0<typeof markArrival> & { unitId: UUID }) => markArrival(arg),
  invalidateQueryKeys: ({ unitId }) => [queryKeys.unit(unitId)]
})

export const staffDepartureMutation = mutation({
  api: (arg: Arg0<typeof markDeparture> & { unitId: UUID }) =>
    markDeparture(arg),
  invalidateQueryKeys: ({ unitId }) => [queryKeys.unit(unitId)]
})

export const externalStaffArrivalMutation = mutation({
  api: (arg: Arg0<typeof markExternalArrival> & { unitId: UUID }) =>
    markExternalArrival(arg),
  invalidateQueryKeys: ({ unitId }) => [queryKeys.unit(unitId)]
})

export const externalStaffDepartureMutation = mutation({
  api: (arg: Arg0<typeof markExternalDeparture> & { unitId: UUID }) =>
    markExternalDeparture(arg),
  invalidateQueryKeys: ({ unitId }) => [queryKeys.unit(unitId)]
})

export const staffAttendanceMutation = mutation({
  api: setAttendances,
  invalidateQueryKeys: ({ unitId }) => [queryKeys.unit(unitId)]
})

export const openAttendanceQuery = query({
  api: getOpenGroupAttendance,
  queryKey: queryKeys.openGroupAttendance
})
