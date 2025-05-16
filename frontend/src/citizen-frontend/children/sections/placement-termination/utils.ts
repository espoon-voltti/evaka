// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  PlacementType,
  TerminatablePlacementGroup
} from 'lib-common/generated/api-types/placement'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'

type TerminatedPlacementInfoType =
  | { type: 'placement'; placementType: PlacementType }
  | { type: 'connectedDaycare' }

export function terminatedPlacementInfo(
  placementGroup: TerminatablePlacementGroup
): {
  type: TerminatedPlacementInfoType
  unitId: DaycareId
  unitName: string
  lastDay: LocalDate
} {
  // If a connected daycare placement is terminated and the preschool/preparatory placement continues,
  // show the connected daycare placement as terminated
  const terminatedConnectedDaycare = placementGroup.placements.find(
    (p) =>
      p.terminationRequestedDate !== null &&
      (p.type === 'PRESCHOOL_DAYCARE' || p.type === 'PREPARATORY_DAYCARE')
  )
  if (terminatedConnectedDaycare) {
    const ongoingType =
      terminatedConnectedDaycare.type === 'PRESCHOOL_DAYCARE'
        ? 'PRESCHOOL'
        : 'PREPARATORY'
    if (
      placementGroup.placements.find(
        (p) =>
          p.terminationRequestedDate === null &&
          p.type === ongoingType &&
          p.startDate.isEqual(terminatedConnectedDaycare.endDate.addDays(1))
      )
    ) {
      return {
        type: { type: 'connectedDaycare' },
        unitId: placementGroup.unitId,
        unitName: placementGroup.unitName,
        lastDay: terminatedConnectedDaycare.endDate
      }
    }
  }

  const terminatedAdditional = placementGroup.additionalPlacements.find(
    (p) => p.terminationRequestedDate !== null
  )
  const type: TerminatedPlacementInfoType = terminatedAdditional
    ? { type: 'connectedDaycare' }
    : { type: 'placement', placementType: placementGroup.type }
  const lastDay = terminatedAdditional
    ? terminatedAdditional.endDate
    : placementGroup.endDate

  return {
    type,
    unitId: placementGroup.unitId,
    unitName: placementGroup.unitName,
    lastDay
  }
}
