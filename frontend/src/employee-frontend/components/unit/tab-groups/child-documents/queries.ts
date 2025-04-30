// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createDocuments,
  getActiveTemplatesByGroupId,
  getNonCompletedChildDocumentChildIds
} from '../../../../generated/api-clients/document'

const q = new Queries()

export const getActiveTemplatesByGroupIdQuery = q.query(
  getActiveTemplatesByGroupId
)
export const getNonCompletedChildDocumentChildIdsQuery = q.query(
  getNonCompletedChildDocumentChildIds
)
export const createDocumentsMutation = q.mutation(createDocuments, [])
