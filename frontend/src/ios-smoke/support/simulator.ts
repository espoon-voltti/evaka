// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { execFileSync } from 'node:child_process'

export const simulatorName = 'evaka-smoke'
const deviceTypeId = 'com.apple.CoreSimulator.SimDeviceType.iPhone-17'

interface SimDevice {
  udid: string
  name: string
  state: string
  isAvailable: boolean
}

interface SimRuntime {
  identifier: string
  version: string
  platform?: string
  isAvailable: boolean
}

const simctl = (...args: string[]) =>
  execFileSync('xcrun', ['simctl', ...args], { encoding: 'utf8' })

const compareVersions = (a: string, b: string) => {
  const pa = a.split('.').map(Number)
  const pb = b.split('.').map(Number)
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const diff = (pa[i] ?? 0) - (pb[i] ?? 0)
    if (diff !== 0) return diff
  }
  return 0
}

const findDevice = (): SimDevice | undefined => {
  const { devices } = JSON.parse(simctl('list', 'devices', '--json')) as {
    devices: Record<string, SimDevice[]>
  }
  return Object.values(devices)
    .flat()
    .find((device) => device.name === simulatorName && device.isAvailable)
}

const newestIosRuntime = (): SimRuntime => {
  const { runtimes } = JSON.parse(simctl('list', 'runtimes', '--json')) as {
    runtimes: SimRuntime[]
  }
  const ios = runtimes
    .filter(
      (runtime) =>
        runtime.isAvailable &&
        (runtime.platform === 'iOS' ||
          runtime.identifier.includes('SimRuntime.iOS-'))
    )
    .sort((a, b) => compareVersions(b.version, a.version))
  if (ios.length === 0) {
    throw new Error(
      'No iOS simulator runtime installed: run `xcodebuild -downloadPlatform iOS`'
    )
  }
  return ios[0]
}

export function ensureSimulatorBooted(): string {
  const udid =
    findDevice()?.udid ??
    simctl(
      'create',
      simulatorName,
      deviceTypeId,
      newestIosRuntime().identifier
    ).trim()
  simctl('bootstatus', udid, '-b')
  return udid
}

export function rebootSimulator(udid: string) {
  simctl('shutdown', udid)
  simctl('bootstatus', udid, '-b')
}

export function terminateApp(udid: string, bundleId: string) {
  try {
    execFileSync('xcrun', ['simctl', 'terminate', udid, bundleId], {
      stdio: 'ignore'
    })
  } catch {
    // not running
  }
}
