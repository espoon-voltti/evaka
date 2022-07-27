// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import type { Unit } from '../api/daycare'

interface Child {
  id: UUID
  firstName: string
  lastName: string
  dob: LocalDate
}

export interface PlacementDraft {
  child: Child
  preferredUnits: Unit[]
  type: PlacementType
  period: FiniteDateRange
  preschoolDaycarePeriod?: FiniteDateRange
  placements: PlacementDraftPlacement[]
  guardianHasRestrictedDetails: boolean
}

export interface DaycarePlacementPlan {
  unitId?: UUID
  period?: FiniteDateRange
  preschoolDaycarePeriod?: FiniteDateRange
}

export interface PlacementDraftPlacement {
  id: UUID
  type: PlacementType
  childId: UUID
  unit: Unit
  startDate: LocalDate
  endDate: LocalDate
  overlap?: boolean
}
