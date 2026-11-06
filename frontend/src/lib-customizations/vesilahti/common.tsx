// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CommonCustomizations } from 'lib-customizations/types'

import featureFlags from './featureFlags'
import theme from './vesilahti-theme'

const customizations: CommonCustomizations = {
  theme,
  placementTypes: [
    'DAYCARE',
    'TEMPORARY_DAYCARE',
    ...(featureFlags.preschool ? (['PRESCHOOL_DAYCARE'] as const) : []),
    'PRESCHOOL_DAYCARE_ONLY',
    'PREPARATORY_DAYCARE_ONLY',
    'SCHOOL_SHIFT_CARE'
  ]
}

export default customizations
