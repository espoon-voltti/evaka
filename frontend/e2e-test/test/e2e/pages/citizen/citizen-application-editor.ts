import { t, ClientFunction, Selector } from 'testcafe'
import { FormInput } from '../../utils/application-forms'

const getWindowLocation = ClientFunction(() => window.location)

export default class CitizenApplicationEditor {
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
    await t.click('[data-qa="verify-checkbox-icon"]')
    await t.click('[data-qa="send-btn"]')
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
    const input = this.applicationSection(section).find(
      `[data-qa="${field}-input"]`
    )

    const inputIcon = this.applicationSection(section).find(
      `[data-qa="${field}-input-icon"]`
    )

    if ((await input.checked) !== value) {
      await t.click(inputIcon)
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
    const radio = this.applicationSection(section).find(
      // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
      `[data-qa="${field}-input-${value}-icon"]`
    )

    await t.click(radio)
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
          await t.click(Selector('[data-qa="other-sibling-icon"]'))
        } else if (field === 'otherGuardianAgreementStatus') {
          await t.click(
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            Selector(`[data-qa="otherGuardianAgreementStatus-${value}-icon"]`)
          )
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
}
