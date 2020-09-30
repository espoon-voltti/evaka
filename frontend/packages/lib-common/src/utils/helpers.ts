// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const getEnvironment = (): string => {
  if (window.location.host.startsWith('localhost')) {
    return 'local'
  }

  if (window.location.host === 'espoonvarhaiskasvatus.fi') {
    return 'prod'
  }

  if (
    window.location.host.includes('espoonvarhaiskasvatus.fi') ||
    window.location.host.includes('espoon-voltti.fi')
  ) {
    const splitDomains = window.location.host.split('.')
    return splitDomains[splitDomains.length - 3]
  }

  return ''
}

export const isProduction = (): boolean => {
  return getEnvironment() === 'prod'
}
