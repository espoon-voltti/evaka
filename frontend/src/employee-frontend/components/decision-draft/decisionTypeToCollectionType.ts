// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  DecisionReasoningCollectionType,
  DecisionType
} from 'lib-common/generated/api-types/decision'

export function decisionTypeToCollectionType(
  type: DecisionType
): DecisionReasoningCollectionType {
  switch (type) {
    case 'DAYCARE':
    case 'DAYCARE_PART_TIME':
    case 'CLUB':
    case 'PRESCHOOL_DAYCARE':
    case 'PRESCHOOL_CLUB':
      return 'DAYCARE'
    case 'PRESCHOOL':
    case 'PREPARATORY_EDUCATION':
      return 'PRESCHOOL'
  }
}
