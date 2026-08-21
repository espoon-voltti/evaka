// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import { useQuery } from 'lib-common/query'

import type { User } from '../auth/state'
import { useUser } from '../auth/state'

import { isEmailVerified } from './emailVerification'
import { emailVerificationStatusQuery } from './queries'

export const personalDetailsTasks = [
  'ADD_EMAIL',
  'VERIFY_EMAIL',
  'ADD_PHONE',
  'ADD_WEAK_LOGIN'
] as const

export type PersonalDetailsTask = (typeof personalDetailsTasks)[number]

export type PersonalDetailsTaskSection = 'contact' | 'login' | 'notifications'

interface PersonalDetailsTaskContext {
  user: User
  emailVerification: EmailVerificationStatusResponse
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
  ADD_WEAK_LOGIN: {
    dataQa: 'task-add-weak-login',
    section: 'login',
    isPending: ({ user, emailVerification }) =>
      isEmailVerified(emailVerification) && !user.weakLoginUsername
  }
}

const noTasks: PersonalDetailsTask[] = []

export function usePersonalDetailsTasks(): PersonalDetailsTask[] {
  const user = useUser()
  const { data: emailVerification } = useQuery(emailVerificationStatusQuery(), {
    enabled: user !== undefined
  })
  return useMemo(() => {
    if (!user || !emailVerification) return noTasks
    const ctx = { user, emailVerification }
    return personalDetailsTasks.filter((task) =>
      personalDetailsTaskConfig[task].isPending(ctx)
    )
  }, [user, emailVerification])
}
