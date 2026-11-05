{
  /*
SPDX-FileCopyrightText: 2021 City of Turku

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import type { CommonCustomizations } from 'lib-customizations/types'

import theme from './turku-theme'

const customizations: CommonCustomizations = {
  theme,
  placementTypes: [
    'DAYCARE',
    'DAYCARE_PART_TIME',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PREPARATORY',
    'PREPARATORY_DAYCARE',
    'CLUB',
    'TEMPORARY_DAYCARE'
  ]
}

export default customizations
