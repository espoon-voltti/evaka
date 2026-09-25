// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import { constantQuery, useQuery } from 'lib-common/query'

import { passkeysSupported } from '../auth/passkeys'
import { useUser } from '../auth/state'
import { useInstallAvailability } from '../pwa/installAvailability'
import { useIsRunningInstalled } from '../pwa/installed'
import { usePushAvailability } from '../pwa/pushNotifications'

import { emailVerificationStatusQuery, passkeysQuery } from './queries'
import type { PersonalDetailsTask } from './taskRules'
import {
  resolvePersonalDetailsTasks,
  toPersonalDetailsTaskInput
} from './taskRules'

export type { PersonalDetailsTask } from './taskRules'

export type PersonalDetailsTaskSection =
  | 'contact'
  | 'login'
  | 'passkeys'
  | 'homeScreen'
  | 'push'
  | 'notifications'

export const personalDetailsTaskConfig: Record<
  PersonalDetailsTask,
  { dataQa: string; section: PersonalDetailsTaskSection }
> = {
  ADD_EMAIL_AND_PHONE: {
    dataQa: 'task-add-email-and-phone',
    section: 'contact'
  },
  ADD_EMAIL: { dataQa: 'task-add-email', section: 'contact' },
  ADD_PHONE: { dataQa: 'task-add-phone', section: 'contact' },
  VERIFY_EMAIL: { dataQa: 'task-verify-email', section: 'contact' },
  ADD_TO_HOME_SCREEN: {
    dataQa: 'task-add-to-home-screen',
    section: 'homeScreen'
  },
  ENABLE_PUSH_NOTIFICATIONS: {
    dataQa: 'task-enable-push-notifications',
    section: 'push'
  },
  ADD_WEAK_LOGIN: {
    dataQa: 'task-add-weak-login',
    section: passkeysSupported() ? 'passkeys' : 'login'
  }
}

const noTasks: PersonalDetailsTask[] = []

export function usePersonalDetailsTasks(): PersonalDetailsTask[] {
  const user = useUser()
  const { data: emailVerification } = useQuery(
    user !== undefined ? emailVerificationStatusQuery() : constantQuery(null)
  )
  const { data: passkeys } = useQuery(
    user !== undefined ? passkeysQuery() : constantQuery(null)
  )
  const runningInstalled = useIsRunningInstalled()
  const install = useInstallAvailability().kind
  const push = usePushAvailability().kind
  return useMemo(() => {
    const input = toPersonalDetailsTaskInput({
      user,
      emailVerification,
      passkeys,
      runningInstalled,
      install,
      push
    })
    return input ? resolvePersonalDetailsTasks(input) : noTasks
  }, [user, emailVerification, passkeys, runningInstalled, install, push])
}
