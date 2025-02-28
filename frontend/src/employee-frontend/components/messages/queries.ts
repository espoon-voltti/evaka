// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PersonId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  archiveThread,
  createMessagePreflightCheck,
  deleteDraftMessage,
  getFinanceMessagesWithPerson,
  initDraftMessage,
  replyToThread,
  updateDraftMessage
} from '../../generated/api-clients/messaging'

const q = new Queries()

export const createMessagePreflightCheckQuery = q.query(
  createMessagePreflightCheck
)

export const replyToThreadMutation = q.mutation(replyToThread)

export const initDraftMutation = q.mutation(initDraftMessage, [])

export const saveDraftMutation = q.mutation(updateDraftMessage, [])

export const deleteDraftMutation = q.mutation(deleteDraftMessage, [])

export const financeThreadsQuery = q.query(getFinanceMessagesWithPerson)

export const deleteFinanceThreadMutation = q.parametricMutation<{
  id: PersonId
}>()(archiveThread, [({ id }) => financeThreadsQuery({ personId: id })])
