// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Profile } from '@node-saml/node-saml'

import { SamlSession } from '../saml/index.js'

export type CitizenSessionUser =
  | {
      id: string
      authType: 'sfi'
      userType: 'CITIZEN_STRONG'
      samlSession: SamlSession
    }
  | { id: string; authType: 'citizen-weak'; userType: 'CITIZEN_WEAK' }
  | {
      id: string
      authType: 'keycloak-citizen'
      userType: 'CITIZEN_WEAK'
      samlSession: SamlSession
    }
  | { id: string; authType: 'dev'; userType: 'CITIZEN_STRONG' }

export type EmployeeSessionUser =
  | {
      id: string
      authType: 'ad' | 'keycloak-employee' | 'sfi'
      userType: 'EMPLOYEE'
      samlSession: SamlSession
      globalRoles: string[]
      allScopedRoles: string[]
    }
  | {
      id: string
      authType: 'dev'
      userType: 'EMPLOYEE'
      globalRoles: string[]
      allScopedRoles: string[]
    }

export interface EmployeeMobileSessionUser {
  id: string
  authType: 'employee-mobile'
  userType: 'MOBILE'
  mobileEmployeeId?: string | undefined
}

export type EvakaSessionUser =
  | CitizenSessionUser
  | EmployeeSessionUser
  | EmployeeMobileSessionUser

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
        case undefined:
          throw new Error('User type is undefined')
      }
    })()
  )
}

export const systemUserHeader = JSON.stringify({ type: 'system' })

export const integrationUserHeader = JSON.stringify({ type: 'integration' })

export function createLogoutToken(profile: Profile) {
  return `${profile.nameID}:::${profile.sessionIndex}`
}
