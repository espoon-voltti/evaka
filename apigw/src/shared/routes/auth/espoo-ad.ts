// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import createSamlRouter from './saml'
import createEspooAdStrategy from '../../auth/espoo-ad-saml'
import { SessionType } from '../../session'
import { Strategy } from 'passport'

export function createAuthEndpoints(
  strategyName: string,
  strategy: Strategy,
  sessionType: SessionType
) {
  return createSamlRouter({
    strategyName,
    strategy: createEspooAdStrategy(),
    sessionType
  })
}
