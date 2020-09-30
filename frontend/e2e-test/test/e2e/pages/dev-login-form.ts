// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ClientFunction, Selector, t } from 'testcafe'
import { UUID } from '../dev-api/types'

export interface DevLoginUser {
  aad: UUID
  roles: DevLoginRole[]
}

export type DevLoginRole = typeof devLoginRoles[number]
const devLoginRoles = [
  'SERVICE_WORKER',
  'FINANCE_ADMIN',
  'DIRECTOR',
  'ADMIN'
] as const

export default class DevLoginForm {
  private readonly aadInput = Selector('#aad-input')
  private readonly submit = Selector('form button')
  private readonly roleSelectors: Record<DevLoginRole, Selector> = {
    SERVICE_WORKER: Selector('#evaka-espoo-officeholder'),
    FINANCE_ADMIN: Selector('#evaka-espoo-financeadmin'),
    DIRECTOR: Selector('#evaka-espoo-director'),
    ADMIN: Selector('#evaka-espoo-admin')
  }
  async getRoleCheckboxStates(): Promise<Record<DevLoginRole, boolean>> {
    const roleSelectors = this.roleSelectors
    return ClientFunction(
      () => {
        const isChecked = (role: DevLoginRole) => {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const el = (roleSelectors[role]() as any) as HTMLInputElement | null
          if (!el) throw new Error(`No checkbox found for ${role}`)
          return el.checked
        }
        return {
          SERVICE_WORKER: isChecked('SERVICE_WORKER'),
          FINANCE_ADMIN: isChecked('FINANCE_ADMIN'),
          DIRECTOR: isChecked('DIRECTOR'),
          ADMIN: isChecked('ADMIN')
        }
      },
      { dependencies: { roleSelectors } }
    )()
  }

  async login({ aad, roles }: DevLoginUser) {
    await t.typeText(this.aadInput, aad, {
      paste: true,
      replace: true
    })
    const beforeStates = await this.getRoleCheckboxStates()
    const wantedStates: Record<DevLoginRole, boolean> = {
      SERVICE_WORKER: false,
      FINANCE_ADMIN: false,
      DIRECTOR: false,
      ADMIN: false
    }
    for (const role of devLoginRoles) {
      wantedStates[role] = roles.includes(role)
      if (wantedStates[role] !== beforeStates[role]) {
        await t.click(this.roleSelectors[role])
      }
    }
    const afterStates = await this.getRoleCheckboxStates()
    await t.expect(afterStates).eql(wantedStates)
    await t.click(this.submit)
  }
}
