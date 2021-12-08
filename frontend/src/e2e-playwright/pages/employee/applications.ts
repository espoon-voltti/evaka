// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Checkbox, FileInput } from 'e2e-playwright/utils/page'
import { waitUntilEqual, waitUntilFalse } from 'e2e-playwright/utils'

export default class ApplicationsPage {
  constructor(private readonly page: Page) {}

  applicationStatusFilter(status: 'ALL') {
    return this.page.find(`[data-qa="application-status-filter-${status}"]`)
  }

  async toggleApplicationStatusFilter(status: 'ALL') {
    await this.applicationStatusFilter(status).click()
  }

  applicationRow(id: string) {
    const element = this.page.find(`[data-application-id="${id}"]`)
    return {
      status: element.find('[data-qa="application-status"]'),
      openApplication: async () => {
        const applicationDetails = new Promise<ApplicationDetailsPage>(
          (res) => {
            this.page.onPopup((page) => res(new ApplicationDetailsPage(page)))
          }
        )
        await element.click()
        return applicationDetails
      }
    }
  }

  readonly details = {
    applicantDeadIndicator: this.page.find('[data-qa="applicant-dead"]')
  }
}

class ApplicationDetailsPage {
  constructor(private readonly page: Page) {}

  #editButton = this.page.find('[data-qa="edit-application"]')
  #saveButton = this.page.find('[data-qa="save-application"]')
  #urgentCheckbox = new Checkbox(this.page.find('[data-qa="checkbox-urgent"]'))
  #urgentAttachmentFileUpload = this.page.find('[data-qa="file-upload-urgent"]')
  #shiftCareCheckbox = new Checkbox(
    this.page.find('[data-qa="checkbox-service-need-shift-care"]')
  )
  #shiftCareAttachmentFileUpload = this.page.find(
    '[data-qa="file-upload-shift-care"]'
  )

  applicantDeadIndicator = this.page.find('[data-qa="applicant-dead"]')

  async startEditing() {
    await this.#editButton.click()
  }

  async saveApplication() {
    await this.#saveButton.click()
  }

  async setUrgent() {
    if (await this.#urgentCheckbox.checked) {
      return
    }
    await this.#urgentCheckbox.click()
  }

  async uploadUrgentAttachment(filePath: string) {
    await new FileInput(
      this.#urgentAttachmentFileUpload.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(filePath)
  }

  async assertUrgentAttachmentUploaded(fileName: string) {
    await waitUntilEqual(
      () =>
        this.#urgentAttachmentFileUpload.find(
          '[data-qa="file-download-button"]'
        ).innerText,
      fileName
    )
  }

  async assertUrgencyAttachmentReceivedAtVisible(fileName: string) {
    const attachment = this.page.find(
      `[data-qa="urgent-attachment-${fileName}"]`
    )
    await attachment.waitUntilVisible()
    await attachment
      .find('[data-qa="attachment-received-at"]')
      .waitUntilVisible()
  }

  async setShiftCareNeeded() {
    if (await this.#shiftCareCheckbox.checked) {
      return
    }
    await this.#shiftCareCheckbox.click()
  }

  async uploadShiftCareAttachment(filePath: string) {
    await new FileInput(
      this.#shiftCareAttachmentFileUpload.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(filePath)
  }

  async deleteShiftCareAttachment(fileName: string) {
    await this.#shiftCareAttachmentFileUpload
      .find(`[data-qa="file-delete-button-${fileName}"]`)
      .click()
  }

  async assertShiftCareAttachmentUploaded(fileName: string) {
    await waitUntilEqual(
      () =>
        this.#shiftCareAttachmentFileUpload.find(
          '[data-qa="file-download-button"]'
        ).innerText,
      fileName
    )
  }

  async assertShiftCareAttachmentsDeleted() {
    await waitUntilFalse(
      () =>
        this.#shiftCareAttachmentFileUpload.find(
          '[data-qa="file-download-button"]'
        ).visible
    )
  }
}
