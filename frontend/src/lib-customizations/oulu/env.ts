// SPDX-FileCopyrightText: 2017-2022 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Env = 'staging' | 'prod'

export const env = (): Env | 'default' => {
  if (typeof window !== 'undefined') {
    if (window.location.host === 'varhaiskasvatus.ouka.fi') {
      return 'prod'
    }

    if (window.location.host === 'staging-varhaiskasvatus.ouka.fi') {
      return 'staging'
    }
  }
  return 'default'
}
