// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Env = 'staging' | 'prod'

export const env = (): Env | 'default' => {
  if (typeof window !== 'undefined') {
    if (window.location.host === 'varhaiskasvatus.tampere.fi') {
      return 'prod'
    }

    if (window.location.host === 'staging-varhaiskasvatus.tampere.fi') {
      return 'staging'
    }
  }

  return 'default'
}
