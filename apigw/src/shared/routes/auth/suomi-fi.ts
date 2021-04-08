// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import createSamlRouter from './saml'
import createSuomiFiStrategy from '../../auth/suomi-fi-saml'
import { SessionType } from '../../session'
import createEvakaCustomerSamlStrategy from '../../auth/customer-saml'

export function createSuomiFiAuthEndpoints(sessionType: SessionType) {
  return createSamlRouter({
    strategyName: 'suomifi',
    strategy: createSuomiFiStrategy(),
    sessionType,
    pathIdentifier: 'saml'
  })
}

export function createKeycloakAuthEndpoints(sessionType: SessionType) {
  return createSamlRouter({
    strategyName: 'evaka-customer',
    strategy: createEvakaCustomerSamlStrategy(),
    sessionType,
    pathIdentifier: 'evaka-customer'
  })
}
