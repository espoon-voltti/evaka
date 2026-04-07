// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Env = 'prod'

export const env = (): Env | 'default' => {
  if (typeof window !== 'undefined') {
    if (window.location.host === 'evaka.hameenkyro.fi') {
      return 'prod'
    }
  }
  return 'default'
}
