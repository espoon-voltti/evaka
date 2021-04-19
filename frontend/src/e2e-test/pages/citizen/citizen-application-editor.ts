// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { t, ClientFunction, Selector } from 'testcafe'
import { FormInput } from '../../utils/application-forms'
import { Checkbox } from '../../utils/helpers'
import { format } from 'date-fns'

const getWindowLocation = ClientFunction(() => window.location)

export default class CitizenApplicationEditor {
  readonly preferredStartDateInput = Selector(
    '[data-qa="preferredStartDate-input"]'
  )
  readonly preferredStartDateInputInfo = Selector(
    '[data-qa="preferredStartDate-input-info"]'
  )

  readonly applicationTypeTitle = Selector('[data-qa="application-type-title"]')
  readonly applicationChildNameTitle = Selector(
    '[data-qa="application-child-name-title"]'
  )
  readonly applicationHasErrorsTitle = Selector(
    '[data-qa="application-has-errors-title"]'
  )
  readonly applicationSection = (section: string) =>
    Selector(`[data-qa="${section}-section"]`)
  readonly applicationSectionHeader = (section: string) =>
    Selector(`[data-qa="${section}-section-header"]`)

  readonly saveAsDraftButton = Selector('[data-qa="save-as-draft-btn"]')

  readonly childStreetAddress = Selector('[data-qa="child-street-address"]')

  readonly urgentAttachmentsUpload = Selector('[data-qa="urgent-file-upload"]')
  readonly noGuardianEmailButton = Selector('[data-qa="noGuardianEmail-input"]')

  readonly urgentAttachmentDownloadButton = (file: string) =>
    this.urgentAttachmentsUpload
      .find('[data-qa="uploaded-files"]')
      .find('[data-qa="file-download-button"]')
      .withText(file)

  async getApplicationId() {
    const location = await getWindowLocation()
    const urlParts = location.href.split('/')
    return urlParts[urlParts.length - 2]
  }

  async goToVerify() {
    await t.click('[data-qa="verify-btn"]')
  }

  async verifyAndSend() {
    await this.goToVerify()
    await new Checkbox(Selector('[data-qa="verify-checkbox"]')).click()
    await t.click('[data-qa="send-btn"]')
  }

  async saveAsDraft() {
    await t.click(this.saveAsDraftButton)
  }

  async acknowledgeSendSuccess() {
    await t.click(
      Selector(
        '[data-qa="info-message-application-sent"] [data-qa="modal-okBtn"]'
      )
    )
  }

  async isSectionOpen(section: string) {
    return (await this.applicationSectionHeader(section).classNames).includes(
      'open'
    )
  }

  async openSection(section: string) {
    if (!(await this.isSectionOpen(section))) {
      await t.click(this.applicationSectionHeader(section))
    }
  }

  async inputString(section: string, field: string, value: string) {
    const input = this.applicationSection(section).find(
      `[data-qa="${field}-input"]`
    )
    await t.typeText(input, value, { replace: true })
  }

  async setCheckbox(section: string, field: string, value: boolean) {
    const checkbox = new Checkbox(
      this.applicationSection(section).find(`[data-qa="${field}-input"]`)
    )

    if ((await checkbox.checked) !== value) {
      await checkbox.click()
    }
  }

  async selectUnit(name: string) {
    const input = this.applicationSection('unitPreference').find(
      '[data-qa="preferredUnits-input"]'
    )

    await t.click(input)
    await t.pressKey(
      name
        .split('')
        .map((c) => (c === ' ' ? 'space' : c))
        .join(' ')
    )
    await t.pressKey('enter')
  }

  async selectBooleanRadio(section: string, field: string, value: boolean) {
    await new Checkbox(
      this.applicationSection(section).find(
        `[data-qa="${field}-input-${String(value)}"]`
      )
    ).click()
  }

  async fillData(data: FormInput) {
    for (const section of Object.keys(data)) {
      await this.openSection(section)
      for (const field of Object.keys(data[section])) {
        // eslint-disable-next-line
        const value = data[section][field]
        if (data.unitPreference?.preferredUnits && field === 'preferredUnits') {
          for (const unit of data.unitPreference.preferredUnits) {
            await this.selectUnit(unit.name)
          }
        } else if (field === 'partTime') {
          await this.selectBooleanRadio(section, field, value)
        } else if (field === 'siblingBasis') {
          await this.setCheckbox(section, field, value)
          await new Checkbox(Selector('[data-qa="other-sibling"]')).click()
        } else if (field === 'otherGuardianAgreementStatus') {
          await new Checkbox(
            Selector(
              `[data-qa="otherGuardianAgreementStatus-${String(value)}"]`
            )
          ).click()
        } else if (
          data.contactInfo?.otherChildren &&
          field === 'otherChildren'
        ) {
          for (let i = 0; i < data.contactInfo?.otherChildren?.length; i++) {
            if (
              !(await Selector(
                `[data-qa="otherChildren[${i}].firstName-input"]`
              ).exists)
            ) {
              await t.click('[data-qa="add-other-child"]')
            }
            await this.inputString(
              section,
              `otherChildren[${i}].firstName`,
              data.contactInfo.otherChildren[i].firstName
            )
            await this.inputString(
              section,
              `otherChildren[${i}].lastName`,
              data.contactInfo.otherChildren[i].lastName
            )
            await this.inputString(
              section,
              `otherChildren[${i}].socialSecurityNumber`,
              data.contactInfo.otherChildren[i].socialSecurityNumber
            )
          }
        } else if (typeof value === 'string') {
          await this.inputString(section, field, value)
        } else if (typeof value === 'boolean') {
          await this.setCheckbox(section, field, value)
        }
      }
    }
  }

  async setPreferredStartDate(date: Date) {
    await t.typeText(this.preferredStartDateInput, format(date, 'dd.MM.yyyy'), {
      replace: true
    })
  }

  async uploadUrgentFile(file: string) {
    await t.setFilesToUpload(
      this.urgentAttachmentsUpload.find('[data-qa="btn-upload-file"]'),
      [file]
    )
  }

  async assertUrgentFileHasBeenUploaded(filename: string) {
    await t
      .expect(this.urgentAttachmentDownloadButton(filename).exists)
      .ok({ timeout: 2000 }) // Uploading test files shouldn't take very long
  }

  async assertPreferredStartDateInputInfo(
    expected: boolean,
    expectedText = ''
  ) {
    expected
      ? await t
          .expect(
            this.preferredStartDateInputInfo.with({ timeout: 2000 }).textContent
          )
          .eql(expectedText)
      : await t
          .expect(
            this.preferredStartDateInputInfo.with({ timeout: 2000 }).visible
          )
          .eql(false)
  }

  async assertPreferredStartDateProcessingWarningIsShown(expected: boolean) {
    await t
      .expect(
        Selector('[data-qa="daycare-processing-time-warning"]', {
          timeout: 2000
        }).visible
      )
      .eql(expected)
  }

  async assertChildStreetAddress(expected: string | null) {
    expected != null
      ? await t
          .expect(this.childStreetAddress.with({ timeout: 2000 }).textContent)
          .eql(expected)
      : await t
          .expect(this.childStreetAddress.with({ timeout: 2000 }).visible)
          .eql(false)
  }
}
