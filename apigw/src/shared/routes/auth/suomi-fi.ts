// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import createSamlRouter from './saml'
import createSuomiFiStrategy from '../../auth/suomi-fi-saml'
import { SessionType } from '../../session'

export function createAuthEndpoints(sessionType: SessionType) {
  return createSamlRouter({
    strategyName: 'suomifi',
    strategy: createSuomiFiStrategy(),
    sessionType,
    pathIdentifier: 'saml'
  })
}
