// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import theme from 'lib-common/themes/espoo-theme'
import type { CommonCustomizations } from 'lib-customizations/types'

const customizations: CommonCustomizations = {
  theme,
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
  ]
}

export default customizations
