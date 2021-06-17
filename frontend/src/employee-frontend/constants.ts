// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from './types'
import { AssistanceBasis } from './types/child'

export const EVAKA_START = LocalDate.of(2020, 3, 1)

export const MAX_DATE = LocalDate.of(2099, 12, 31)

export const ENTER_PRESS = 13

export const CHILD_AGE = 18

export function getEmployeeUrlPrefix(): string {
  const isLocalMultiPortEnv = window.location.host.includes(':9093')
  return isLocalMultiPortEnv ? 'http://localhost:9093' : ''
}

export function getMobileUrlPrefix(): string {
  const isLocalMultiPortEnv = window.location.host.includes(':9093')
  return isLocalMultiPortEnv ? 'http://localhost:9095' : ''
}

export function isNotProduction(): boolean {
  return (
    window.location.host.includes(':9093') ||
    window.location.host.includes('localhost') ||
    window.location.host.includes('staging')
  )
}

export function isPilotUnit(unitId: UUID): boolean {
  // Tuomarilan pk, Kartanonpuiston pk, Ruusulinnan pk, Säterin kerho, Kartanonvintin rppk, Storängens daghem och förskola, Järvenperän pk, Luhtaniityn pk, Suvituuli rppk, Kesäheinä rppk, Sunakatti pk
  // The ones starting with letters are all different "groups" of Säterin kerho
  const pilotUnitIds = [
    '2dd63494-788e-11e9-bd69-b3441c1df64b',
    '2ddc2390-788e-11e9-bdc6-93f4e99145c6',
    '2ddf8300-788e-11e9-bdf9-f7700f85506b',
    '2df11214-788e-11e9-bef8-a30aaf67b874',
    '2dff766a-788e-11e9-bfea-27258547b1dc',
    '2dcf4530-788e-11e9-bd15-9b874fded9b9',
    '2dd4b664-788e-11e9-bd51-4f6bec28e68b',
    '2def27b0-788e-11e9-bed7-d38b0305d689',
    '2deef8f8-788e-11e9-bed4-c3aec9f586af',
    '40e834d8-3152-11ea-9ad7-1761bdb4e741',
    'e6bae2a4-5c43-11ea-9921-9ff01014d16d',
    'e6d2b190-5c43-11ea-9922-87e29b5189e3',
    'd5cce2d8-2abe-11e9-8db0-4f32672b511c',
    'd5cd0326-2abe-11e9-8db1-93687938341f',
    'd5cd22f2-2abe-11e9-8db2-57da70f23901',
    'e6e37750-5c43-11ea-9923-df804c6c209c'
  ]

  return pilotUnitIds.includes(unitId)
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
