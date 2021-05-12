// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'tough-cookie'

declare module 'tough-cookie' {
  export namespace CookieJar {
    interface SetCookieOptions {
      http?: boolean
      secure?: boolean
      now?: Date
      ignoreError?: boolean
      // Missing from @types/tough-cookie@4.0.0
      // Could be defined as: "none" | "lax" | "strict" but
      // Cookie.sameSite is defined as: string
      sameSiteContext?: string
    }
  }
}
