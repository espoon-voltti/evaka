// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AttendanceStatus } from 'lib-common/generated/api-types/attendance'
import { AbsenceCategory } from 'lib-common/generated/api-types/daycare'
import { PlacementType } from 'lib-common/generated/enums'
import { Translations } from '../state/i18n'

export type ChildAttendanceUIState =
  | 'coming'
  | 'present'
  | 'departed'
  | 'absent'

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
      switch (category) {
        case 'BILLABLE':
          return i18n.absences.careTypes.PRESCHOOL_DAYCARE
        case 'NONBILLABLE':
          return i18n.absences.careTypes.PRESCHOOL
      }
    // eslint-disable-next-line no-fallthrough
    case 'SCHOOL_SHIFT_CARE':
      return i18n.absences.careTypes.SCHOOL_SHIFT_CARE
  }
}
