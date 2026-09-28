// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import type { CitizenPasskey } from 'lib-common/generated/api-types/user'

import type { User } from '../auth/state'
import type { InstallAvailability } from '../pwa/installAvailability'
import type { PushAvailability } from '../pwa/pushNotifications'

import { isEmailVerified } from './emailVerification'

export const personalDetailsTasks = [
  'ADD_EMAIL_AND_PHONE',
  'ADD_EMAIL',
  'ADD_PHONE',
  'VERIFY_EMAIL',
  'ADD_TO_HOME_SCREEN',
  'ENABLE_PUSH_NOTIFICATIONS',
  'ADD_WEAK_LOGIN'
] as const

export type PersonalDetailsTask = (typeof personalDetailsTasks)[number]

export interface PersonalDetailsTaskInput {
  mode: 'web' | 'pwa'
  email: 'missing' | 'unverified' | 'verified'
  hasPhone: boolean
  hasLoginMethod: boolean
  canInstall: boolean
  canSubscribeToPush: boolean
  homeScreenTaskHidesLoginTask: boolean
}

export interface PersonalDetailsTaskSources {
  user: Pick<User, 'phone' | 'weakLoginUsername'> | undefined
  emailVerification: EmailVerificationStatusResponse | null | undefined
  passkeys: CitizenPasskey[] | null | undefined
  runningInstalled: boolean
  install: InstallAvailability['kind']
  push: PushAvailability['kind']
}

export function resolvePersonalDetailsTasks(
  input: PersonalDetailsTaskInput
): PersonalDetailsTask[] {
  const hasEmail = input.email !== 'missing'
  const contactPending = !hasEmail || !input.hasPhone
  const verifyPending = input.email === 'unverified'
  const homeScreenPending = input.mode === 'web' && input.canInstall
  const pending: Record<PersonalDetailsTask, boolean> = {
    ADD_EMAIL_AND_PHONE: !hasEmail && !input.hasPhone,
    ADD_EMAIL: !hasEmail && input.hasPhone,
    ADD_PHONE: hasEmail && !input.hasPhone,
    VERIFY_EMAIL: verifyPending,
    ADD_TO_HOME_SCREEN: homeScreenPending,
    ENABLE_PUSH_NOTIFICATIONS: input.mode === 'pwa' && input.canSubscribeToPush,
    ADD_WEAK_LOGIN:
      !input.hasLoginMethod &&
      !contactPending &&
      !verifyPending &&
      !(homeScreenPending && input.homeScreenTaskHidesLoginTask)
  }
  return personalDetailsTasks.filter((task) => pending[task])
}

export function toPersonalDetailsTaskInput({
  user,
  emailVerification,
  passkeys,
  runningInstalled,
  install,
  push
}: PersonalDetailsTaskSources): PersonalDetailsTaskInput | undefined {
  if (!user || !emailVerification || !passkeys) return undefined
  const canInstall = install !== 'unavailable'
  return {
    mode: runningInstalled ? 'pwa' : 'web',
    email: !emailVerification.email
      ? 'missing'
      : isEmailVerified(emailVerification)
        ? 'verified'
        : 'unverified',
    hasPhone: !!user.phone,
    hasLoginMethod: passkeys.length > 0 || !!user.weakLoginUsername,
    canInstall,
    canSubscribeToPush: push === 'subscribable',
    homeScreenTaskHidesLoginTask: canInstall
  }
}
