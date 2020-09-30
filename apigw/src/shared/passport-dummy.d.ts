// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module 'passport-dummy' {
  import passport from 'passport'

  export interface Options {
    allow?: boolean
  }

  export type VerifyFunction = (done: VerifiedCallback) => void
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export type VerifiedCallback = (err: any, user?: object) => void

  export class Strategy extends passport.Strategy {
    constructor(options: Options, verify: VerifyFunction)
    constructor(verify: VerifyFunction)
  }

  export const version: string
}
