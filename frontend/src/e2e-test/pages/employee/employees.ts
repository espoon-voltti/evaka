// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput } from '../../utils/page'

export class EmployeesPage {
  constructor(private readonly page: Page) {}

  readonly nameInput = new TextInput(
    this.page.findByDataQa('employee-name-filter')
  )

  get visibleUsers(): Promise<string[]> {
    return this.page.findAllByDataQa('employee-name').allTexts()
  }
}
