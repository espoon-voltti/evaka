// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/src/local-date'
import {
  AssistanceActionType,
  AssistanceBasis,
  AssistanceMeasure
} from '~types/child'

export const DATE_FORMAT_DEFAULT = 'dd.MM.yyyy'
export const DATE_FORMAT_DATE_TIME = 'dd.MM.yyyy HH:mm'
export const DATE_FORMAT_ISO = 'yyyy-MM-dd'
export const DATE_FORMATS_PARSED = [
  'dd.MM.yyyy',
  'dd.MM.yy',
  'd.M.yyyy',
  'd.M.yy',
  'ddMMyyyy',
  'ddMMyy'
]

export const EVAKA_START = LocalDate.of(2020, 3, 1)

export const MAX_DATE = LocalDate.of(2099, 12, 31)

export const ENTER_PRESS = 13

export const CHILD_AGE = 18

export function getEmployeeUrlPrefix(): string {
  const isLocalMultiPortEnv = window.location.host.includes(':9093')
  return isLocalMultiPortEnv ? 'http://localhost:9093' : ''
}

export function isNotProduction(): boolean {
  return (
    window.location.host.includes(':9093') ||
    window.location.host.includes('staging')
  )
}

export const ASSISTANCE_BASIS_LIST: AssistanceBasis[] = [
  'AUTISM',
  'DEVELOPMENTAL_DISABILITY_1',
  'DEVELOPMENTAL_DISABILITY_2',
  'FOCUS_CHALLENGE',
  'LINGUISTIC_CHALLENGE',
  'DEVELOPMENT_MONITORING',
  'DEVELOPMENT_MONITORING_PENDING',
  'MULTI_DISABILITY',
  'LONG_TERM_CONDITION',
  'REGULATION_SKILL_CHALLENGE',
  'DISABILITY',
  'OTHER'
]

export const ASSISTANCE_ACTION_TYPE_LIST: AssistanceActionType[] = [
  'ASSISTANCE_SERVICE_CHILD',
  'ASSISTANCE_SERVICE_UNIT',
  'SMALLER_GROUP',
  'SPECIAL_GROUP',
  'PERVASIVE_VEO_SUPPORT',
  'RESOURCE_PERSON',
  'RATIO_DECREASE',
  'PERIODICAL_VEO_SUPPORT',
  'OTHER'
]

export const ASSISTANCE_MEASURE_LIST: AssistanceMeasure[] = [
  'SPECIAL_ASSISTANCE_DECISION',
  'INTENSIFIED_ASSISTANCE',
  'EXTENDED_COMPULSORY_EDUCATION',
  'CHILD_SERVICE',
  'CHILD_ACCULTURATION_SUPPORT',
  'TRANSPORT_BENEFIT'
]
