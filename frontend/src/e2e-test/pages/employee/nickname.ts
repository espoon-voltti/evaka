// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'assert'

import { waitUntilEqual } from '../../utils'
import { AsyncButton, Page, Select } from '../../utils/page'

export class EmployeeNickname {
  constructor(private readonly page: Page) {}

  readonly nicknameSelect = new Select(
    this.page.findByDataQa('select-nickname')
  )

  readonly confirmButton = new AsyncButton(
    this.page.findByDataQa('confirm-button')
  )

  async assertSelectedNickname(expectedNickname: string) {
    await waitUntilEqual(
      () => this.nicknameSelect.selectedOption,
      expectedNickname
    )
  }

  async assertAvailableNicknames(expectedAvailableNicknames: string[]) {
    const availableNicknames = await this.nicknameSelect.allOptions
    expectedAvailableNicknames.forEach((expected) =>
      assert(availableNicknames.includes(expected))
    )
  }

  async selectNickname(nickname: string) {
    await this.nicknameSelect.selectOption(nickname)
  }

  async confirm() {
    await this.confirmButton.click()
    await this.confirmButton.waitUntilIdle()
  }
}
