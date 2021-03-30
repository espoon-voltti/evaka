// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  RawElement,
  RawRadio,
  RawTextInput
} from 'e2e-playwright/utils/element'
import { UUID } from 'lib-common/types'
import { Page } from 'playwright'

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
  constructor(private readonly page: Page) {}

  readonly #aadInput = new RawTextInput(this.page, '#aad-input')
  readonly #submit = new RawElement(this.page, 'form button')

  private readonly roleSelectors: Record<DevLoginRole, string> = {
    SERVICE_WORKER: '#evaka-espoo-officeholder',
    FINANCE_ADMIN: '#evaka-espoo-financeadmin',
    DIRECTOR: '#evaka-espoo-director',
    ADMIN: '#evaka-espoo-admin'
  }
  async getRoleCheckboxStates(): Promise<Record<DevLoginRole, boolean>> {
    return this.page.evaluate((roleSelectors) => {
      const isChecked = (role: DevLoginRole) => {
        const el = document.querySelector(roleSelectors[role])
        if (!el || !(el instanceof HTMLInputElement))
          throw new Error(`No checkbox found for ${role}`)
        return el.checked
      }
      return {
        SERVICE_WORKER: isChecked('SERVICE_WORKER'),
        FINANCE_ADMIN: isChecked('FINANCE_ADMIN'),
        DIRECTOR: isChecked('DIRECTOR'),
        ADMIN: isChecked('ADMIN')
      }
    }, this.roleSelectors)
  }

  async login({ aad, roles }: DevLoginUser) {
    await this.#aadInput.clear()
    await this.#aadInput.type(aad)
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
        await new RawRadio(this.page, this.roleSelectors[role]).click()
      }
    }
    const afterStates = await this.getRoleCheckboxStates()
    expect(afterStates).toEqual(wantedStates)
    await this.#submit.click()
  }
}
