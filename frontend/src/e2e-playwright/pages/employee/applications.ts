// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { Checkbox, RawElement } from 'e2e-playwright/utils/element'
import { waitUntilEqual, waitUntilFalse } from 'e2e-playwright/utils'

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

  #editButton = new RawElement(this.page, '[data-qa="edit-application"]')
  #saveButton = new RawElement(this.page, '[data-qa="save-application"]')
  #urgentCheckbox = new Checkbox(this.page, '[data-qa="checkbox-urgent"]')
  #urgentAttachmentFileUploadSelector = '[data-qa="file-upload-urgent"]'
  #urgentAttachmentFileUpload = new RawElement(
    this.page,
    this.#urgentAttachmentFileUploadSelector
  )
  #shiftCareCheckbox = new Checkbox(
    this.page,
    '[data-qa="checkbox-service-need-shift-care"]'
  )
  #shiftCareAttachmentFileUploadSelector = '[data-qa="file-upload-shift-care"]'
  #shiftCareAttachmentFileUpload = new RawElement(
    this.page,
    this.#shiftCareAttachmentFileUploadSelector
  )

  applicantDeadIndicator = new RawElement(
    this.page,
    '[data-qa="applicant-dead"]'
  )

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
    await this.page.setInputFiles(
      `${this.#urgentAttachmentFileUploadSelector} [data-qa="btn-upload-file"]`,
      filePath
    )
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
    const attachment = new RawElement(
      this.page,
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
    await this.page.setInputFiles(
      `${
        this.#shiftCareAttachmentFileUploadSelector
      } [data-qa="btn-upload-file"]`,
      filePath
    )
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
