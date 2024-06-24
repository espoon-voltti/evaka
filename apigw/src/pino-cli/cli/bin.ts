// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import pump from 'pump'

import { parserStream, transportStream } from './index.js'

pump(process.stdin, parserStream, transportStream, (err) => {
  if (err) {
    // This outputs the error in non-json and improper format but it's assumed that parserStream and transporStream
    // output a more useful error log message in a proper format
    console.error('An unexpected error occurred:', err)
    process.exit(1)
  }
})

process.on('uncaughtException', (err) => {
  // This outputs the error in non-json and improper format but it's assumed that parserStream and transporStream
  // output a more useful error log message in a proper format
  console.error('An unexpected error occurred:', err)
  process.exit(1)
})
