// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '../types'

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
  updatedAt: Date
  updatedBy: UUID
}

export const feeAlterationTypes = ['DISCOUNT', 'INCREASE', 'RELIEF'] as const

export type FeeAlterationType = typeof feeAlterationTypes[number]
