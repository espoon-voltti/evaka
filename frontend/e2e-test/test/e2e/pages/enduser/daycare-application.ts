// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { addDays, addMonths, format } from 'date-fns'
import { roundToBusinessDay } from '../../utils/dates'
import { ApplicationRow } from './applications'

export default class DaycareApplication {
  private readonly datePicker = Selector(
    '#daycare-application-form .date-picker'
  )

  readonly checkAndSendBtn = Selector('#preview-and-send')
  readonly preferredUnitDetails = Selector('.unit-container')
  readonly preferredUnitDetailsTitle = Selector('.unit-title')
  readonly guardianInformation = Selector('.title').withText('Huoltajan tiedot')
  readonly additionalDetails = Selector('.title').withText('Lisätiedot')
  readonly placeList = Selector('#list-select-daycare')
  readonly additionalDetailsTextarea = Selector(
    '[name="additionalDetails.otherInfo"]'
  )
  readonly saveAsDraftButton = Selector('#save-as-draft')
  readonly agreementStatusAgreed = Selector(
    '[data-qa="agreement-status-agreed"]'
  )
  readonly agreementStatusNotAgreed = Selector(
    '[data-qa="agreement-status-not-agreed"]'
  )
  readonly otherGuardianTel = Selector('[name="guardian2.phoneNumber"]')
  readonly otherGuardianEmail = Selector('[name="guardian2.email"]')
  readonly guardianPhoneNumber = Selector('[name="guardian.phoneNumber"]')
  readonly guardianEmail = Selector('[name="guardian.email"]')
  private readonly preschoolDaycareCheckbox = Selector(
    '[name="connectedDaycare"]'
  )
    .parent()
    .find('label')
  private readonly startDate = this.datePicker.find('input')
  private readonly startTime = Selector('#daycare-start')
  private readonly endTime = Selector('#daycare-end')
  private readonly startTimeInput = Selector('#daycare-start > div > input')
  private readonly endTimeInput = Selector('#daycare-end > div > input')
  private readonly placeItem = (unitName: string) =>
    Selector('.multiselect__option').withText(unitName)

  private readonly serviceSection = Selector('[data-qa="service-section"]')
  private readonly preferredUnit = Selector(
    '[data-qa="preferred-units-section"]'
  )
  private readonly personalInfo = Selector('[data-qa="personal-info-section"]')
  private readonly payment = Selector('[data-qa="payment-section"]')
  private readonly additionalInfo = Selector('[data-qa="additional-section"]')

  private readonly sendBtn = Selector('#daycare-application-btn')
  private readonly summaryHeadline = Selector('h2').withText(
    'hakemuksen tarkistaminen'
  )
  private readonly summaryCheckbox = Selector('.summary-checkbox')
  protected readonly applicationId = Selector(
    '#daycare-application-form'
  ).getAttribute('data-application-id')

  getApplicationId() {
    return this.applicationId
  }

  async selectDaycareTime() {
    await t.click(this.startTime)
    await t.click(this.endTime)
    await t.typeText(this.startTimeInput, '08:00')
    await t.typeText(this.endTimeInput, '16:00')
    await t.expect(this.startTimeInput.value).eql('08:00')
    await t.expect(this.endTimeInput.value).eql('16:00')
  }

  async openSection(section: string) {
    let selector
    switch (section) {
      case 'service':
        selector = this.serviceSection
        break
      case 'preferredunits':
        selector = this.preferredUnit
        break
      case 'personalinfo':
        selector = this.personalInfo
        break
      case 'payment':
        selector = this.payment
        break
      case 'additionaldetails':
        selector = this.additionalInfo
        break
      default:
        throw new Error('Unknown section')
    }

    if (await selector.hasAttribute('data-open')) return null
    return t.click(selector)
  }

  async selectPreferredUnits(unitName: string) {
    await this.openSection('preferredunits')
    await t.expect(this.placeList.visible).ok()
    await t.click(this.placeList).click(this.placeItem(unitName))
    await t.expect(this.preferredUnitDetails.visible).ok()
    await t.expect(this.preferredUnitDetailsTitle.innerText).eql(unitName)
  }

  async fillGuardianInformation() {
    await this.openSection('personalinfo')
    await t.expect(this.guardianInformation.visible).ok()
    await t
      .typeText(this.guardianPhoneNumber, '358501234567', { paste: true })
      .typeText(this.guardianEmail, 'foo@espoo.fi', { paste: true })
    await t.expect(this.guardianPhoneNumber.value).eql('358501234567')
    await t.expect(this.guardianEmail.value).eql('foo@espoo.fi')
  }

  async fillOtherGuardianAgreedInformation() {
    await this.openSection('personalinfo')
    await t.expect(this.agreementStatusAgreed.visible).ok()
    await t.click(this.agreementStatusAgreed)
  }

  async fillOtherGuardianNotAgreedInformation() {
    await this.openSection('personalinfo')
    await t.expect(this.agreementStatusNotAgreed.visible).ok()
    await t.click(this.agreementStatusNotAgreed)
  }

  async fillContactDetails(tel: string, email: string) {
    await this.openSection('personalinfo')
    await t.typeText(this.otherGuardianTel, tel, { paste: true })
    await t.typeText(this.otherGuardianEmail, email, { paste: true })
  }

  async assertAgreementStatusNotAgreed(
    expectedTel: string,
    expectedEmail: string
  ) {
    await this.openSection('personalinfo')
    await t.expect(this.otherGuardianTel.value).eql(expectedTel)
    await t.expect(this.otherGuardianEmail.value).eql(expectedEmail)
  }

  async fillAdditionalDetails() {
    await this.openSection('additionaldetails')
    await t.expect(this.additionalDetails.visible).ok()
    await t.typeText(
      this.additionalDetailsTextarea,
      'Tässä hieno lisätietoteksti',
      { paste: true }
    )
    await t
      .expect(this.additionalDetailsTextarea.value)
      .eql('Tässä hieno lisätietoteksti')
  }

  async checkAndSend() {
    await t.wait(1000) // Without this the next button click does nothing every now and then
    await t.click(this.checkAndSendBtn)
    await t.expect(this.summaryHeadline.visible).ok()
    await t.click(this.summaryCheckbox)
    await t.click(this.sendBtn)
  }

  async assertApplicationStatus(applicationId: string, statusText: string) {
    const applicationRow = new ApplicationRow(applicationId)
    await t.expect(applicationRow.applicationListItem.exists).ok()
    const applicationStatus = await applicationRow.getStatus()
    await t.expect(applicationStatus).eql(statusText)
  }

  async fillInBasicDaycareApplicationDetails(preferredUnitName: string) {
    const startDate = roundToBusinessDay(addDays(addMonths(new Date(), 3), 2))
    await t.typeText(this.startDate, format(startDate, 'dd.MM.yyyy'))
    await this.selectDaycareTime()
    await this.selectPreferredUnits(preferredUnitName)
    await this.fillGuardianInformation()
    await this.fillAdditionalDetails()
    return startDate
  }

  async fillInBasicPreschoolApplicationDetails(
    preferredUnitName: string,
    { preschoolDaycare }: { preschoolDaycare?: boolean } = {}
  ) {
    const startDate = roundToBusinessDay(new Date())
    await t.typeText(this.startDate, format(startDate, 'dd.MM.yyyy'))

    if (preschoolDaycare) {
      await t.click(this.preschoolDaycareCheckbox)
      await t.typeText(this.startTimeInput, '08:00')
      await t.typeText(this.endTimeInput, '16:00')
    }

    await this.selectPreferredUnits(preferredUnitName)
    await this.fillGuardianInformation()
    await this.fillAdditionalDetails()
    return startDate
  }
}
