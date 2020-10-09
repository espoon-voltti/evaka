// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import EnduserPage from './enduser-navigation'
import { addMonths, addWeeks, format } from 'date-fns'
import { roundToBusinessDay } from '../../utils/dates'
import { ApplicationRow } from './applications'

export default class ClubApplication {
  private readonly datePickerWrapper = Selector(
    '#club-care-date-picker-wrapper'
  )
  private readonly datePickerInput = this.datePickerWrapper.find('input')
  private readonly desiredPlaceHeader = Selector('#club-form-section-clubs')
  private readonly personalInfoHeader = Selector(
    '#club-form-section-personal-details'
  )
  private readonly childHeadline = Selector('#title-child-information')
  private readonly childNationalityList = Selector('#childNationality')
  private readonly childNationalityItem = Selector(
    '.multiselect__option'
  ).withText('Suomi')
  private readonly childLanguageList = Selector('#list-child-language')
  private readonly childLanguageItem = Selector(
    '.multiselect__option'
  ).withText('suomi')
  private readonly guardianPhonenumber = Selector('#input-guardian1-phone')
  private readonly guardianEmail = Selector('#input-guardian1-email')
  readonly summaryHeadline = Selector('.title').withText(
    'hakemuksen tarkistaminen'
  )
  readonly summaryCheckbox = Selector('.summary-checkbox')
  readonly sendBtn = Selector('[data-qa="btn-send"]')

  protected readonly applicationId = Selector(
    '#club-application-form'
  ).getAttribute('data-application-id')
  protected readonly placeListItem = Selector('.multiselect__element').nth(0)

  readonly checkAndSendBtn = Selector('[data-qa="btn-check-and-send"]')
  readonly saveBtn = Selector('[data-qa="btn-save-application"]')
  readonly placeList = Selector('.daycare-select-list')
  readonly noSelectedUnits = Selector('.no-selected-units')
  readonly guardianPhonenumberContent = this.guardianPhonenumber.find('input')
  readonly guardianEmailContent = this.guardianEmail.find('input')

  getApplicationId() {
    return this.applicationId
  }

  // Section Desired place of care (Hakutoive)
  async expandDesiredPlaceSection() {
    await t.click(this.desiredPlaceHeader).expect(this.placeList.visible).ok()
  }

  async chooseDesiredPlace() {
    await t.click(this.placeList)
    await t.expect(this.placeListItem.visible).ok().click(this.placeListItem)
  }

  // Section Personal information (Henkilötiedot)
  async expandPersonalInfo() {
    await t
      .click(this.personalInfoHeader)
      .expect(this.childHeadline.visible)
      .ok()
  }

  // Child information
  async fillInChildInformation() {
    await t.click(this.childNationalityList).click(this.childNationalityItem)
    await t.click(this.childLanguageList).click(this.childLanguageItem)
  }

  // Guardian information
  async fillInGuardianInformation(
    phoneNumber = '358501234567',
    email = 'foo@espoo.fi'
  ) {
    await t
      .expect(this.guardianEmail.visible)
      .ok()
      .expect(this.guardianPhonenumber.visible)
      .ok()
      .typeText(this.guardianPhonenumber, phoneNumber, {
        paste: true,
        replace: true
      })
      .typeText(this.guardianEmail, email, { paste: true, replace: true })
  }

  async navigateToApplications() {
    const enduserNavigation = new EnduserPage()
    await enduserNavigation.login()
    // List applications & create new application
    await enduserNavigation.navigateToApplicationsTab()
  }

  async assertApplicationStatus(applicationId: string, statusText: string) {
    const applicationRow = new ApplicationRow(applicationId)
    await t.expect(applicationRow.applicationListItem.exists).ok()
    const applicationStatus = await applicationRow.getStatus()
    await t.expect(applicationStatus).eql(statusText)
  }

  async fillClubApplication() {
    // Select date
    const targetDate = roundToBusinessDay(addWeeks(new Date(), 1))
    await t.typeText(this.datePickerInput, format(targetDate, 'dd.MM.yyyy'))

    // Select preferred unit
    await this.expandDesiredPlaceSection()
    await t.expect(this.placeList.visible).ok()
    await this.chooseDesiredPlace()
    // Check that at least one unit is selected by verifying that no-selected-units element does not exist.
    await t.expect(this.noSelectedUnits.exists).notOk()

    // Fill missing guardian info
    await this.expandPersonalInfo()
    await this.fillInGuardianInformation()
    // Check that guardian contact information is filled in.
    await t
      .expect(this.guardianPhonenumberContent.value)
      .notEql('')
      .expect(this.guardianEmailContent.value)
      .notEql('')
  }

  async save() {
    await t.expect(this.saveBtn.visible).ok()
    await t.click(this.saveBtn)
  }

  async checkAndSend() {
    await t
      .click(this.checkAndSendBtn)
      .expect(this.summaryHeadline.exists)
      .ok()
      .click(this.summaryCheckbox)
      .click(this.sendBtn)
  }

  async modifyClubApplication() {
    const targetDate = roundToBusinessDay(addMonths(new Date(), 1))
    await t.typeText(this.datePickerInput, format(targetDate, 'dd.MM.yyyy'))

    // Select preferred unit
    await this.expandDesiredPlaceSection()
    await t.expect(this.placeList.visible).ok()
    await this.chooseDesiredPlace()
    // Check that at least one unit is selected by verifying that no-selected-units element does not exist.
    await t.expect(this.noSelectedUnits.exists).notOk()

    await this.expandPersonalInfo()
    await this.fillInGuardianInformation('0401234567', 'modify@muokattu.fi')

    // Check that guardian contact information is filled in.
    await t
      .expect(this.guardianPhonenumberContent.value)
      .notEql('')
      .expect(this.guardianEmailContent.value)
      .notEql('')
  }
}
