// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export interface PartialFeeAlteration {
  personId: UUID
  type: FeeAlterationType
  amount: number
  isAbsolute: boolean
  validFrom: LocalDate
  validTo: LocalDate | null
  notes: string
}

export interface FeeAlteration extends PartialFeeAlteration {
  id: UUID
  notes: string
  updatedAt: HelsinkiDateTime
  updatedBy: UUID
}

export const feeAlterationTypes = ['DISCOUNT', 'INCREASE', 'RELIEF'] as const

export type FeeAlterationType = typeof feeAlterationTypes[number]
