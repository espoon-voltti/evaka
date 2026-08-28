// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isIOS } from 'lib-common/utils/helpers'

import { useIsHandheld } from './handheld'
import { useInstallPrompt } from './installPrompt'
import { useIsRunningInstalled } from './installed'

export type InstallAvailability =
  | { kind: 'unavailable' }
  | { kind: 'prompt'; show: () => Promise<'accepted' | 'dismissed'> }
  | { kind: 'instructions' }

export function useInstallAvailability(): InstallAvailability {
  const handheld = useIsHandheld()
  const runningInstalled = useIsRunningInstalled()
  const prompt = useInstallPrompt()

  if (!handheld || runningInstalled) return { kind: 'unavailable' }
  if (prompt.available) return { kind: 'prompt', show: prompt.show }
  return isIOS() ? { kind: 'instructions' } : { kind: 'unavailable' }
}
