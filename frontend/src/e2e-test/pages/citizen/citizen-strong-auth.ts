// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page } from '../../utils/page'

export class MockStrongAuthPage {
  constructor(private readonly page: Page) {}

  async login<T>(ssn: string, createPage: (page: Page) => T): Promise<T> {
    await this.page.find(`[id="${ssn}"]`).locator.check()
    await this.page.find('[type=submit]').findText('Kirjaudu').click()
    await this.page.find('[type=submit]').findText('Jatka').click()
    await this.page.findByDataQa('header-city-logo').waitUntilVisible()
    return createPage(this.page)
  }
}
