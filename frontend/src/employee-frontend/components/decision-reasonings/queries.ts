// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createGenericReasoning,
  createIndividualReasoning,
  deleteGenericReasoning,
  getGenericReasonings,
  getIndividualReasonings,
  removeGenericReasoning,
  removeIndividualReasoning,
  updateGenericReasoning
} from '../../generated/api-clients/decision'

const q = new Queries()

export const genericReasoningsQuery = q.query(getGenericReasonings)

export const individualReasoningsQuery = q.query(getIndividualReasonings)

export const createGenericReasoningMutation = q.mutation(
  createGenericReasoning,
  [genericReasoningsQuery.prefix]
)

export const updateGenericReasoningMutation = q.mutation(
  updateGenericReasoning,
  [genericReasoningsQuery.prefix]
)

export const deleteGenericReasoningMutation = q.mutation(
  deleteGenericReasoning,
  [genericReasoningsQuery.prefix]
)

export const removeGenericReasoningMutation = q.mutation(
  removeGenericReasoning,
  [genericReasoningsQuery.prefix]
)

export const createIndividualReasoningMutation = q.mutation(
  createIndividualReasoning,
  [individualReasoningsQuery.prefix]
)

export const removeIndividualReasoningMutation = q.mutation(
  removeIndividualReasoning,
  [individualReasoningsQuery.prefix]
)
