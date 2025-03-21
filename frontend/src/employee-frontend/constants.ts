// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

export const EVAKA_START = LocalDate.of(2020, 3, 1)

export const MAX_DATE = LocalDate.of(2099, 12, 31)

export const PROFILE_AGE_THRESHOLD_DEFAULT = 13

export function getEmployeeUrlPrefix(): string {
  const isLocalMultiPortEnv = window.location.host.includes(':9093')
  return isLocalMultiPortEnv ? 'http://localhost:9093' : ''
}

export function getMobileUrlPrefix(): string {
  const isLocalMultiPortEnv = window.location.host.includes(':9093')
  return isLocalMultiPortEnv ? 'http://localhost:9095' : ''
}
