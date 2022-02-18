// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { User } from '../auth/state'

export const getWeakLoginUri = (path: string) =>
  `/api/application/auth/evaka-customer/login?RelayState=${encodeURIComponent(
    path
  )}`

export const getLoginUri = (path?: string) =>
  `/api/application/auth/saml/login?RelayState=${encodeURIComponent(
    `${path ? path : window.location.pathname}${window.location.search}${
      window.location.hash
    }`
  )}`

export const getLogoutUri = (user: User) =>
  `/api/application/auth/${
    user?.userType === 'CITIZEN_WEAK' ? 'evaka-customer' : 'saml'
  }/logout`

export const headerHeightDesktop = 80
export const headerHeightMobile = 60
