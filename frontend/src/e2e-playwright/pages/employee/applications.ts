import { Page } from 'playwright'
import { RawElement } from 'e2e-playwright/utils/element'

export default class ApplicationsPage {
  constructor(private readonly page: Page) {}

  applicationStatusFilter(status: 'ALL') {
    return new RawElement(
      this.page,
      `[data-qa="application-status-filter-${status}"]`
    )
  }

  async toggleApplicationStatusFilter(status: 'ALL') {
    await this.applicationStatusFilter(status).click()
  }

  applicationRow(id: string) {
    const element = new RawElement(this.page, `[data-application-id="${id}"]`)
    return {
      status: element.find('[data-qa="application-status"]'),
      openApplication: async () => {
        const applicationDetails = new Promise<ApplicationDetailsPage>(
          (res) => {
            this.page.on('popup', (page) =>
              res(new ApplicationDetailsPage(page))
            )
          }
        )
        await element.click()
        return applicationDetails
      }
    }
  }

  readonly details = {
    applicantDeadIndicator: new RawElement(
      this.page,
      '[data-qa="applicant-dead"]'
    )
  }
}

class ApplicationDetailsPage {
  constructor(private readonly page: Page) {}

  applicantDeadIndicator = new RawElement(
    this.page,
    '[data-qa="applicant-dead"]'
  )
}
