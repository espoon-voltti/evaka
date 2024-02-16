// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export const defaultAbsenceType = 'SICKLEAVE'
export const defaultAbsenceCategories: AbsenceCategory[] = []

export const absenceCategories: AbsenceCategory[] = ['NONBILLABLE', 'BILLABLE']

type AbsenceTypeWithBackupCare = AbsenceType | 'TEMPORARY_RELOCATION'

export interface CellPart {
  childId: UUID
  date: LocalDate
  absenceType: AbsenceTypeWithBackupCare | undefined
  category: AbsenceCategory
  position: 'left' | 'right'
}
