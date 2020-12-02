// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import createSamlRouter from './saml'
import createEspooAdStrategy from '../../auth/espoo-ad-saml'
import { SessionType } from '../../session'
import createKeycloakSamlStrategy from '../../auth/keycloak-saml'

export function createKeycloakAuthEndpoints(sessionType: SessionType) {
  return createSamlRouter({
    strategyName: 'keycloak',
    strategy: createKeycloakSamlStrategy(),
    sessionType
  })
}
