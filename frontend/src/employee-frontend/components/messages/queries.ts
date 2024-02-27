// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation } from 'lib-common/query'

import { replyToThread } from '../../generated/api-clients/messaging'

export const replyToThreadMutation = mutation({
  api: replyToThread
})
