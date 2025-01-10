// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createMessagePreflightCheck,
  replyToThread
} from '../../generated/api-clients/messaging'

const q = new Queries()

export const createMessagePreflightCheckQuery = q.query(
  createMessagePreflightCheck
)

export const replyToThreadMutation = q.mutation(replyToThread)
