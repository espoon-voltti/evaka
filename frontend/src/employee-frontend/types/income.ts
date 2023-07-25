// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Attachment } from 'lib-common/api-types/attachment'
import { IncomeEffect, IncomeValue } from 'lib-common/api-types/income'
import { Action } from 'lib-common/generated/action'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

// eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
export type IncomeId = 'new' | UUID

export type IncomeFields = Partial<Record<string, IncomeValue>>

export interface IncomeBody {
  effect: IncomeEffect
  data: IncomeFields
  isEntrepreneur: boolean
  worksAtECHA: boolean
  validFrom: LocalDate
  validTo?: LocalDate
  notes: string
  attachments: Attachment[]
}

export interface Income extends IncomeBody {
  id: UUID
  personId: UUID
  total: number
  totalIncome: number
  totalExpenses: number
  updatedAt: HelsinkiDateTime
  updatedBy: string
  applicationId: UUID | null
  notes: string
}

export interface IncomeWithPermittedActions {
  data: Income
  permittedActions: Action.Income[]
}

export interface IncomeOption {
  value: string
  nameFi: string
  multiplier: number
  withCoefficient: boolean
  isSubType: boolean
}
