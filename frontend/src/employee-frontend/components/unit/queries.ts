// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'
import { featureFlags } from 'lib-customizations/employee'

import {
  acceptPlacementProposal,
  getUnitApplications,
  respondToPlacementProposal
} from '../../generated/api-clients/application'
import {
  createBackupCare,
  updateBackupCare
} from '../../generated/api-clients/backupcare'
import {
  createDaycare,
  createGroup,
  deleteGroup,
  getAreas,
  getDaycare,
  getDaycares,
  getGroups,
  getUnitGroupDetails,
  getUnitNotifications,
  updateDaycare,
  updateGroup
} from '../../generated/api-clients/daycare'
import {
  getOccupancyPeriodsSpeculated,
  getUnitOccupancies
} from '../../generated/api-clients/occupancy'
import {
  createGroupPlacement,
  deleteGroupPlacement,
  transferGroupPlacement
} from '../../generated/api-clients/placement'
import {
  getAttendanceReservations,
  getExpectedAbsences,
  postChildDatePresence,
  postReservations
} from '../../generated/api-clients/reservations'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('unit', {
  areas: () => ['areas'],
  units: (arg: Arg0<typeof getDaycares>) => ['units', arg],
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
    params: Arg0<typeof getOccupancyPeriodsSpeculated>
  ) => ['unitSpeculatedOccupancyRates', params],
  unitGroupDetails: (unitId: UUID) => ['unitGroupDetails', unitId],
  unitGroupDetailsRange: (unitId: UUID, from: LocalDate, to: LocalDate) => [
    'unitGroupDetails',
    unitId,
    { from, to }
  ],

  unitAttendanceReservations: () => ['unitAttendanceReservations'],
  unitAttendanceReservationsRange: (
    arg: Arg0<typeof getAttendanceReservations>
  ) => ['unitAttendanceReservations', arg],

  expectedAbsences: (arg: Arg0<typeof getExpectedAbsences>) => [
    'expectedAbsences',
    arg
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
  queryKey: ({ daycareId }) => queryKeys.unit(daycareId)
})

export const unitNotificationsQuery = query({
  api: getUnitNotifications,
  queryKey: ({ daycareId }) => queryKeys.unitNotifications(daycareId)
})

export const unitOccupanciesQuery = query({
  api: getUnitOccupancies,
  queryKey: ({ unitId, from, to }) =>
    queryKeys.unitOccupancies(unitId, from, to)
})

export const unitApplicationsQuery = query({
  api: getUnitApplications,
  queryKey: ({ unitId }) => queryKeys.unitApplications(unitId)
})

export const createGroupPlacementMutation = mutation({
  api: (arg: Arg0<typeof createGroupPlacement> & { unitId: UUID }) =>
    createGroupPlacement(arg),
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const deleteGroupPlacementMutation = mutation({
  api: (arg: Arg0<typeof deleteGroupPlacement> & { unitId: UUID }) =>
    deleteGroupPlacement(arg),
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const transferGroupMutation = mutation({
  api: (arg: Arg0<typeof transferGroupPlacement> & { unitId: UUID }) =>
    transferGroupPlacement(arg),
  invalidateQueryKeys: ({ unitId }) => [
    queryKeys.unitGroupDetails(unitId),
    queryKeys.unitNotifications(unitId)
  ]
})

export const createGroupMutation = mutation({
  api: createGroup,
  invalidateQueryKeys: ({ daycareId }) => [
    queryKeys.unitNotifications(daycareId)
  ]
})

export const unitGroupsQuery = query({
  api: getGroups,
  queryKey: ({ daycareId }) => queryKeys.unitGroups(daycareId)
})

export const updateGroupMutation = mutation({
  api: updateGroup,
  invalidateQueryKeys: ({ daycareId }) => [
    queryKeys.unitGroupDetails(daycareId),
    queryKeys.unitNotifications(daycareId)
  ]
})

export const deleteGroupMutation = mutation({
  api: deleteGroup,
  invalidateQueryKeys: ({ daycareId }) => [
    queryKeys.unitGroupDetails(daycareId),
    queryKeys.unitNotifications(daycareId)
  ]
})

export const unitSpeculatedOccupancyRatesQuery = query({
  api: getOccupancyPeriodsSpeculated,
  queryKey: queryKeys.unitSpeculatedOccupancyRates
})

export const createUnitMutation = mutation({
  api: createDaycare
})

export const updateUnitMutation = mutation({
  api: updateDaycare,
  invalidateQueryKeys: ({ daycareId }) => [queryKeys.unit(daycareId)]
})

export const unitGroupDetailsQuery = query({
  api: getUnitGroupDetails,
  queryKey: ({ unitId, from, to }) =>
    queryKeys.unitGroupDetailsRange(unitId, from, to)
})

export const postReservationsMutation = mutation({
  api: (arg: Pick<Arg0<typeof postReservations>, 'body'>) =>
    postReservations({
      ...arg,
      automaticFixedScheduleAbsencesEnabled:
        featureFlags.automaticFixedScheduleAbsences
    }),
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
  api: getAttendanceReservations,
  queryKey: queryKeys.unitAttendanceReservationsRange
})

export const childDateExpectedAbsencesQuery = query({
  api: getExpectedAbsences,
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
