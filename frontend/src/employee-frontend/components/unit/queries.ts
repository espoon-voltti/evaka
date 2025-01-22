// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  acceptPlacementProposal,
  getUnitApplications,
  respondToPlacementProposal
} from '../../generated/api-clients/application'
import { getOpenGroupAttendance } from '../../generated/api-clients/attendance'
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
  getUnits,
  getUnitServiceWorkerNote,
  setUnitServiceWorkerNote,
  updateDaycare,
  updateGroup,
  updateUnitClosingDate
} from '../../generated/api-clients/daycare'
import {
  getOccupancyPeriodsSpeculated,
  getUnitOccupancies,
  getUnitPlannedOccupanciesForDay,
  getUnitRealizedOccupanciesForDay
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
import { getUndecidedServiceApplications } from '../../generated/api-clients/serviceneed'

const q = new Queries()

export const areaQuery = q.query(getAreas)

export const unitFilterQuery = q.query(getUnits)

export const unitsQuery = q.query(getDaycares)

export const unitQuery = q.query(getDaycare)

export const unitServiceWorkerNoteQuery = q.query(getUnitServiceWorkerNote)

export const unitServiceWorkerNoteMutation = q.mutation(
  setUnitServiceWorkerNote,
  [unitServiceWorkerNoteQuery]
)

export const unitNotificationsQuery = q.query(getUnitNotifications)

export const unitOccupanciesQuery = q.query(getUnitOccupancies)

export const unitRealizedOccupanciesForDayQuery = q.query(
  getUnitRealizedOccupanciesForDay
)

export const unitPlannedOccupanciesForDayQuery = q.query(
  getUnitPlannedOccupanciesForDay
)

export const unitApplicationsQuery = q.query(getUnitApplications)

export const unitServiceApplicationsQuery = q.query(
  getUndecidedServiceApplications
)

export const unitGroupDetailsQuery = q.query(getUnitGroupDetails)

export const createGroupPlacementMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(createGroupPlacement, [
  unitGroupDetailsQuery.prefix,
  ({ unitId }) => unitNotificationsQuery({ daycareId: unitId })
])

export const deleteGroupPlacementMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(deleteGroupPlacement, [
  ({ unitId }) => unitNotificationsQuery({ daycareId: unitId }),
  unitGroupDetailsQuery.prefix
])

export const transferGroupMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(transferGroupPlacement, [
  ({ unitId }) => unitNotificationsQuery({ daycareId: unitId }),
  unitGroupDetailsQuery.prefix
])

export const createGroupMutation = q.mutation(createGroup, [
  ({ daycareId }) => unitNotificationsQuery({ daycareId })
])

export const unitGroupsQuery = q.query(getGroups)

export const updateGroupMutation = q.mutation(updateGroup, [
  ({ daycareId }) => unitNotificationsQuery({ daycareId }),
  unitGroupDetailsQuery.prefix
])

export const deleteGroupMutation = q.mutation(deleteGroup, [
  ({ daycareId }) => unitNotificationsQuery({ daycareId }),
  unitGroupDetailsQuery.prefix
])

export const unitSpeculatedOccupancyRatesQuery = q.query(
  getOccupancyPeriodsSpeculated
)

export const createUnitMutation = q.mutation(createDaycare)

export const updateUnitMutation = q.mutation(updateDaycare, [
  ({ daycareId }) => unitQuery({ daycareId })
])

export const updateUnitClosingDateMutation = q.mutation(updateUnitClosingDate, [
  ({ unitId }) => unitQuery({ daycareId: unitId })
])

export const unitAttendanceReservationsQuery = q.query(
  getAttendanceReservations
)

export const postReservationsMutation = q.mutation(postReservations, [
  unitAttendanceReservationsQuery.prefix
])

export const createBackupCareMutation = q.mutation(createBackupCare)

export const updateBackupCareMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(updateBackupCare, [
  ({ unitId }) => unitNotificationsQuery({ daycareId: unitId }),
  unitGroupDetailsQuery.prefix
])

export const childDateExpectedAbsencesQuery = q.query(getExpectedAbsences)

export const upsertChildDatePresenceMutation = q.mutation(
  postChildDatePresence,
  [unitAttendanceReservationsQuery.prefix]
)

export const acceptPlacementProposalMutation = q.mutation(
  acceptPlacementProposal,
  [
    ({ unitId }) => unitNotificationsQuery({ daycareId: unitId }),
    ({ unitId }) => unitApplicationsQuery({ unitId })
  ]
)

export const respondToPlacementProposalMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(respondToPlacementProposal, [
  ({ unitId }) => unitApplicationsQuery({ unitId }),
  ({ unitId }) => unitNotificationsQuery({ daycareId: unitId })
])

export const openAttendanceQuery = q.query(getOpenGroupAttendance)
