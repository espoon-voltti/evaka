// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EmployeeCustomizations } from 'lib-customizations/types'
import EspooLogo from './EspooLogo.png'
import featureFlags from './featureFlags'

const customizations: EmployeeCustomizations = {
  translations: {
    fi: {}
  },
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  },
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
  placementPlanRejectReasons: ['REASON_1', 'REASON_2', 'OTHER']
}

export default customizations
