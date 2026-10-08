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
import { fileURLToPath } from 'node:url'

import config from '../../e2e-test/config'
import type { DevPerson } from '../../e2e-test/generated/api-types'

import { rebootSimulator, terminateApp } from './simulator'
import type { Context, Session } from './webdriver'
import { sleep, waitUntil } from './webdriver'

const webClipTitle = 'eVaka'
const iconFile = fileURLToPath(
  new URL('../../../public/icons/evaka-180px.png', import.meta.url)
)
const contextTimeoutMs = 30000
const appRenderTimeoutMs = 30000
const maxHomeScreenPages = 3
const homeScreenAnimationMs = 1000

// A static page on the app's origin: the session cookie can be set there
// without the app running its own auth checks concurrently
const setupPageUrl = `${config.enduserUrl}/offline.html`

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

const readPlistKey = (infoPlist: string, key: string) => {
  try {
    return execFileSync(
      'plutil',
      ['-extract', key, 'raw', '-o', '-', infoPlist],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }
    ).trim()
  } catch {
    return undefined
  }
}

const escapeXml = (value: string) =>
  value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

// Keys copied from a clip that iOS 26.4 Safari created with "Add to Home
// Screen". With the legacy translucent status bar style the page is drawn
// from the top of the screen while innerHeight still leaves out the status
// bar, so the app ends 62 points above the screen bottom. iOS scopes a clip
// without a web app manifest to its start URL's path, so the URL is the root.
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
<key>WebClipStatusBarStyle</key><string>UIWebClipStatusBarStyleDefault</string>
</dict></plist>
`

/**
 * Puts an eVaka icon on the simulator's home screen, as if the user had added
 * the app with Safari's "Add to Home Screen". An existing clip with the same
 * URL is kept and only closed, so that the app starts in a fresh process.
 */
export function seedWebClip(udid: string) {
  const url = `${config.enduserUrl}/`
  const dir = webClipsDir(udid)
  mkdirSync(dir, { recursive: true })
  let found = false
  for (const entry of readdirSync(dir)) {
    const plist = path.join(dir, entry, 'Info.plist')
    if (!entry.endsWith('.webclip') || !existsSync(plist)) continue
    if (readPlistKey(plist, 'Title') !== webClipTitle) continue
    if (readPlistKey(plist, 'URL') === url && !found) {
      found = true
    } else {
      rmSync(path.join(dir, entry), { recursive: true, force: true })
    }
  }
  if (found) {
    terminateApp(udid, 'com.apple.webapp')
    terminateApp(udid, 'com.apple.SafariViewService')
    return
  }

  const id = randomUUID().replace(/-/g, '').toUpperCase()
  const clipDir = path.join(dir, `${id}.webclip`)
  mkdirSync(clipDir)
  writeFileSync(path.join(clipDir, 'Info.plist'), infoPlist(id, url))
  copyFileSync(iconFile, path.join(clipDir, 'icon.png'))

  for (const args of inspectorDefaults) {
    simctlSpawn(udid, 'defaults', 'write', ...args)
  }

  // SpringBoard reads the clips only on start. Restarting just SpringBoard
  // leaves the simulator unable to rotate ("Unable To Rotate Device"), so the
  // whole simulator is rebooted.
  rebootSimulator(udid)
}

async function tapHomeScreenIcon(session: Session) {
  for (let page = 0; page < maxHomeScreenPages; page++) {
    // The icon exists in the accessibility tree also when it is on another
    // page, but it is not visible there
    await sleep(homeScreenAnimationMs)
    const icon = await session.findElement(webClipTitle, 'accessibility id')
    if (icon && (await session.isDisplayed(icon))) {
      await session.clickElement(icon)
      return
    }
    await session.mobile('swipe', { direction: 'left' })
  }
  throw new Error(
    `The ${webClipTitle} icon is not visible on the first ${maxHomeScreenPages} home screen pages`
  )
}

/**
 * Taps the eVaka icon on the home screen, switches to the web clip's webview
 * and waits until the app has rendered. The context id changes on every
 * launch and the webview belongs to com.apple.SafariViewService, so the
 * context is recognised by its URL.
 */
export async function launchWebClip(session: Session) {
  await session.switchContext('NATIVE_APP')
  await session.mobile('pressButton', { name: 'home' })
  await tapHomeScreenIcon(session)
  const context = await waitUntil(
    async () =>
      (await session.contexts()).find(
        (context): context is Context =>
          typeof context === 'object' &&
          context.url?.startsWith(config.enduserUrl) === true
      ),
    {
      timeout: contextTimeoutMs,
      interval: 1500,
      message: `No webview with a URL under ${config.enduserUrl} appeared after tapping the icon`
    }
  )
  await session.switchContext(context.id)
  // Right after the tap the dev bundle is still loading and the screen can be
  // blank
  await waitUntil(
    () => session.execute(() => !!document.querySelector('#app [data-qa]')),
    {
      timeout: appRenderTimeoutMs,
      message: 'The app did not render in the web clip'
    }
  )
}

declare global {
  interface Window {
    iosSmokeStalePage?: boolean
  }
}

/**
 * WebKit may return from the navigation command before the previous page has
 * been replaced, so commands would run against the old document. The marker
 * set here disappears only when the new document has loaded.
 */
export async function navigate(session: Session, url: string) {
  await session.execute(() => {
    window.iosSmokeStalePage = true
  })
  await session.navigate(url)
  await waitUntil(
    () =>
      session.execute(
        (target: string) =>
          window.iosSmokeStalePage !== true &&
          document.readyState === 'complete' &&
          location.href.startsWith(target),
        url
      ),
    { message: `Page did not load: ${url}` }
  )
}

/** Logs in inside the web clip, which has its own cookies separate from Safari */
export async function openInWebClip(
  session: Session,
  person: DevPerson,
  path: string,
  { cookies = [] }: { cookies?: string[] } = {}
) {
  if (!person.ssn) throw new Error('Person does not have an SSN: cannot login')
  await navigate(session, setupPageUrl)
  for (const cookie of cookies) {
    await session.execute((value: string) => {
      document.cookie = `${value}; max-age=86400; path=/; SameSite=Strict`
    }, cookie)
  }
  const result = await session.executeAsync(
    async (url: string, ssn: string) => {
      const response = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ssn })
      })
      return { httpStatus: response.status }
    },
    `${config.devApiGwUrl}/auth/citizen-sfi-login`,
    person.ssn
  )
  if (!('httpStatus' in result) || result.httpStatus !== 200) {
    throw new Error(`Citizen login failed: ${JSON.stringify(result)}`)
  }
  await navigate(session, config.enduserUrl + path)
}
