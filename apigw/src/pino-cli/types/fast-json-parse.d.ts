// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module 'fast-json-parse' {
  interface Result {
    err?: SyntaxError
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    value?: any
  }

  function Parse(data: string): Result
  namespace Parse {}
  export = Parse
}
