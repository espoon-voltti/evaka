// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AbsenceChild } from 'lib-common/api-types/child/absence'
import {
  AbsenceCareType,
  AbsenceType,
  PlacementType
} from 'lib-common/generated/enums'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { Translations } from '../state/i18n'

export const AbsenceTypes: AbsenceType[] = [
  'OTHER_ABSENCE',
  'SICKLEAVE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'PARENTLEAVE',
  'FORCE_MAJEURE'
]

export const defaultAbsenceType = 'SICKLEAVE'
export const defaultCareTypeCategory: CareTypeCategory[] = []

export type CareTypeCategory = 'BILLABLE' | 'NONBILLABLE'

export const CareTypeCategories: CareTypeCategory[] = [
  'NONBILLABLE',
  'BILLABLE'
]

export const billableCareTypes: AbsenceCareType[] = [
  'PRESCHOOL_DAYCARE',
  'DAYCARE'
]

export function formatCareType(
  careType: AbsenceCareType,
  placementType: PlacementType,
  i18n: Translations
) {
  const isPreparatory =
    placementType === 'PREPARATORY' || placementType === 'PREPARATORY_DAYCARE'

  if (
    careType === 'DAYCARE' &&
    fiveYearOldPlacementTypes.includes(placementType)
  ) {
    return i18n.common.types.DAYCARE_5YO_PAID
  }

  if (isPreparatory && careType === 'PRESCHOOL')
    return i18n.common.types.PREPARATORY_EDUCATION

  return i18n.absences.careTypes[careType]
}

const fiveYearOldPlacementTypes = [
  'DAYCARE_FIVE_YEAR_OLDS',
  'DAYCARE_PART_TIME_FIVE_YEAR_OLDS'
]

export interface Cell {
  id: UUID
  parts: CellPart[]
}

type AbsenceTypeWithBackupCare = AbsenceType | 'TEMPORARY_RELOCATION'

export interface CellPart {
  id: UUID
  childId: UUID
  date: LocalDate
  absenceType?: AbsenceTypeWithBackupCare
  careType: AbsenceCareType
  position: string
}

export const deserializeChild = (json: JsonOf<AbsenceChild>): AbsenceChild => ({
  ...json,
  child: {
    ...json.child,
    dateOfBirth: LocalDate.parseIso(json.child.dateOfBirth)
  },
  absences: Object.fromEntries(
    Object.entries(json.absences).map(([date, absences]) => [
      date,
      absences.map((absence) => ({
        ...absence,
        date: LocalDate.parseIso(absence.date),
        modifiedAt: new Date(absence.modifiedAt)
      }))
    ])
  )
})

export interface AbsenceBackupCare {
  childId: UUID
  date: LocalDate
}
