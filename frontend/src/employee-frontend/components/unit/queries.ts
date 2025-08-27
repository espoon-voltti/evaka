// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  acceptPlacementProposal,
  getUnitApplications,
  respondToPlacementProposal
} from '../../generated/api-clients/application'
import { getOpenGroupAttendance } from '../../generated/api-clients/attendance'
import {
  addFullAclForRole,
  createDaycare,
  createGroup,
  createTemporaryEmployee,
  deleteEarlyChildhoodEducationSecretary,
  deleteGroup,
  deleteScheduledAcl,
  deleteSpecialEducationTeacher,
  deleteStaff,
  deleteTemporaryEmployee,
  deleteTemporaryEmployeeAcl,
  deleteUnitSupervisor,
  getDaycare,
  getDaycareAcl,
  getDaycares,
  getGroups,
  getScheduledDaycareAcl,
  getTemporaryEmployee,
  getTemporaryEmployees,
  getUnitGroupDetails,
  getUnitNotifications,
  getUnitServiceWorkerNote,
  reactivateTemporaryEmployee,
  setUnitServiceWorkerNote,
  updateDaycare,
  updateGroup,
  updateGroupAclWithOccupancyCoefficient,
  updateTemporaryEmployee,
  updateUnitClosingDate
} from '../../generated/api-clients/daycare'
import {
  getNekkuUnitNumbers,
  nekkuManualOrder
} from '../../generated/api-clients/nekku'
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
  getOngoingChildAttendance,
  getExpectedAbsences,
  postChildDatePresence,
  postReservations
} from '../../generated/api-clients/reservations'
import { getUndecidedServiceApplications } from '../../generated/api-clients/serviceneed'
import { getPreschoolTerms } from '../../generated/api-clients/daycare'

const q = new Queries()

export const daycaresQuery = q.query(getDaycares)

export const daycareQuery = q.query(getDaycare)

export const unitAclQuery = q.query(getDaycareAcl)

export const unitScheduledAclQuery = q.query(getScheduledDaycareAcl)

export const temporaryEmployeeQuery = q.query(getTemporaryEmployee)

export const temporaryEmployeesQuery = q.query(getTemporaryEmployees)

export const addFullAclForRoleMutation = q.mutation(addFullAclForRole, [
  ({ unitId }) => unitAclQuery({ unitId }),
  ({ unitId }) => unitScheduledAclQuery({ unitId })
])
export const createTemporaryEmployeeMutation = q.mutation(
  createTemporaryEmployee,
  [
    ({ unitId }) => unitAclQuery({ unitId }),
    ({ unitId }) => temporaryEmployeesQuery({ unitId })
  ]
)
export const updateTemporaryEmployeeMutation = q.mutation(
  updateTemporaryEmployee,
  [
    ({ unitId }) => unitAclQuery({ unitId }),
    ({ unitId }) => temporaryEmployeesQuery({ unitId }),
    ({ unitId, employeeId }) => temporaryEmployeeQuery({ unitId, employeeId })
  ]
)
export const deleteTemporaryEmployeeMutation = q.mutation(
  deleteTemporaryEmployee,
  [
    ({ unitId }) => temporaryEmployeesQuery({ unitId }),
    ({ unitId, employeeId }) => temporaryEmployeeQuery({ unitId, employeeId })
  ]
)
export const deleteTemporaryEmployeeAclMutation = q.mutation(
  deleteTemporaryEmployeeAcl,
  [
    ({ unitId }) => unitAclQuery({ unitId }),
    ({ unitId }) => temporaryEmployeesQuery({ unitId })
  ]
)
export const reactivateTemporaryEmployeeMutation = q.mutation(
  reactivateTemporaryEmployee,
  [({ unitId }) => unitAclQuery({ unitId })]
)

export const deleteUnitSupervisorMutation = q.mutation(deleteUnitSupervisor, [
  ({ unitId }) => unitAclQuery({ unitId })
])
export const deleteSpecialEducationTeacherMutation = q.mutation(
  deleteSpecialEducationTeacher,
  [({ unitId }) => unitAclQuery({ unitId })]
)
export const deleteEarlyChildhoodEducationSecretaryMutation = q.mutation(
  deleteEarlyChildhoodEducationSecretary,
  [({ unitId }) => unitAclQuery({ unitId })]
)
export const deleteStaffMutation = q.mutation(deleteStaff, [
  ({ unitId }) => unitAclQuery({ unitId })
])
export const deleteScheduledAclMutation = q.mutation(deleteScheduledAcl, [
  ({ unitId }) => unitScheduledAclQuery({ unitId })
])
export const updateGroupAclWithOccupancyCoefficientMutation = q.mutation(
  updateGroupAclWithOccupancyCoefficient,
  [({ unitId }) => unitAclQuery({ unitId })]
)

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

export const nekkuUnitNumbersQuery = q.query(getNekkuUnitNumbers)

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

export const getPreschoolTermsQuery = q.query(getPreschoolTerms)


export const transferGroupMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(transferGroupPlacement, [
  ({ unitId }) => unitNotificationsQuery({ daycareId: unitId }),
  unitGroupDetailsQuery.prefix
])

export const createGroupMutation = q.mutation(createGroup, [
  ({ daycareId }) => unitNotificationsQuery({ daycareId }),
  daycareQuery.prefix,
  unitGroupDetailsQuery.prefix
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
  ({ daycareId }) => daycareQuery({ daycareId })
])

export const updateUnitClosingDateMutation = q.mutation(updateUnitClosingDate, [
  ({ unitId }) => daycareQuery({ daycareId: unitId })
])

export const unitAttendanceReservationsQuery = q.query(
  getAttendanceReservations
)

export const ongoingChildAttendanceQuery = q.query(getOngoingChildAttendance)

export const postReservationsMutation = q.mutation(postReservations, [
  unitAttendanceReservationsQuery.prefix
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

export const nekkuManualOrderMutation = q.mutation(nekkuManualOrder)
