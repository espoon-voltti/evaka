// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import type { EmployeeCustomizations } from 'lib-customizations/types'
import { employeeConfig } from './appConfigs'
import Logo from './assets/EspooLogoPrimary.svg'
import featureFlags from './featureFlags'

const customizations: EmployeeCustomizations = {
  appConfig: employeeConfig,
  translations: {
    fi: {}
  },
  vasuTranslations: {
    FI: {},
    SV: {}
  },
  cityLogo: <img src={Logo} alt="Espoo Logo" data-qa="footer-city-logo" />,
  featureFlags,
  placementTypes: [
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'DAYCARE',
    'DAYCARE_PART_TIME',
    'PREPARATORY',
    'PREPARATORY_DAYCARE',
    'CLUB',
    'TEMPORARY_DAYCARE',
    'TEMPORARY_DAYCARE_PART_DAY'
  ],
  assistanceMeasures: [
    'SPECIAL_ASSISTANCE_DECISION',
    'INTENSIFIED_ASSISTANCE',
    'EXTENDED_COMPULSORY_EDUCATION',
    'CHILD_SERVICE',
    'CHILD_ACCULTURATION_SUPPORT',
    'TRANSPORT_BENEFIT'
  ],
  placementPlanRejectReasons: ['REASON_1', 'REASON_2', 'OTHER'],
  unitProviderTypes: [
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'MUNICIPAL_SCHOOL',
    'PRIVATE_SERVICE_VOUCHER',
    'EXTERNAL_PURCHASED'
  ]
}

export default customizations
