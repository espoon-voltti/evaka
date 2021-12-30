// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Env = 'staging' | 'prod' | 'dev'

export const env = (): Env | 'default' => {
  if (window.location.host === 'espoonvarhaiskasvatus.fi') {
    return 'prod'
  }

  if (window.location.host === 'staging.espoonvarhaiskasvatus.fi') {
    return 'staging'
  }

  if (window.location.host === 'evaka.dev.espoon-voltti.fi') {
    return 'dev'
  }

  return 'default'
}
