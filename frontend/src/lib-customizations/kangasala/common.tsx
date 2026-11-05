// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CommonCustomizations } from 'lib-customizations/types'

import theme from './kangasala-theme'

const customizations: CommonCustomizations = {
  theme,
  placementTypes: [
    'DAYCARE',
    'TEMPORARY_DAYCARE',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PRESCHOOL_DAYCARE_ONLY',
    'PREPARATORY',
    'PREPARATORY_DAYCARE',
    'PREPARATORY_DAYCARE_ONLY',
    'CLUB',
    'SCHOOL_SHIFT_CARE'
  ]
}

export default customizations
