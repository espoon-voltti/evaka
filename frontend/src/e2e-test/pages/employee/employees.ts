// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput } from '../../utils/page'

export class EmployeesPage {
  constructor(private readonly page: Page) {}

  readonly nameInput = new TextInput(
    this.page.find('[data-qa="employee-name-filter"]')
  )

  get visibleUsers(): Promise<string[]> {
    return this.page.findAll('[data-qa="employee-name"]').allInnerTexts()
  }
}
