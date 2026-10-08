// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mkdirSync } from 'node:fs'
import path from 'node:path'

import e2eConfig from '../e2e-test/config'

import { ensureSimulatorBooted, terminateApp } from './support/simulator'
import { seedWebClip } from './support/webclip'

process.env.APPIUM_HOME ??= '.appium'

const resultsDir = 'ios-smoke-results'

const safeFileName = (value: string) =>
  value.replace(/[^a-zA-Z0-9-]+/g, '_').replace(/^_+|_+$/g, '')

async function saveFailureScreenshot(file: string | undefined, title: string) {
  const spec = path.basename(file ?? 'unknown', '.spec.ts')
  await browser.saveScreenshot(
    `${resultsDir}/${safeFileName(spec)}-${safeFileName(title)}.png`
  )
}

const webClipSpecs = './specs/webclip/**/*.spec.ts'

// WebDriverAgent screenshots are HEIC by default, which blurs the edges that
// the paint check in support/reachable.ts counts
const losslessScreenshots = { 'appium:settings[screenshotQuality]': 0 }

const safari: WebdriverIO.Capabilities = {
  'wdio:exclude': [webClipSpecs],
  platformName: 'iOS',
  'appium:automationName': 'XCUITest',
  browserName: 'Safari',
  'appium:safariInitialUrl': e2eConfig.enduserUrl,
  'appium:webviewConnectTimeout': 20000,
  'appium:newCommandTimeout': 120,
  'appium:wdaLaunchTimeout': 240000,
  'appium:fullContextList': true,
  'appium:additionalWebviewBundleIds': ['com.apple.webapp', '*'],
  'appium:simulatorStartupTimeout': 180000,
  ...losslessScreenshots
}

// No browserName or app: the session starts on the home screen and the spec
// opens the web clip by tapping its icon
const webClip: WebdriverIO.Capabilities = {
  'wdio:specs': [webClipSpecs],
  platformName: 'iOS',
  'appium:automationName': 'XCUITest',
  'appium:fullContextList': true,
  'appium:additionalWebviewBundleIds': ['com.apple.webapp', '*'],
  'appium:includeSafariInWebviews': true,
  'appium:webviewConnectTimeout': 20000,
  'appium:newCommandTimeout': 180,
  'appium:wdaLaunchTimeout': 240000,
  ...losslessScreenshots
}

const isWebClip = (caps: WebdriverIO.Capabilities) =>
  caps['wdio:specs']?.includes(webClipSpecs) === true

export const config: WebdriverIO.Config = {
  runner: 'local',
  specs: ['./specs/**/*.spec.ts'],
  maxInstances: 1,
  capabilities:
    process.env.IOS_SMOKE_WEBCLIP === '1' ? [safari, webClip] : [safari],
  baseUrl: e2eConfig.enduserUrl,
  logLevel: 'warn',
  waitforTimeout: 15000,
  connectionRetryTimeout: 300000,
  connectionRetryCount: 1,
  framework: 'mocha',
  mochaOpts: { ui: 'bdd', timeout: 120000 },
  reporters: ['spec'],
  services: [
    [
      'appium',
      {
        args: { logTimestamp: true, log: `${resultsDir}/appium.log` }
      }
    ]
  ],
  onPrepare(_config, caps) {
    mkdirSync(resultsDir, { recursive: true })
    const udid = ensureSimulatorBooted()
    for (const cap of caps as WebdriverIO.Capabilities[]) {
      cap['appium:udid'] = udid
    }
  },
  beforeSession(_config, capabilities) {
    const caps = capabilities as WebdriverIO.Capabilities
    const udid = caps['appium:udid']
    if (!udid) throw new Error('Simulator udid was not resolved in onPrepare')
    terminateApps(udid)
    if (isWebClip(caps)) seedWebClip(udid)
  },
  async before() {
    await prepareSession()
  },
  async afterHook(test, _context, { error }) {
    if (error) await recoverFromFailure(test.file, test.title)
  },
  async afterTest(test, _context, { passed }) {
    if (!passed) await recoverFromFailure(test.file, test.title)
  }
}

function terminateApps(udid: string) {
  terminateApp(udid, 'com.apple.mobilesafari')
  terminateApp(udid, 'com.apple.webapp')
}

// The script timeout is 0 by default, which fails every execute(async ...)
async function prepareSession() {
  await browser.setTimeout({ script: 30000 })
}

// A failed check leaves Safari in a state where the next navigation can hang
// for minutes, so Safari specs continue in a fresh session. The web clip
// session is launched once by its spec and is left alone.
async function recoverFromFailure(file: string | undefined, title: string) {
  await saveFailureScreenshot(file, title)
  const caps = browser.requestedCapabilities as WebdriverIO.Capabilities
  if (isWebClip(caps)) return
  const udid = caps['appium:udid']
  if (udid) terminateApps(udid)
  await browser.reloadSession()
  await prepareSession()
}
