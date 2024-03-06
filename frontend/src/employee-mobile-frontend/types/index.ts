// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AbsenceCategory } from 'lib-common/generated/api-types/absence'
import { AttendanceStatus } from 'lib-common/generated/api-types/attendance'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { INVALID } from 'lib-common/useRequiredParams'

import { Translations } from '../common/i18n'

export type ChildAttendanceUIState =
  | 'coming'
  | 'present'
  | 'departed'
  | 'absent'

export function parseChildAttendanceUiState(state: string) {
  return state === 'coming' ||
    state === 'present' ||
    state === 'departed' ||
    state === 'absent'
    ? state
    : INVALID
}

export function mapChildAttendanceUIState(
  uiState: ChildAttendanceUIState
): AttendanceStatus {
  switch (uiState) {
    case 'coming':
      return 'COMING'
    case 'present':
      return 'PRESENT'
    case 'departed':
      return 'DEPARTED'
    case 'absent':
      return 'ABSENT'
  }
}

export function formatCategory(
  category: AbsenceCategory,
  placementType: PlacementType,
  i18n: Translations
) {
  switch (placementType) {
    case 'CLUB':
      return i18n.absences.careTypes.CLUB
    case 'DAYCARE':
    case 'DAYCARE_PART_TIME':
    case 'PRESCHOOL_DAYCARE_ONLY':
    case 'PREPARATORY_DAYCARE_ONLY':
    case 'TEMPORARY_DAYCARE':
    case 'TEMPORARY_DAYCARE_PART_DAY':
      return i18n.absences.careTypes.DAYCARE
    case 'DAYCARE_FIVE_YEAR_OLDS':
    case 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS':
      switch (category) {
        case 'BILLABLE':
          return i18n.common.types.DAYCARE_5YO_PAID
        case 'NONBILLABLE':
          return i18n.absences.careTypes.DAYCARE_5YO_FREE
      }
    // eslint-disable-next-line no-fallthrough
    case 'PREPARATORY':
    case 'PREPARATORY_DAYCARE':
      switch (category) {
        case 'BILLABLE':
          return i18n.absences.careTypes.PRESCHOOL_DAYCARE
        case 'NONBILLABLE':
          return i18n.common.types.PREPARATORY_EDUCATION
      }
    // eslint-disable-next-line no-fallthrough
    case 'PRESCHOOL':
    case 'PRESCHOOL_DAYCARE':
    case 'PRESCHOOL_CLUB':
      switch (category) {
        case 'BILLABLE':
          return placementType === 'PRESCHOOL_CLUB'
            ? i18n.absences.careTypes.PRESCHOOL_CLUB
            : i18n.absences.careTypes.PRESCHOOL_DAYCARE
        case 'NONBILLABLE':
          return i18n.absences.careTypes.PRESCHOOL
      }
    // eslint-disable-next-line no-fallthrough
    case 'SCHOOL_SHIFT_CARE':
      return i18n.absences.careTypes.SCHOOL_SHIFT_CARE
  }
}
