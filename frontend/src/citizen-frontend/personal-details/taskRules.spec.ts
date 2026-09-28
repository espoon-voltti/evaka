// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import type { CitizenPasskey } from 'lib-common/generated/api-types/user'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'

import type {
  PersonalDetailsTask,
  PersonalDetailsTaskInput,
  PersonalDetailsTaskSources
} from './taskRules'
import {
  resolvePersonalDetailsTasks,
  toPersonalDetailsTaskInput
} from './taskRules'

const C: PersonalDetailsTask = 'ADD_EMAIL_AND_PHONE'
const E: PersonalDetailsTask = 'ADD_EMAIL'
const P: PersonalDetailsTask = 'ADD_PHONE'
const V: PersonalDetailsTask = 'VERIFY_EMAIL'
const H: PersonalDetailsTask = 'ADD_TO_HOME_SCREEN'
const Pu: PersonalDetailsTask = 'ENABLE_PUSH_NOTIFICATIONS'
const L: PersonalDetailsTask = 'ADD_WEAK_LOGIN'

const everythingDone = {
  email: 'verified',
  hasPhone: true,
  hasLoginMethod: true,
  canInstall: false,
  canSubscribeToPush: false,
  homeScreenTaskHidesLoginTask: true
} as const

const web = (
  overrides: Partial<PersonalDetailsTaskInput>
): PersonalDetailsTaskInput => ({
  ...everythingDone,
  mode: 'web',
  ...overrides
})

const pwa = (
  overrides: Partial<PersonalDetailsTaskInput>
): PersonalDetailsTaskInput => ({
  ...everythingDone,
  mode: 'pwa',
  ...overrides
})

describe('resolvePersonalDetailsTasks', () => {
  describe('designed task scenarios', () => {
    it.each<{
      scenario: string
      input: PersonalDetailsTaskInput
      expected: PersonalDetailsTask[]
    }>([
      {
        scenario: 'web 1',
        input: web({ hasLoginMethod: false, canInstall: true }),
        expected: [H]
      },
      {
        scenario: 'web 2',
        input: web({ hasLoginMethod: false }),
        expected: [L]
      },
      { scenario: 'web 3', input: web({}), expected: [] },
      {
        scenario: 'web 4',
        input: web({
          email: 'missing',
          hasPhone: false,
          hasLoginMethod: false,
          canInstall: true
        }),
        expected: [C, H]
      },
      {
        scenario: 'web 5',
        input: web({
          email: 'missing',
          hasPhone: false,
          hasLoginMethod: false
        }),
        expected: [C]
      },
      {
        scenario: 'web 6',
        input: web({
          email: 'missing',
          hasLoginMethod: false,
          canInstall: true
        }),
        expected: [E, H]
      },
      {
        scenario: 'web 7',
        input: web({ email: 'missing', hasLoginMethod: false }),
        expected: [E]
      },
      {
        scenario: 'web 8',
        input: web({
          email: 'unverified',
          hasPhone: false,
          hasLoginMethod: false,
          canInstall: true
        }),
        expected: [P, V, H]
      },
      {
        scenario: 'web 9',
        input: web({
          email: 'unverified',
          hasPhone: false,
          hasLoginMethod: false
        }),
        expected: [P, V]
      },
      {
        scenario: 'web 10',
        input: web({
          email: 'unverified',
          hasLoginMethod: false,
          canInstall: true
        }),
        expected: [V, H]
      },
      {
        scenario: 'web 11',
        input: web({ email: 'unverified', hasLoginMethod: false }),
        expected: [V]
      },
      {
        scenario: 'web 12',
        input: web({
          hasPhone: false,
          hasLoginMethod: false,
          canInstall: true
        }),
        expected: [P, H]
      },
      { scenario: 'web 13', input: web({ hasPhone: false }), expected: [P] },
      {
        scenario: 'installed app 1',
        input: pwa({ hasLoginMethod: false, canSubscribeToPush: true }),
        expected: [Pu, L]
      },
      {
        scenario: 'installed app 2',
        input: pwa({ canSubscribeToPush: true }),
        expected: [Pu]
      },
      {
        scenario: 'installed app 3',
        input: pwa({ hasLoginMethod: false }),
        expected: [L]
      },
      { scenario: 'installed app 4', input: pwa({}), expected: [] },
      {
        scenario: 'installed app 5',
        input: pwa({
          email: 'missing',
          hasPhone: false,
          hasLoginMethod: false,
          canSubscribeToPush: true
        }),
        expected: [C, Pu]
      },
      {
        scenario: 'installed app 6',
        input: pwa({
          email: 'missing',
          hasPhone: false,
          hasLoginMethod: false
        }),
        expected: [C]
      },
      {
        scenario: 'installed app 7',
        input: pwa({
          email: 'missing',
          hasLoginMethod: false,
          canSubscribeToPush: true
        }),
        expected: [E, Pu]
      },
      {
        scenario: 'installed app 8',
        input: pwa({ email: 'missing', hasLoginMethod: false }),
        expected: [E]
      },
      {
        scenario: 'installed app 9',
        input: pwa({
          email: 'unverified',
          hasPhone: false,
          hasLoginMethod: false,
          canSubscribeToPush: true
        }),
        expected: [P, V, Pu]
      },
      {
        scenario: 'installed app 10',
        input: pwa({
          email: 'unverified',
          hasPhone: false,
          hasLoginMethod: false
        }),
        expected: [P, V]
      },
      {
        scenario: 'installed app 11',
        input: pwa({
          email: 'unverified',
          hasLoginMethod: false,
          canSubscribeToPush: true
        }),
        expected: [V, Pu]
      },
      {
        scenario: 'installed app 12',
        input: pwa({ email: 'unverified', hasLoginMethod: false }),
        expected: [V]
      },
      {
        scenario: 'installed app 13',
        input: pwa({ hasPhone: false, canSubscribeToPush: true }),
        expected: [P, Pu]
      },
      {
        scenario: 'installed app 14',
        input: pwa({ hasPhone: false }),
        expected: [P]
      }
    ])('$scenario', ({ input, expected }) => {
      expect(resolvePersonalDetailsTasks(input)).toEqual(expected)
    })
  })

  describe('every input combination', () => {
    const bools = [false, true] as const
    const axes = {
      mode: ['web', 'pwa'],
      email: ['missing', 'unverified', 'verified'],
      hasPhone: bools,
      hasLoginMethod: bools,
      canInstall: bools,
      canSubscribeToPush: bools,
      homeScreenTaskHidesLoginTask: bools
    } as const satisfies {
      [
        K in keyof PersonalDetailsTaskInput
      ]: readonly PersonalDetailsTaskInput[K][]
    }
    const allInputs = Object.entries(axes).reduce<Record<string, unknown>[]>(
      (partials, [key, values]) =>
        partials.flatMap((partial) =>
          values.map((value) => ({ ...partial, [key]: value }))
        ),
      [{}]
    ) as unknown as PersonalDetailsTaskInput[]

    const forAll = (
      check: (
        input: PersonalDetailsTaskInput,
        tasks: PersonalDetailsTask[],
        label: string
      ) => void
    ) => {
      expect(allInputs).toHaveLength(192)
      for (const input of allInputs) {
        check(input, resolvePersonalDetailsTasks(input), JSON.stringify(input))
      }
    }

    const groupRank: Record<PersonalDetailsTask, number> = {
      ADD_EMAIL_AND_PHONE: 0,
      ADD_EMAIL: 0,
      ADD_PHONE: 0,
      VERIFY_EMAIL: 1,
      ADD_TO_HOME_SCREEN: 2,
      ENABLE_PUSH_NOTIFICATIONS: 2,
      ADD_WEAK_LOGIN: 3
    }

    it('lists contact tasks, then verification, then device, then login, without duplicates', () => {
      forAll((_, tasks, label) => {
        expect(new Set(tasks).size, label).toBe(tasks.length)
        const ranks = tasks.map((task) => groupRank[task])
        expect(ranks, label).toEqual([...ranks].sort((a, b) => a - b))
      })
    })

    it('shows one combined task when both email and phone are missing, otherwise the missing one', () => {
      forAll((input, tasks, label) => {
        const noEmail = input.email === 'missing'
        expect(tasks.includes(C), label).toBe(noEmail && !input.hasPhone)
        expect(tasks.includes(E), label).toBe(noEmail && input.hasPhone)
        expect(tasks.includes(P), label).toBe(!noEmail && !input.hasPhone)
      })
    })

    it('asks for verification only for an added, unverified email', () => {
      forAll((input, tasks, label) => {
        expect(tasks.includes(V), label).toBe(input.email === 'unverified')
      })
    })

    it('hides the login task while any contact or verification task is open', () => {
      forAll((_, tasks, label) => {
        if ([C, E, P, V].some((task) => tasks.includes(task))) {
          expect(tasks, label).not.toContain(L)
        }
      })
    })

    it('hides the login task on web while the login-hiding home screen task is open', () => {
      forAll((input, tasks, label) => {
        if (tasks.includes(H) && input.homeScreenTaskHidesLoginTask) {
          expect(tasks, label).not.toContain(L)
        }
      })
    })

    it('hides the login task once the citizen has a login method', () => {
      forAll((input, tasks, label) => {
        if (input.hasLoginMethod) expect(tasks, label).not.toContain(L)
      })
    })

    it('shows the login task whenever nothing hides it', () => {
      forAll((input, tasks, label) => {
        const hidden =
          input.hasLoginMethod ||
          [C, E, P, V].some((task) => tasks.includes(task)) ||
          (tasks.includes(H) && input.homeScreenTaskHidesLoginTask)
        if (!hidden) expect(tasks, label).toContain(L)
      })
    })

    it('offers the home screen only on web and push only in the installed app', () => {
      forAll((input, tasks, label) => {
        expect(tasks.includes(H), label).toBe(
          input.mode === 'web' && input.canInstall
        )
        expect(tasks.includes(Pu), label).toBe(
          input.mode === 'pwa' && input.canSubscribeToPush
        )
      })
    })

    it('has no tasks exactly when everything relevant is done', () => {
      forAll((input, tasks, label) => {
        const everythingRelevantDone =
          input.email === 'verified' &&
          input.hasPhone &&
          input.hasLoginMethod &&
          !(input.mode === 'web' && input.canInstall) &&
          !(input.mode === 'pwa' && input.canSubscribeToPush)
        expect(tasks.length === 0, label).toBe(everythingRelevantDone)
      })
    })
  })
})

describe('toPersonalDetailsTaskInput', () => {
  const passkey: CitizenPasskey = {
    id: fromUuid('0b0c8a52-6f7e-4d1c-9a55-3f0f4f5b4a01'),
    name: 'Puhelin',
    agentName: 'Chrome',
    operatingSystemName: 'Android',
    deviceClass: 'PHONE',
    createdAt: HelsinkiDateTime.of(2026, 9, 1, 12, 0),
    lastUsedAt: null
  }
  const verifiedEmail: EmailVerificationStatusResponse = {
    email: 'citizen@example.com',
    verifiedEmail: 'citizen@example.com',
    latestVerification: null
  }
  const sources: PersonalDetailsTaskSources = {
    user: { phone: '0401234567', weakLoginUsername: null },
    emailVerification: verifiedEmail,
    passkeys: [passkey],
    runningInstalled: false,
    install: 'unavailable',
    push: 'unavailable'
  }

  it('maps fully loaded sources', () => {
    expect(toPersonalDetailsTaskInput(sources)).toEqual({
      mode: 'web',
      email: 'verified',
      hasPhone: true,
      hasLoginMethod: true,
      canInstall: false,
      canSubscribeToPush: false,
      homeScreenTaskHidesLoginTask: false
    })
  })

  it.each<[string, Partial<PersonalDetailsTaskSources>]>([
    ['user', { user: undefined }],
    ['email verification', { emailVerification: undefined }],
    ['email verification (skipped query)', { emailVerification: null }],
    ['passkeys', { passkeys: undefined }],
    ['passkeys (skipped query)', { passkeys: null }]
  ])('gives no input while %s is not loaded', (_, missing) => {
    expect(toPersonalDetailsTaskInput({ ...sources, ...missing })).toBe(
      undefined
    )
  })

  it.each<[EmailVerificationStatusResponse, PersonalDetailsTaskInput['email']]>(
    [
      [
        { email: null, verifiedEmail: null, latestVerification: null },
        'missing'
      ],
      [
        {
          email: null,
          verifiedEmail: 'old@example.com',
          latestVerification: null
        },
        'missing'
      ],
      [
        {
          email: 'new@example.com',
          verifiedEmail: null,
          latestVerification: null
        },
        'unverified'
      ],
      [
        {
          email: 'new@example.com',
          verifiedEmail: 'old@example.com',
          latestVerification: null
        },
        'unverified'
      ],
      [verifiedEmail, 'verified']
    ]
  )('maps email status %o to %s', (emailVerification, expected) => {
    expect(
      toPersonalDetailsTaskInput({ ...sources, emailVerification })?.email
    ).toBe(expected)
  })

  it.each<[string, boolean]>([
    ['0401234567', true],
    ['', false]
  ])('maps phone %j to hasPhone %s', (phone, expected) => {
    expect(
      toPersonalDetailsTaskInput({
        ...sources,
        user: { phone, weakLoginUsername: null }
      })?.hasPhone
    ).toBe(expected)
  })

  it.each<[string, CitizenPasskey[], string | null, boolean]>([
    ['nothing', [], null, false],
    ['a passkey', [passkey], null, true],
    ['a weak login', [], 'citizen@example.com', true],
    ['both', [passkey], 'citizen@example.com', true]
  ])(
    'maps a login method from %s',
    (_, passkeys, weakLoginUsername, expected) => {
      expect(
        toPersonalDetailsTaskInput({
          ...sources,
          passkeys,
          user: { phone: '0401234567', weakLoginUsername }
        })?.hasLoginMethod
      ).toBe(expected)
    }
  )

  it('treats the installed app as the pwa mode', () => {
    expect(
      toPersonalDetailsTaskInput({ ...sources, runningInstalled: true })?.mode
    ).toBe('pwa')
  })

  it.each<
    [
      PersonalDetailsTaskSources['install'],
      Pick<
        PersonalDetailsTaskInput,
        'canInstall' | 'homeScreenTaskHidesLoginTask'
      >
    ]
  >([
    ['unavailable', { canInstall: false, homeScreenTaskHidesLoginTask: false }],
    ['prompt', { canInstall: true, homeScreenTaskHidesLoginTask: true }],
    // iOS Safari cannot see an installed app, so this keeps hiding the login task there
    ['instructions', { canInstall: true, homeScreenTaskHidesLoginTask: true }]
  ])('maps install availability %s', (install, expected) => {
    expect(toPersonalDetailsTaskInput({ ...sources, install })).toMatchObject(
      expected
    )
  })

  it.each<[PersonalDetailsTaskSources['push'], boolean]>([
    ['unavailable', false],
    ['blocked', false],
    ['subscribed', false],
    ['subscribable', true]
  ])('maps push availability %s to canSubscribeToPush %s', (push, expected) => {
    expect(
      toPersonalDetailsTaskInput({ ...sources, push })?.canSubscribeToPush
    ).toBe(expected)
  })
})
