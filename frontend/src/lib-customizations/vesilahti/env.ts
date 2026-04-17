// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Env = 'prod'

export const env = (): Env | 'default' => {
  if (typeof window !== 'undefined') {
    if (window.location.host === 'evaka.vesilahti.fi') {
      return 'prod'
    }
  }
  return 'default'
}
