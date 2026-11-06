{
  /*
SPDX-FileCopyrightText: 2021 City of Tampere

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import type { CommonCustomizations } from 'lib-customizations/types'

import theme from './tampere-theme'

const customizations: CommonCustomizations = {
  theme,
  placementTypes: [
    'DAYCARE',
    'DAYCARE_PART_TIME',
    'TEMPORARY_DAYCARE',
    'TEMPORARY_DAYCARE_PART_DAY',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PRESCHOOL_DAYCARE_ONLY',
    'CLUB',
    'SCHOOL_SHIFT_CARE'
  ]
}

export default customizations
