// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import {
  waitUntilEqual,
  waitUntilFalse,
  waitUntilDefined,
  waitUntilTrue
} from 'e2e-playwright/utils'
import {
  Checkbox,
  Radio,
  RawElement,
  RawTextInput
} from 'e2e-playwright/utils/element'
import {
  FormInput,
  Section,
  sections
} from 'e2e-playwright/utils/application-forms'
import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'

export default class CitizenApplicationsPage {
  constructor(private readonly page: Page) {}

  #newApplicationButton = (childId: string) =>
    new RawElement(this.page, `[data-qa="new-application-${childId}"]`)
  #applicationTypeRadio = (type: 'DAYCARE' | 'PRESCHOOL' | 'CLUB') =>
    new Radio(this.page, `[data-qa="type-radio-${type}"]`)
  #createNewApplicationButton = new RawElement(this.page, '[data-qa="submit"]')
  #transferApplicationNotification = new RawElement(
    this.page,
    '[data-qa="transfer-application-notification"]'
  )
  #duplicateApplicationNotification = new RawElement(
    this.page,
    '[data-qa="duplicate-application-notification"]'
  )
  #applicationTitle = new RawElement(
    this.page,
    '[data-qa="application-type-title"]'
  )
  #openApplicationButton = (id: string) =>
    new RawElement(this.page, `[data-qa="button-open-application-${id}"]`)
  #cancelApplicationButton = (id: string) =>
    new RawElement(this.page, `[data-qa="button-remove-application-${id}"]`)
  #childTitle = (childId: string) =>
    new RawElement(
      this.page,
      `[data-qa="title-applications-child-name-${childId}"]`
    )
  #applicationType = (id: string) =>
    new RawElement(this.page, `[data-qa="title-application-type-${id}"]`)
  #applicationUnit = (id: string) =>
    new RawElement(this.page, `[data-qa="application-unit-${id}"]`)
  #applicationPreferredStartDate = (id: string) =>
    new RawElement(this.page, `[data-qa="application-period-${id}"]`)
  #applicationStatus = (id: string) =>
    new RawElement(this.page, `[data-qa="application-status-${id}"]`)

  async createApplication(
    childId: string,
    type: 'DAYCARE' | 'PRESCHOOL' | 'CLUB'
  ) {
    await this.#newApplicationButton(childId).click()
    await this.#applicationTypeRadio(type).click()
    await this.#createNewApplicationButton.click()

    const title =
      type === 'DAYCARE'
        ? 'Varhaiskasvatus- ja palvelusetelihakemus'
        : type === 'PRESCHOOL'
        ? 'Ilmoittautuminen esiopetukseen'
        : type === 'CLUB'
        ? 'Kerhohakemus'
        : ''
    await waitUntilEqual(() => this.#applicationTitle.innerText, title)

    return new CitizenApplicationEditor(this.page)
  }

  async assertDuplicateWarningIsShown(
    childId: string,
    type: 'DAYCARE' | 'PRESCHOOL'
  ) {
    await this.#newApplicationButton(childId).click()
    await this.#applicationTypeRadio(type).click()
    await this.#duplicateApplicationNotification.waitUntilVisible()
  }

  async assertTransferNotificationIsShown(
    childId: string,
    type: 'DAYCARE' | 'PRESCHOOL'
  ) {
    await this.#newApplicationButton(childId).click()
    await this.#applicationTypeRadio(type).click()
    await this.#transferApplicationNotification.waitUntilVisible()
  }

  async editApplication(applicationId: string) {
    await this.#openApplicationButton(applicationId).click()

    return new CitizenApplicationEditor(this.page)
  }

  async assertChildIsShown(childId: string, childName: string) {
    await waitUntilEqual(() => this.#childTitle(childId).innerText, childName)
  }

  async assertApplicationIsListed(
    id: string,
    title: string,
    unitName: string,
    preferredStartDate: string,
    status: string
  ) {
    await waitUntilEqual(() => this.#applicationType(id).innerText, title)
    await waitUntilEqual(() => this.#applicationUnit(id).innerText, unitName)
    await waitUntilEqual(
      () => this.#applicationPreferredStartDate(id).innerText,
      preferredStartDate
    )
    await waitUntilTrue(async () =>
      (await this.#applicationStatus(id).innerText)
        .toLowerCase()
        .includes(status.toLowerCase())
    )
  }

  async cancelApplication(id: string) {
    await this.#cancelApplicationButton(id).click()
    await new RawElement(this.page, '[data-qa="modal-okBtn"]').click()
  }

  async assertApplicationDoesNotExist(id: string) {
    await waitUntilFalse(() => this.#applicationType(id).visible)
  }
}

class CitizenApplicationEditor {
  constructor(private readonly page: Page) {}

  #verifyButton = new RawElement(this.page, '[data-qa="verify-btn"]')
  #verifyCheckbox = new Checkbox(this.page, '[data-qa="verify-checkbox"]')
  #sendButton = new RawElement(this.page, '[data-qa="send-btn"]')
  #applicationSentModal = new RawElement(
    this.page,
    '[data-qa="info-message-application-sent"]'
  )
  #errorsTitle = new RawElement(
    this.page,
    '[data-qa="application-has-errors-title"]'
  )
  #section = (name: string) =>
    new RawElement(this.page, `[data-qa="${name}-section"]`)
  #sectionHeader = (name: string) =>
    new RawElement(this.page, `[data-qa="${name}-section-header"]`)
  #preferredUnitsInput = new RawElement(
    this.page,
    '[data-qa="preferredUnits-input"]'
  ).findInput('input')
  #preferredStartDateInput = new RawTextInput(
    this.page,
    '[data-qa="preferredStartDate-input"]'
  )
  #preferredStartDateWarning = new RawElement(
    this.page,
    '[data-qa="daycare-processing-time-warning"]'
  )
  #preferredStartDateInfo = new RawElement(
    this.page,
    '[data-qa="preferredStartDate-input-info"]'
  )

  getNewApplicationId() {
    const urlParts = this.page.url().split('/')
    return urlParts[urlParts.length - 2]
  }

  async goToVerification() {
    await this.#verifyButton.click()
  }

  async verifyAndSend() {
    await this.goToVerification()
    await this.#verifyCheckbox.click()
    await this.#sendButton.click()
    await this.#applicationSentModal.waitUntilVisible()
    await this.#applicationSentModal.find('[data-qa="modal-okBtn"]').click()
  }

  async assertErrorsExist() {
    await this.#errorsTitle.waitUntilVisible()
  }

  async openSection(section: string) {
    const status = await waitUntilDefined(() =>
      this.#section(section).getAttribute('data-status')
    )
    if (status !== 'open') {
      await this.#sectionHeader(section).click()
    }
  }

  async selectUnit(name: string) {
    await this.#preferredUnitsInput.type(name)
    await this.page.keyboard.press('Enter')
  }

  async selectBooleanRadio(field: string, value: boolean) {
    await new Radio(
      this.page,
      `[data-qa="${field}-input-${String(value)}"]`
    ).click()
  }

  async setCheckbox(field: string, value: boolean) {
    const element = new Checkbox(this.page, `[data-qa="${field}-input"]`)

    if ((await element.checked) !== value) {
      await element.click()
    }
  }

  async fillInput(field: string, value: string, clearFirst = true) {
    const element = new RawTextInput(this.page, `[data-qa="${field}-input"]`)
    if (clearFirst) {
      await element.clear()
    }
    await element.type(value)
  }

  async fillData(data: FormInput) {
    await sections
      .map((section) => [section, data[section]])
      .filter(
        (pair): pair is [Section, Partial<ApplicationFormData[Section]>] =>
          pair[1] !== undefined
      )
      .reduce(async (promise, [section, sectionData]) => {
        await promise
        await this.openSection(section)

        for (const [field, value] of Object.entries(sectionData)) {
          if (
            data.unitPreference?.preferredUnits &&
            field === 'preferredUnits'
          ) {
            for (const unit of data.unitPreference.preferredUnits) {
              await this.selectUnit(unit.name)
            }
          } else if (field === 'partTime') {
            await this.selectBooleanRadio(field, value)
          } else if (field === 'siblingBasis') {
            await this.setCheckbox(field, value)
            await new Checkbox(this.page, '[data-qa="other-sibling"]').click()
          } else if (field === 'otherGuardianAgreementStatus') {
            await new Radio(
              this.page,
              `[data-qa="otherGuardianAgreementStatus-${String(value)}"]`
            ).click()
          } else if (
            data.contactInfo?.otherChildren &&
            field === 'otherChildren'
          ) {
            for (let i = 0; i < data.contactInfo?.otherChildren?.length; i++) {
              if (i > 0) {
                await new RawElement(
                  this.page,
                  '[data-qa="add-other-child"]'
                ).click()
              }
              await this.fillInput(
                `otherChildren[${i}].firstName`,
                data.contactInfo.otherChildren[i].firstName
              )
              await this.fillInput(
                `otherChildren[${i}].lastName`,
                data.contactInfo.otherChildren[i].lastName
              )
              await this.fillInput(
                `otherChildren[${i}].socialSecurityNumber`,
                data.contactInfo.otherChildren[i].socialSecurityNumber
              )
            }
          } else if (
            typeof value === 'string' &&
            (field === 'startTime' || field === 'endTime')
          ) {
            const [hours, minutes] = value.split(':')
            await this.fillInput(field, hours)
            await this.fillInput(field, minutes, false)
          } else if (typeof value === 'string') {
            await this.fillInput(field, value)
          } else if (typeof value === 'boolean') {
            await this.setCheckbox(field, value)
          }
        }
      }, Promise.resolve())
  }

  async assertChildAddress(fullAddress: string) {
    await this.openSection('contactInfo')
    await waitUntilEqual(
      () =>
        new RawElement(this.page, '[data-qa="child-street-address"]').innerText,
      fullAddress
    )
  }

  async setPreferredStartDate(formattedDate: string) {
    await this.openSection('serviceNeed')
    await this.#preferredStartDateInput.clear()
    await this.#preferredStartDateInput.type(formattedDate)
    await this.page.keyboard.press('Tab')
  }

  async assertPreferredStartDateWarningIsShown(visible: boolean) {
    await waitUntilEqual(() => this.#preferredStartDateWarning.visible, visible)
  }

  async assertPreferredStartDateInfo(infoText: string | undefined) {
    if (infoText === undefined) {
      await waitUntilFalse(() => this.#preferredStartDateInfo.visible)
      return
    }
    await waitUntilEqual(() => this.#preferredStartDateInfo.innerText, infoText)
  }

  async markApplicationUrgentAndAddAttachment(attachmentFilePath: string) {
    await this.openSection('serviceNeed')
    await this.setCheckbox('urgent', true)
    await this.page.setInputFiles(
      '[data-qa="urgent-file-upload"] [data-qa="btn-upload-file"]',
      attachmentFilePath
    )
  }

  async assertAttachmentUploaded(attachmentFileName: string) {
    await waitUntilTrue(async () =>
      (
        await new RawElement(
          this.page,
          '[data-qa="uploaded-files"] [data-qa="file-download-button"]'
        ).innerText
      ).includes(attachmentFileName)
    )
  }

  async assertUrgencyFileDownload(fileName: string) {
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      new RawElement(
        this.page,
        '[data-qa="service-need-urgency-attachment-download"]'
      ).click()
    ])
    expect(download.suggestedFilename()).toEqual(fileName)
  }
}
