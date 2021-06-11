// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from '../../lib-common/json'
import { UUID } from './index'

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
