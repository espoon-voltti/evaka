// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { JsonOf } from 'lib-common/json'

import { waitUntilDefined, waitUntilEqual } from '../../utils'
import { FormInput, Section, sections } from '../../utils/application-forms'
import { Checkbox, FileInput, Page, Radio, TextInput } from '../../utils/page'

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
    await this.#applicationTitle.assertTextEquals(title)

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

  async viewApplication(applicationId: string) {
    await this.#openApplicationButton(applicationId).click()
    return new CitizenApplicationReadView(this.page)
  }

  async assertChildIsShown(childId: string, childName: string) {
    await this.#childTitle(childId).assertTextEquals(childName)
  }

  async assertApplicationIsListed(
    id: string,
    title: string,
    unitName: string,
    preferredStartDate: string,
    status: string
  ) {
    await this.#applicationType(id).assertTextEquals(title)
    await this.#applicationUnit(id).assertTextEquals(unitName)
    await this.#applicationPreferredStartDate(id).assertTextEquals(
      preferredStartDate
    )
    await this.#applicationStatus(id).assertText((text) =>
      text.toLowerCase().includes(status.toLowerCase())
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

class CitizenApplicationReadView {
  constructor(private readonly page: Page) {}

  contactInfoSection = this.page.findByDataQa('contact-info-section')
  unitPreferenceSection = this.page.findByDataQa('unit-preference-section')
}

class CitizenApplicationEditor {
  constructor(private readonly page: Page) {}

  #verifyButton = this.page.find('[data-qa="verify-btn"]')
  #verifyCheckbox = new Checkbox(this.page.find('[data-qa="verify-checkbox"]'))
  #allowOtherGuardianAccess = new Checkbox(
    this.page.findByDataQa('allow-other-guardian-access')
  )
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

  saveAsDraftButton = this.page.findByDataQa('save-as-draft-btn')
  modalOkBtn = this.page.findByDataQa('modal-okBtn')
  guardianPhoneInput = new TextInput(
    this.page.findByDataQa('guardianPhone-input')
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

  async verifyAndSend({ hasOtherGuardian }: { hasOtherGuardian: boolean }) {
    await this.goToVerification()
    await this.#verifyCheckbox.check()
    if (hasOtherGuardian) {
      await this.#allowOtherGuardianAccess.check()
    }
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

  async fillInput(
    field: string,
    value: string,
    clearFirst = true,
    pressEnterAfter = false
  ) {
    const element = new TextInput(this.page.find(`[data-qa="${field}-input"]`))
    if (clearFirst) {
      await element.clear()
    }
    await element.type(value)
    if (pressEnterAfter) {
      await element.press('Enter')
    }
  }

  async fillData(data: JsonOf<FormInput>) {
    await sections
      .map((section) => [section, data[section]])
      .filter(
        (
          pair
        ): pair is [Section, Partial<JsonOf<ApplicationFormData>[Section]>] =>
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
          } else if (field === 'otherGuardianAgreementStatus' && value) {
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
            await this.fillInput(
              field,
              value,
              true,
              field.toLowerCase().indexOf('date') >= 0
            )
          } else if (typeof value === 'boolean') {
            await this.setCheckbox(field, value)
          }
        }
      }, Promise.resolve())
  }

  async assertChildAddress(fullAddress: string) {
    await this.openSection('contactInfo')
    await this.page
      .find('[data-qa="child-street-address"]')
      .assertTextEquals(fullAddress)
  }

  async setPreferredStartDate(formattedDate: string) {
    await this.openSection('serviceNeed')
    await this.#preferredStartDateInput.clear()
    await this.#preferredStartDateInput.type(formattedDate)
    await this.#preferredStartDateInput.press('Enter')
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
    await this.#preferredStartDateInfo.assertTextEquals(infoText)
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
    await this.page
      .find('[data-qa="uploaded-files"] [data-qa="file-download-button"]')
      .assertText((text) => text.includes(attachmentFileName))
  }

  async assertUrgencyFileDownload() {
    const [popup] = await Promise.all([
      this.page.waitForPopup(),
      this.page
        .find('[data-qa="service-need-urgency-attachment-download"]')
        .click()
    ])
    await popup.waitForSelector('img:not([src=""])')
  }
}
