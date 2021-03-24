// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { CareType } from 'lib-common/api-types/units/enums'
import { Translations } from '../localization'

export function formatCareType(t: Translations, type: CareType): string {
  switch (type) {
    case 'CENTRE':
    case 'FAMILY':
    case 'GROUP_FAMILY':
      return t.map.careTypes.DAYCARE
    case 'PRESCHOOL':
      return t.common.unit.careTypes.PRESCHOOL
    case 'PREPARATORY_EDUCATION':
      return t.common.unit.careTypes.PREPARATORY_EDUCATION
    case 'CLUB':
      return t.common.unit.careTypes.CLUB
  }
}

export function formatCareTypes(t: Translations, types: CareType[]): string[] {
  return types
    .sort((a, b) => {
      if (a === 'CENTRE') return -1
      if (b === 'CENTRE') return 1
      if (a === 'PREPARATORY_EDUCATION') return 1
      if (b === 'PREPARATORY_EDUCATION') return -1
      return 0
    })
    .map((careType) => formatCareType(t, careType))
}
