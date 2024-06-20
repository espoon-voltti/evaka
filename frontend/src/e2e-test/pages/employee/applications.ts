// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Element, Page } from '../../utils/page'

import ApplicationReadView from './applications/application-read-view'

export default class ApplicationsPage {
  constructor(private readonly page: Page) {}

  applicationStatusFilter(status: 'ALL') {
    return this.page.findByDataQa(`application-status-filter-${status}`)
  }

  async toggleApplicationStatusFilter(status: 'ALL') {
    await this.applicationStatusFilter(status).click()
  }

  applicationRow(id: string) {
    const element = this.page.find(`[data-application-id="${id}"]`)
    return new ApplicationRow(this.page, element)
  }

  readonly details = {
    applicantDeadIndicator: this.page.findByDataQa('applicant-dead')
  }
}

export class ApplicationRow extends Element {
  constructor(
    private page: Page,
    root: Element
  ) {
    super(root)
  }

  status = this.find('[data-qa="application-status"]')

  async openApplication() {
    const applicationDetails = new Promise<ApplicationReadView>((res) => {
      this.page.onPopup((page) => res(new ApplicationReadView(page)))
    })
    await this.click()
    return applicationDetails
  }
}
