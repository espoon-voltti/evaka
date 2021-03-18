// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/local-date'
import { Translations } from '../state/i18n'

export interface User {
  id: string
  name: string
  unitId?: string // only mobile devices have this
}

export type AdRole =
  | 'SERVICE_WORKER'
  | 'UNIT_SUPERVISOR'
  | 'STAFF'
  | 'FINANCE_ADMIN'
  | 'ADMIN'
  | 'DIRECTOR'
  | 'SPECIAL_EDUCATION_TEACHER'

export type PlacementType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY'
  | 'PREPARATORY_DAYCARE'

export type AbsenceType =
  | 'OTHER_ABSENCE'
  | 'SICKLEAVE'
  | 'UNKNOWN_ABSENCE'
  | 'PLANNED_ABSENCE'
  | 'TEMPORARY_RELOCATION'
  | 'TEMPORARY_VISITOR'
  | 'PARENTLEAVE'
  | 'FORCE_MAJEURE'
  | 'PRESENCE'

export const AbsenceTypes: AbsenceType[] = [
  'OTHER_ABSENCE',
  'SICKLEAVE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'PARENTLEAVE',
  'FORCE_MAJEURE',
  'PRESENCE'
]

export type CareType =
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'DAYCARE_5YO_FREE'
  | 'DAYCARE'
  | 'CLUB'

export function formatCareType(
  careType: CareType,
  placementType: PlacementType,
  entitledToFreeFiveYearsOldDaycare: boolean,
  i18n: Translations
) {
  const isPreparatory =
    placementType === 'PREPARATORY' || placementType === 'PREPARATORY_DAYCARE'

  if (careType === 'DAYCARE' && entitledToFreeFiveYearsOldDaycare)
    return i18n.common.types.DAYCARE_5YO_PAID

  if (isPreparatory && careType === 'PRESCHOOL')
    return i18n.common.types.PREPARATORY_EDUCATION

  return i18n.absences.careTypes[careType]
}

export interface AbsenceBackupCare {
  childId: string
  date: LocalDate
}
