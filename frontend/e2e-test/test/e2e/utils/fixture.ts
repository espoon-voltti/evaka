// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import TestController from 'testcafe'

export async function logConsoleMessages(t: TestController): Promise<void> {
  try {
    const { warn, log, info, error } = await t.getBrowserConsoleMessages()
    if (warn.length || log.length || info.length || error.length) {
      console.info('Browser console messages:')
    }
    warn.forEach((msg) => console.warn(msg))
    info.forEach((msg) => console.info(msg))
    error.forEach((msg) => console.error(msg))
    log.forEach((msg) => console.log(msg))
  } catch (e) {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
    if (e.code === 'E54') {
      // TestCafe PageLoadError
      return
    }
    console.error(
      // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
      `Failed to log console messages: ${String(e.stack || e.message)}`
    )
  }
}
