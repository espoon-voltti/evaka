// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Profile } from '@node-saml/node-saml'

import { UserType } from '../service-client.js'

export interface EvakaSessionUser {
  // eVaka id
  id?: string | undefined
  userType?: UserType | undefined
  // all are optional because of legacy sessions
  roles?: string[] | undefined
  globalRoles?: string[] | undefined
  allScopedRoles?: string[] | undefined
  mobileEmployeeId?: string | undefined
}

export function createUserHeader(user: EvakaSessionUser): string {
  return JSON.stringify(
    ((): object => {
      switch (user.userType) {
        case 'CITIZEN_WEAK':
          return { type: 'citizen_weak', id: user.id }
        case 'CITIZEN_STRONG':
          return { type: 'citizen', id: user.id }
        case 'EMPLOYEE':
          return {
            type: 'employee',
            id: user.id,
            globalRoles: user.globalRoles,
            allScopedRoles: user.allScopedRoles
          }
        case 'MOBILE':
          return {
            type: 'mobile',
            id: user.id,
            employeeId: user.mobileEmployeeId
          }
        case 'SYSTEM':
          return { type: 'system' }
        case undefined:
          throw new Error('User type is undefined')
      }
    })()
  )
}

export const integrationUserHeader = JSON.stringify({ type: 'integration' })

export function createLogoutToken(profile: Profile) {
  return `${profile.nameID}:::${profile.sessionIndex}`
}
