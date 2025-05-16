// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Attachment } from 'lib-common/generated/api-types/attachment'
import type {
  FeeAlteration,
  FeeAlterationType
} from 'lib-common/generated/api-types/invoicing'
import type {
  FeeAlterationId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'

export type PartialFeeAlteration = Omit<
  FeeAlteration,
  'modifiedAt' | 'modifiedBy'
>

export interface FeeAlterationForm {
  amount: number
  attachments: Attachment[]
  id: FeeAlterationId | null
  isAbsolute: boolean
  notes: string
  personId: PersonId
  type: FeeAlterationType
  validFrom: LocalDate | null
  validTo: LocalDate | null
}
