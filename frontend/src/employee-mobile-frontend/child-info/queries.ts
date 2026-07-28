// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getSensitiveInfo } from '../generated/api-clients/sensitive'

const q = new Queries()

export const childSensitiveInfoQuery = q.query(getSensitiveInfo, {
  // Drop this PIN-gated sensitive data from the cache as soon as the view
  // unmounts so a later PIN session can't be shown it.
  gcTime: 0
})
