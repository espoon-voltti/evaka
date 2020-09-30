// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '~types/index'

interface Unit {
  id: UUID
  name: string
}

interface Child {
  id: UUID
  firstName: string
  lastName: string
  dob: LocalDate
}

export interface Period {
  start: LocalDate
  end: LocalDate
}

export type PlacementType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY'
  | 'PREPARATORY_DAYCARE'

export interface PlacementDraft {
  child: Child
  preferredUnits: Unit[]
  type: PlacementType
  period: Period
  preschoolDaycarePeriod?: Period
  placements: PlacementDraftPlacement[]
}

export interface DaycarePlacementPlan {
  unitId?: UUID
  period?: Period
  preschoolDaycarePeriod?: Period
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
