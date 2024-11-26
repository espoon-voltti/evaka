// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { User } from '../auth/state'

export const getWeakLoginUri = (
  url = `${window.location.pathname}${window.location.search}${window.location.hash}`
) =>
  `/api/application/auth/evaka-customer/login?RelayState=${encodeURIComponent(url)}`

export const getStrongLoginUri = (
  url = `${window.location.pathname}${window.location.search}${window.location.hash}`
) => `/api/application/auth/saml/login?RelayState=${encodeURIComponent(url)}`

export const getLogoutUri = (user: User) =>
  `/api/application/auth/${
    user?.authLevel === 'WEAK' ? 'evaka-customer' : 'saml'
  }/logout`

export const headerHeightDesktop = 80
export const headerHeightMobile = 60
export const mobileBottomNavHeight = 60
