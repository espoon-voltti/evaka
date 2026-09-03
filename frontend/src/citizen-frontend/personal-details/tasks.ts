// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import type { CitizenPasskey } from 'lib-common/generated/api-types/user'
import { constantQuery, useQuery } from 'lib-common/query'

import type { User } from '../auth/state'
import { useUser } from '../auth/state'
import { useInstallAvailability } from '../pwa/installAvailability'

import { isEmailVerified } from './emailVerification'
import { emailVerificationStatusQuery, passkeysQuery } from './queries'

export const personalDetailsTasks = [
  'ADD_EMAIL',
  'VERIFY_EMAIL',
  'ADD_PHONE',
  'ADD_TO_HOME_SCREEN',
  'ADD_WEAK_LOGIN'
] as const

export type PersonalDetailsTask = (typeof personalDetailsTasks)[number]

export type PersonalDetailsTaskSection =
  | 'contact'
  | 'login'
  | 'passkeys'
  | 'homeScreen'
  | 'notifications'

interface PersonalDetailsTaskContext {
  user: User
  emailVerification: EmailVerificationStatusResponse
  passkeys: CitizenPasskey[]
  canInstall: boolean
}

export const personalDetailsTaskConfig: Record<
  PersonalDetailsTask,
  {
    dataQa: string
    section: PersonalDetailsTaskSection
    isPending: (ctx: PersonalDetailsTaskContext) => boolean
  }
> = {
  ADD_EMAIL: {
    dataQa: 'task-add-email',
    section: 'contact',
    isPending: ({ emailVerification }) => !emailVerification.email
  },
  VERIFY_EMAIL: {
    dataQa: 'task-verify-email',
    section: 'contact',
    isPending: ({ emailVerification }) =>
      !!emailVerification.email && !isEmailVerified(emailVerification)
  },
  ADD_PHONE: {
    dataQa: 'task-add-phone',
    section: 'contact',
    isPending: ({ user }) => !user.phone
  },
  ADD_TO_HOME_SCREEN: {
    dataQa: 'task-add-to-home-screen',
    section: 'homeScreen',
    isPending: ({ canInstall }) => canInstall
  },
  ADD_WEAK_LOGIN: {
    dataQa: 'task-add-weak-login',
    section: 'passkeys',
    isPending: ({ user, passkeys }) =>
      passkeys.length === 0 && !user.weakLoginUsername
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
  const canInstall = useInstallAvailability().kind !== 'unavailable'
  return useMemo(() => {
    if (!user || !emailVerification || !passkeys) return noTasks
    const ctx = { user, emailVerification, passkeys, canInstall }
    return personalDetailsTasks.filter((task) =>
      personalDetailsTaskConfig[task].isPending(ctx)
    )
  }, [user, emailVerification, passkeys, canInstall])
}
