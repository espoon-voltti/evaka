// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationType,
  PersonApplicationSummary
} from 'lib-common/generated/api-types/application'

export type InferredApplicationType =
  | ApplicationType
  | 'PRESCHOOL_WITH_DAYCARE'
  | 'PREPARATORY_WITH_DAYCARE'
  | 'PREPARATORY_EDUCATION'

export function inferApplicationType(
  application: PersonApplicationSummary
): InferredApplicationType {
  const baseType = application.type
  if (baseType !== 'PRESCHOOL') return baseType
  else if (application.connectedDaycare && !application.preparatoryEducation) {
    return 'PRESCHOOL_WITH_DAYCARE'
  } else if (application.connectedDaycare && application.preparatoryEducation) {
    return 'PREPARATORY_WITH_DAYCARE'
  } else if (
    !application.connectedDaycare &&
    application.preparatoryEducation
  ) {
    return 'PREPARATORY_EDUCATION'
  } else return 'PRESCHOOL'
}
