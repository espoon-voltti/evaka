// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { execFileSync } from 'node:child_process'
import { randomUUID } from 'node:crypto'
import {
  copyFileSync,
  existsSync,
  mkdirSync,
  readdirSync,
  rmSync,
  writeFileSync
} from 'node:fs'
import { homedir } from 'node:os'
import path from 'node:path'
import { setTimeout as sleep } from 'node:timers/promises'
import { fileURLToPath } from 'node:url'

import config from '../../e2e-test/config'
import type { DevPerson } from '../../e2e-test/generated/api-types'

import { citizenLogin } from './login'
import { navigate } from './navigate'
import { storeTestConfig } from './test-config'

export const webClipTitle = 'eVaka'
const webClipPath = '/calendar'
const iconFile = fileURLToPath(
  new URL('../../../public/icons/evaka-180px.png', import.meta.url)
)
const springBoardRestartMs = 5000
const contextTimeoutMs = 30000
const maxHomeScreenPages = 3
const homeScreenAnimationMs = 1000

const simctlSpawn = (udid: string, ...args: string[]) =>
  execFileSync('xcrun', ['simctl', 'spawn', udid, ...args], {
    stdio: 'ignore'
  })

// Web Inspector settings that Safari's developer menu would turn on. They
// persist across simulator reboots.
const inspectorDefaults = [
  ['com.apple.WebInspector', 'EnableRemoteInspection', '-bool', 'YES'],
  ['com.apple.WebInspector', 'RemoteInspectorEnabled', '-bool', 'YES'],
  ['com.apple.webinspectord', 'RemoteInspectorEnabled', '-int', '1'],
  ['com.apple.webapp', 'WebKitDeveloperExtrasEnabled', '-bool', 'YES']
]

const webClipsDir = (udid: string) =>
  path.join(
    homedir(),
    'Library/Developer/CoreSimulator/Devices',
    udid,
    'data/Library/WebClips'
  )

const readTitle = (infoPlist: string) => {
  try {
    return execFileSync(
      'plutil',
      ['-extract', 'Title', 'raw', '-o', '-', infoPlist],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }
    ).trim()
  } catch {
    return undefined
  }
}

const escapeXml = (value: string) =>
  value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

// Keys copied from a clip that iOS 26.4 Safari created with "Add to Home Screen"
const infoPlist = (
  id: string,
  url: string
) => `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>ApplicationBundleVersion</key><integer>1</integer>
<key>ClassicMode</key><false/>
<key>ConfigurationIsManaged</key><false/>
<key>ContentMode</key><string>UIWebClipContentModeRecommended</string>
<key>Eligibility</key><integer>0</integer>
<key>FullScreen</key><true/>
<key>IconIsPrecomposed</key><false/>
<key>IconIsScreenShotBased</key><false/>
<key>IgnoreManifestScope</key><false/>
<key>IsAppClip</key><false/>
<key>Orientations</key><integer>0</integer>
<key>PlaceholderBundleIdentifier</key><string>com.apple.WebKit.PushBundle.${id}</string>
<key>ScenelessBackgroundLaunch</key><false/>
<key>Title</key><string>${webClipTitle}</string>
<key>TrustedClientBundleIdentifiers</key><array><string>com.apple.mobilesafari</string></array>
<key>URL</key><string>${escapeXml(url)}</string>
<key>WebClipStatusBarStyle</key><string>UIWebClipStatusBarStyleLegacyBlackTranslucent</string>
</dict></plist>
`

/**
 * Puts an eVaka icon on the simulator's home screen, as if the user had added
 * the app with Safari's "Add to Home Screen". Replaces earlier eVaka clips.
 */
export async function seedWebClip(udid: string) {
  const dir = webClipsDir(udid)
  mkdirSync(dir, { recursive: true })
  for (const entry of readdirSync(dir)) {
    const plist = path.join(dir, entry, 'Info.plist')
    if (
      entry.endsWith('.webclip') &&
      existsSync(plist) &&
      readTitle(plist) === webClipTitle
    ) {
      rmSync(path.join(dir, entry), { recursive: true, force: true })
    }
  }

  const id = randomUUID().replace(/-/g, '').toUpperCase()
  const clipDir = path.join(dir, `${id}.webclip`)
  mkdirSync(clipDir)
  writeFileSync(
    path.join(clipDir, 'Info.plist'),
    infoPlist(id, config.enduserUrl + webClipPath)
  )
  copyFileSync(iconFile, path.join(clipDir, 'icon.png'))

  for (const args of inspectorDefaults) {
    simctlSpawn(udid, 'defaults', 'write', ...args)
  }

  // SpringBoard reads the clips only on start; launchd restarts it
  simctlSpawn(udid, 'launchctl', 'stop', 'com.apple.SpringBoard')
  await sleep(springBoardRestartMs)
}

interface WebviewContext {
  id: string
  url?: string
}

const findAppContext = async () => {
  const contexts = (await browser.getContexts()) as (string | WebviewContext)[]
  return contexts.find(
    (context): context is WebviewContext =>
      typeof context === 'object' &&
      context.url?.startsWith(config.enduserUrl) === true
  )
}

const tapHomeScreenIcon = async () => {
  const icon = $(`~${webClipTitle}`)
  for (let page = 0; page < maxHomeScreenPages; page++) {
    // The icon exists in the accessibility tree also when it is on another
    // page, but it is not visible there
    await browser.pause(homeScreenAnimationMs)
    if (await icon.isDisplayed()) {
      await icon.click()
      return
    }
    await browser.execute('mobile: swipe', { direction: 'left' })
  }
  throw new Error(
    `The ${webClipTitle} icon is not visible on the first ${maxHomeScreenPages} home screen pages`
  )
}

/**
 * Taps the eVaka icon on the home screen and switches to the web clip's
 * webview. The context id changes on every launch and the webview belongs to
 * com.apple.SafariViewService, so the context is recognised by its URL.
 */
export async function launchWebClip() {
  await browser.switchContext('NATIVE_APP')
  await browser.execute('mobile: pressButton', { name: 'home' })
  await tapHomeScreenIcon()

  const context = await browser.waitUntil(findAppContext, {
    timeout: contextTimeoutMs,
    interval: 1500,
    timeoutMsg: `No webview with a URL under ${config.enduserUrl} appeared after tapping the icon`
  })
  await browser.switchContext(context.id)
}

/**
 * Logs in inside the web clip, which has its own cookies and storage separate
 * from Safari. Only `automatedTest` is stored: the installed app layout must
 * come from the real `navigator.standalone`.
 */
export async function openInWebClip(person: DevPerson, path: string) {
  await navigate(`${config.enduserUrl}/offline.html`)
  await storeTestConfig({ automatedTest: true })
  await citizenLogin(person, path)
}
