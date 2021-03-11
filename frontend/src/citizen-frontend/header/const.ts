// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const getLoginUri = () =>
  `/api/application/auth/saml/login?RelayState=${encodeURIComponent(
    `${window.location.pathname}${window.location.search}${window.location.hash}`
  )}`

export const headerHeight = '80px'
