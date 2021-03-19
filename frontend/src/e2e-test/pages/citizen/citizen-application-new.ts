// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { ApplicationType } from 'lib-common/api-types/application/enums'

export default class CitizenNewApplicationPage {
  readonly title = Selector('h1')
  readonly submit = Selector('[data-qa="submit"]')
  readonly transferApplicationNotification = Selector(
    '[data-qa="transfer-application-notification"]'
  )
  readonly duplicateApplicationNotification = Selector(
    '[data-qa="duplicate-application-notification"]'
  )

  async selectType(type: ApplicationType) {
    await t.click(Selector(`[data-qa="type-radio-${type}"]`))
  }

  async createApplication(type: ApplicationType) {
    await this.selectType(type)
    await t.click(this.submit)
  }
}
