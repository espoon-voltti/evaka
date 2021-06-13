// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from '../../lib-common/json'
import { UUID } from './index'
import FiniteDateRange from '../../lib-common/finite-date-range'
import LocalDate from '../../lib-common/local-date'
import { VasuContent } from '../api/child/vasu'

export type VasuDocumentState = 'DRAFT' | 'CREATED' | 'REVIEWED' | 'CLOSED'

export interface VasuDocumentSummary {
  name: string
  state: VasuDocumentState
  modifiedAt: Date
  publishedAt?: Date
  id: UUID
}
export const deserializeVasuDocumentSummary = ({
  modifiedAt,
  publishedAt,
  ...rest
}: JsonOf<VasuDocumentSummary>): VasuDocumentSummary => ({
  ...rest,
  modifiedAt: new Date(modifiedAt),
  publishedAt: publishedAt ? new Date(publishedAt) : undefined
})

export interface VasuTemplateSummary {
  id: UUID
  name: string
  valid: FiniteDateRange
}
export const deserializeVasuTemplateSummary = ({
  valid,
  ...rest
}: JsonOf<VasuTemplateSummary>): VasuTemplateSummary => ({
  ...rest,
  valid: FiniteDateRange.parseJson(valid)
})

export interface VasuTemplate {
  id: UUID
  name: string
  valid: FiniteDateRange
  content: VasuContent
}
export const deserializeVasuTemplate = ({
  valid,
  ...rest
}: JsonOf<VasuTemplate>): VasuTemplate => ({
  ...rest,
  valid: new FiniteDateRange(
    LocalDate.parseIso(valid.start),
    LocalDate.parseIso(valid.end)
  )
})
