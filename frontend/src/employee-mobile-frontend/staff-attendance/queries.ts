// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ExternalStaffArrivalRequest,
  ExternalStaffDepartureRequest,
  StaffArrivalRequest,
  StaffDepartureRequest,
  StaffAttendanceUpdateRequest
} from 'lib-common/generated/api-types/attendance'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  getUnitStaffAttendances,
  postExternalStaffArrival,
  postExternalStaffDeparture,
  postStaffArrival,
  postStaffDeparture,
  putStaffAttendances
} from './api'

const queryKeys = createQueryKeys('staffAttendance', {
  ofUnit: (unitId: UUID) => ['unit', unitId]
})

export const staffAttendanceQuery = query({
  api: getUnitStaffAttendances,
  queryKey: queryKeys.ofUnit
})

export const staffArrivalMutation = mutation({
  api: ({ request }: { unitId: UUID; request: StaffArrivalRequest }) =>
    postStaffArrival(request),
  invalidateQueryKeys: ({ unitId }) => [staffAttendanceQuery(unitId).queryKey]
})

export const staffDepartureMutation = mutation({
  api: ({ request }: { unitId: UUID; request: StaffDepartureRequest }) =>
    postStaffDeparture(request),
  invalidateQueryKeys: ({ unitId }) => [staffAttendanceQuery(unitId).queryKey]
})

export const externalStaffArrivalMutation = mutation({
  api: ({ request }: { unitId: UUID; request: ExternalStaffArrivalRequest }) =>
    postExternalStaffArrival(request),
  invalidateQueryKeys: ({ unitId }) => [staffAttendanceQuery(unitId).queryKey]
})

export const externalStaffDepartureMutation = mutation({
  api: ({
    request
  }: {
    unitId: UUID
    request: ExternalStaffDepartureRequest
  }) => postExternalStaffDeparture(request),
  invalidateQueryKeys: ({ unitId }) => [staffAttendanceQuery(unitId).queryKey]
})

export const staffAttendanceMutation = mutation({
  api: ({
    unitId,
    request
  }: {
    unitId: UUID
    request: StaffAttendanceUpdateRequest
  }) => putStaffAttendances(unitId, request),
  invalidateQueryKeys: ({ unitId }) => [staffAttendanceQuery(unitId).queryKey]
})
