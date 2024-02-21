// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { ExpectedAbsencesRequest } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import { getAreas } from '../../api/daycare'
import {
  createDaycare,
  createGroup,
  CreateGroupPlacement,
  createGroupPlacement,
  deleteGroup,
  deleteGroupPlacement,
  getChildDateExpectedAbsences,
  getDaycare,
  getDaycareGroups,
  getDaycares,
  getSpeculatedOccupancyRates,
  getUnitApplications,
  getUnitAttendanceReservations,
  getUnitGroupDetails,
  getUnitNotifications,
  getUnitOccupancies,
  postChildDatePresence,
  postReservations,
  TransferGroup,
  transferGroup,
  updateDaycare,
  updateGroup
} from '../../api/unit'
import {
  acceptPlacementProposal,
  respondToPlacementProposal
} from '../../generated/api-clients/application'
import {
  createBackupCare,
  updateBackupCare
} from '../../generated/api-clients/backupcare'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('unit', {
  areas: () => ['areas'],
  units: () => ['units'],
  unit: (unitId: UUID) => ['unit', unitId],
  unitNotifications: (unitId: UUID) => ['unitNotifications', unitId],
  unitOccupancies: (unitId: UUID, from: LocalDate, to: LocalDate) => [
    'unitOccupancies',
    unitId,
    { from, to }
  ],
  unitApplications: (unitId: UUID) => ['unitApplications', unitId],
  unitGroups: (unitId: UUID) => ['unitGroups', unitId],
  unitSpeculatedOccupancyRates: (
    applicationId: UUID,
    unitId: UUID,
    from: LocalDate,
    to: LocalDate,
    preschoolDaycareFrom?: LocalDate,
    preschoolDaycareTo?: LocalDate
  ) => [
    'unitSpeculatedOccupancyRates',
    {
      applicationId,
      unitId,
      from,
      to,
      preschoolDaycareFrom,
      preschoolDaycareTo
    }
  ],
  unitGroupDetails: (unitId: UUID) => ['unitGroupDetails', unitId],
  unitGroupDetailsRange: (unitId: UUID, from: LocalDate, to: LocalDate) => [
    'unitGroupDetails',
    unitId,
    { from, to }
  ],

  unitAttendanceReservations: () => ['unitAttendanceReservations'],
  unitAttendanceReservationsRange: (
    unitId: UUID,
    dateRange: FiniteDateRange,
    includeNonOperationalDays: boolean
  ) => [
    'unitAttendanceReservations',
    unitId,
    { dateRange, includeNonOperationalDays }
  ],

  expectedAbsences: (input: ExpectedAbsencesRequest) => [
    'expectedAbsences',
    input
  ]
})

export const areaQuery = query({
  api: getAreas,
  queryKey: queryKeys.areas
})

export const unitsQuery = query({
  api: getDaycares,
  queryKey: queryKeys.units
})

export const unitQuery = query({
  api: getDaycare,
  queryKey: queryKeys.unit
})

export const unitNotificationsQuery = query({
  api: getUnitNotifications,
  queryKey: queryKeys.unitNotifications
})

export const unitOccupanciesQuery = query({
  api: getUnitOccupancies,
  queryKey: queryKeys.unitOccupancies
})

export const unitApplicationsQuery = query({
  api: getUnitApplications,
  queryKey: queryKeys.unitApplications
})

export const createGroupPlacementMutation = mutation({
  api: ({
    unitId: _,
    ...payload
  }: CreateGroupPlacement & {
    unitId: UUID
  }) => createGroupPlacement(payload),
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const deleteGroupPlacementMutation = mutation({
  api: ({
    unitId: _,
    groupPlacementId
  }: {
    unitId: UUID
    groupPlacementId: UUID
  }) => deleteGroupPlacement(groupPlacementId),
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const transferGroupMutation = mutation({
  api: ({ unitId: _, ...payload }: TransferGroup & { unitId: UUID }) =>
    transferGroup(payload),
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const createGroupMutation = mutation({
  api: createGroup,
  invalidateQueryKeys: ({ unitId }) => [queryKeys.unitNotifications(unitId)]
})

export const unitGroupsQuery = query({
  api: getDaycareGroups,
  queryKey: queryKeys.unitGroups
})

export const updateGroupMutation = mutation({
  api: updateGroup,
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const deleteGroupMutation = mutation({
  api: deleteGroup,
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const unitSpeculatedOccupancyRatesQuery = query({
  api: getSpeculatedOccupancyRates,
  queryKey: queryKeys.unitSpeculatedOccupancyRates
})

export const createUnitMutation = mutation({
  api: createDaycare
})

export const updateUnitMutation = mutation({
  api: updateDaycare,
  invalidateQueryKeys: ({ id }) => [queryKeys.unit(id)]
})

export const unitGroupDetailsQuery = query({
  api: getUnitGroupDetails,
  queryKey: queryKeys.unitGroupDetailsRange
})

export const postReservationsMutation = mutation({
  api: postReservations,
  invalidateQueryKeys: () => [queryKeys.unitAttendanceReservations()]
})

export const createBackupCareMutation = mutation({
  api: createBackupCare
})

export const updateBackupCareMutation = mutation({
  api: (arg: Arg0<typeof updateBackupCare> & { unitId: UUID }) =>
    updateBackupCare(arg),
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const unitAttendanceReservationsQuery = query({
  api: getUnitAttendanceReservations,
  queryKey: queryKeys.unitAttendanceReservationsRange
})

export const childDateExpectedAbsencesQuery = query({
  api: getChildDateExpectedAbsences,
  queryKey: queryKeys.expectedAbsences
})

export const upsertChildDatePresenceMutation = mutation({
  api: postChildDatePresence,
  invalidateQueryKeys: () => [queryKeys.unitAttendanceReservations()]
})

export const acceptPlacementProposalMutation = mutation({
  api: acceptPlacementProposal,
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitApplications(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const respondToPlacementProposalMutation = mutation({
  api: (arg: Arg0<typeof respondToPlacementProposal> & { unitId: UUID }) =>
    respondToPlacementProposal(arg),
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitApplications(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})
