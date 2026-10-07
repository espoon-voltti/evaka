// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

interface TestConfig {
  automatedTest: boolean
  forceStandalone?: boolean
}

const installedAppLayout: TestConfig = {
  automatedTest: true,
  forceStandalone: true
}

/** Takes effect on the next page load */
export async function storeTestConfig(testConfig = installedAppLayout) {
  await browser.execute((value: string) => {
    window.localStorage.setItem('evaka.testConfig', value)
  }, JSON.stringify(testConfig))
}
