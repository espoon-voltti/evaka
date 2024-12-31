// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { ignoreVoucherValueDecisionDrafts } from '../../generated/api-clients/invoicing'
import { getVoucherValueDecisionMetadata } from '../../generated/api-clients/process'

const q = new Queries()

export const voucherValueDecisionMetadataQuery = q.query(
  getVoucherValueDecisionMetadata
)

export const ignoreVoucherValueDecisionDraftsMutation = q.mutation(
  ignoreVoucherValueDecisionDrafts
)
