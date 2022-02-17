// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'

import { waitUntilEqual, waitUntilDefined, waitUntilTrue } from '../../utils'
import { FormInput, Section, sections } from '../../utils/application-forms'
import { Page, Checkbox, Radio, TextInput, FileInput } from '../../utils/page'

export default class CitizenApplicationsPage {
  constructor(private readonly page: Page) {}

  #newApplicationButton = (childId: string) =>
    this.page.find(`[data-qa="new-application-${childId}"]`)
  #applicationTypeRadio = (type: 'DAYCARE' | 'PRESCHOOL' | 'CLUB') =>
    new Radio(this.page.find(`[data-qa="type-radio-${type}"]`))
  #createNewApplicationButton = this.page.find('[data-qa="submit"]')
  #transferApplicationNotification = this.page.find(
    '[data-qa="transfer-application-notification"]'
  )
  #duplicateApplicationNotification = this.page.find(
    '[data-qa="duplicate-application-notification"]'
  )
  #applicationTitle = this.page.find('[data-qa="application-type-title"]')
  #openApplicationButton = (id: string) =>
    this.page.find(`[data-qa="button-open-application-${id}"]`)
  #cancelApplicationButton = (id: string) =>
    this.page.find(`[data-qa="button-remove-application-${id}"]`)
  #childTitle = (childId: string) =>
    this.page.find(`[data-qa="title-applications-child-name-${childId}"]`)
  #applicationType = (id: string) =>
    this.page.find(`[data-qa="title-application-type-${id}"]`)
  #applicationUnit = (id: string) =>
    this.page.find(`[data-qa="application-unit-${id}"]`)
  #applicationPreferredStartDate = (id: string) =>
    this.page.find(`[data-qa="application-period-${id}"]`)
  #applicationStatus = (id: string) =>
    this.page.find(`[data-qa="application-status-${id}"]`)

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
    await this.page.find('[data-qa="modal-okBtn"]').click()
  }

  async assertApplicationDoesNotExist(id: string) {
    await this.#applicationType(id).waitUntilHidden()
  }

  async assertApplicationExists(id: string) {
    await this.#applicationType(id).waitUntilVisible()
  }
}

class CitizenApplicationEditor {
  constructor(private readonly page: Page) {}

  #verifyButton = this.page.find('[data-qa="verify-btn"]')
  #verifyCheckbox = new Checkbox(this.page.find('[data-qa="verify-checkbox"]'))
  #sendButton = this.page.find('[data-qa="send-btn"]')
  #applicationSentModal = this.page.find(
    '[data-qa="info-message-application-sent"]'
  )
  #errorsTitle = this.page.find('[data-qa="application-has-errors-title"]')
  #section = (name: string) => this.page.find(`[data-qa="${name}-section"]`)
  #sectionHeader = (name: string) =>
    this.page.find(`[data-qa="${name}-section-header"]`)
  #preferredUnitsInput = new TextInput(
    this.page.find('[data-qa="preferredUnits-input"] input')
  )
  #preferredStartDateInput = new TextInput(
    this.page.find('[data-qa="preferredStartDate-input"]')
  )
  #preferredStartDateWarning = this.page.find(
    '[data-qa="daycare-processing-time-warning"]'
  )
  #preferredStartDateInfo = this.page.find(
    '[data-qa="preferredStartDate-input-info"]'
  )

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="applications-list"][data-isloading="false"]')
      .waitUntilVisible()
  }

  getNewApplicationId() {
    const urlParts = this.page.url.split('/')
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
      this.page.find(`[data-qa="${field}-input-${String(value)}"]`)
    ).click()
  }

  async setCheckbox(field: string, value: boolean) {
    const element = new Checkbox(this.page.find(`[data-qa="${field}-input"]`))

    if ((await element.checked) !== value) {
      await element.click()
    }
  }

  async fillInput(field: string, value: string, clearFirst = true) {
    const element = new TextInput(this.page.find(`[data-qa="${field}-input"]`))
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
            // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
            await this.selectBooleanRadio(field, value)
          } else if (field === 'siblingBasis') {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
            await this.setCheckbox(field, value)
            await new Checkbox(
              this.page.find('[data-qa="other-sibling"]')
            ).click()
          } else if (field === 'otherGuardianAgreementStatus') {
            await new Radio(
              this.page.find(
                `[data-qa="otherGuardianAgreementStatus-${String(value)}"]`
              )
            ).click()
          } else if (
            data.contactInfo?.otherChildren &&
            field === 'otherChildren'
          ) {
            for (let i = 0; i < data.contactInfo?.otherChildren?.length; i++) {
              if (i > 0) {
                await this.page.find('[data-qa="add-other-child"]').click()
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
      () => this.page.find('[data-qa="child-street-address"]').innerText,
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
      await this.#preferredStartDateInfo.waitUntilHidden()
      return
    }
    await waitUntilEqual(() => this.#preferredStartDateInfo.innerText, infoText)
  }

  async markApplicationUrgentAndAddAttachment(attachmentFilePath: string) {
    await this.openSection('serviceNeed')
    await this.setCheckbox('urgent', true)
    await new FileInput(
      this.page.find(
        '[data-qa="urgent-file-upload"] [data-qa="btn-upload-file"]'
      )
    ).setInputFiles(attachmentFilePath)
  }

  async assertAttachmentUploaded(attachmentFileName: string) {
    await waitUntilTrue(async () =>
      (
        await this.page.find(
          '[data-qa="uploaded-files"] [data-qa="file-download-button"]'
        ).innerText
      ).includes(attachmentFileName)
    )
  }

  async assertUrgencyFileDownload(fileName: string) {
    const [download] = await Promise.all([
      this.page.waitForDownload(),
      this.page
        .find('[data-qa="service-need-urgency-attachment-download"]')
        .click()
    ])
    expect(download.suggestedFilename()).toEqual(fileName)
  }
}
