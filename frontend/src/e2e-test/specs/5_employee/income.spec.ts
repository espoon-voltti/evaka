// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  EmployeeId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import { evakaUserId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import ErrorModal from '../../pages/employee/error-modal'
import type { IncomeSection } from '../../pages/employee/guardian-information'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { waitUntilEqual, waitUntilFalse, waitUntilTrue } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personId: PersonId
let incomesSection: IncomeSection
let placementStart: LocalDate
let placementEnd: LocalDate
let financeAdminId: EmployeeId
beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  personId = testAdult.id

  const financeAdmin = await Fixture.employee().financeAdmin().save()
  financeAdminId = financeAdmin.id

  await Fixture.fridgeChild({
    headOfChild: testAdult.id,
    childId: testChild.id,
    startDate: LocalDate.of(2020, 1, 1),
    endDate: LocalDate.of(2020, 12, 31)
  }).save()

  placementStart = LocalDate.of(2020, 1, 1)
  placementEnd = LocalDate.of(2020, 3, 31)

  await Fixture.placement({
    childId: testChild.id,
    unitId: testDaycare.id,
    startDate: placementStart,
    endDate: placementEnd
  }).save()

  page = await Page.open()
  await employeeLogin(page, financeAdmin)
  await page.goto(config.employeeUrl + '/profile/' + personId)

  const guardianInformationPage = new GuardianInformationPage(page)
  incomesSection = await guardianInformationPage.openCollapsible('incomes')
})

describe('Income', () => {
  it('Create a new max fee accepted income', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.incomeListItemCount(), 1)
  })

  it('Create a new income with multiple values', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('INCOME')

    await incomesSection.fillIncome('MAIN_INCOME', '5000')
    await incomesSection.fillIncome('SECONDARY_INCOME', '100,50')
    await incomesSection.fillIncome('ALL_EXPENSES', '35,75')
    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.incomeListItemCount(), 1)
    await waitUntilEqual(() => incomesSection.getIncomeSum(), '5100,50 €')
    await waitUntilEqual(() => incomesSection.getExpensesSum(), '35,75 €')
  })

  it('Create a new income without end date', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.chooseIncomeEffect('INCOME')
    // end date is set to +1 year
    await incomesSection.incomeEndDateInput.assertValueEquals('31.12.2020')
    // end date can be removed
    await incomesSection.fillIncomeEndDate('')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.incomeListItemCount(), 1)
  })

  test('Income editor save button is disabled with invalid values', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await waitUntilFalse(() => incomesSection.saveIsDisabled())

    // not a number
    await incomesSection.fillIncome('MAIN_INCOME', 'asd')
    await waitUntilTrue(() => incomesSection.saveIsDisabled())

    // too many decimals
    await incomesSection.fillIncome('MAIN_INCOME', '123,123')
    await waitUntilTrue(() => incomesSection.saveIsDisabled())
  })

  it('Existing income item can have its values updated', async () => {
    // create new income item
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('INCOME')
    await incomesSection.fillIncome('MAIN_INCOME', '5000')
    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.getIncomeSum(), '5000 €')
    await waitUntilEqual(() => incomesSection.getExpensesSum(), '0 €')

    // edit existing item
    await incomesSection.edit()

    await incomesSection.fillIncome('SECONDARY_INCOME', '200')
    await incomesSection.fillIncome('ALL_EXPENSES', '300')
    await incomesSection.fillIncome('MAIN_INCOME', '')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.getIncomeSum(), '200 €')
    await waitUntilEqual(() => incomesSection.getExpensesSum(), '300 €')
  })

  it('Income coefficients are saved and affect the sum', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()

    await incomesSection.chooseIncomeEffect('INCOME')

    await incomesSection.fillIncome('MAIN_INCOME', '100000')
    await incomesSection.chooseCoefficient('MAIN_INCOME', 'YEARLY')

    await incomesSection.fillIncome('ALIMONY', '50')
    await incomesSection.fillIncome('ALL_EXPENSES', '35,75')

    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.getIncomeSum(), '8380 €') // (100000 / 12) + 50
    await waitUntilEqual(() => incomesSection.getExpensesSum(), '35,75 €')
  })

  it('Non-contiguous incomes warning', async () => {
    await incomesSection.openNewIncomeForm()
    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.save()

    await incomesSection.openNewIncomeForm()
    await incomesSection.fillIncomeStartDate('1.3.2020')
    await incomesSection.fillIncomeEndDate('31.3.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.save()

    const errorModal = new ErrorModal(page)
    await errorModal.ensureTitle('Tulotiedot puuttuvat joiltain päiviltä')
  })

  it('Overlapping incomes error', async () => {
    await incomesSection.openNewIncomeForm()
    await incomesSection.fillIncomeStartDate('1.2.2020')
    await incomesSection.fillIncomeEndDate('31.3.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.save()

    await incomesSection.openNewIncomeForm()
    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('15.2.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.saveFailing()

    const errorModal = new ErrorModal(page)
    await errorModal.ensureText(
      'Ajanjaksolle on jo tallennettu tulotietoja! Tarkista tulotietojen voimassaoloajat.'
    )
  })

  it('Overlapping income may be automatically ended', async () => {
    await incomesSection.openNewIncomeForm()
    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.3.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.save()

    await incomesSection.openNewIncomeForm()
    await incomesSection.fillIncomeStartDate('1.2.2020')
    await incomesSection.fillIncomeEndDate('15.6.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.incomeListItemCount(), 2)
    await incomesSection.incomeListItems
      .nth(0)
      .assertText((s) =>
        s.includes('Tulotiedot ajalle 01.02.2020 - 15.06.2020')
      )
    await incomesSection.incomeListItems
      .nth(1)
      .assertText((s) =>
        s.includes('Tulotiedot ajalle 01.01.2020 - 31.01.2020')
      )
  })

  it('Attachments can be added', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.attachmenUpload.uploadTestFile()
    await incomesSection.save()
    await waitUntilEqual(() => incomesSection.getAttachmentCount(), 1)

    await incomesSection.edit()

    await incomesSection.attachmenUpload.uploadTestFile()
    await incomesSection.save()
    await waitUntilEqual(() => incomesSection.getAttachmentCount(), 2)
  })

  it('Income with attachment can be deleted', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.attachmenUpload.uploadTestFile()
    await incomesSection.save()
    await waitUntilEqual(() => incomesSection.getAttachmentCount(), 1)

    await incomesSection.deleteIncomeItem(0)

    await waitUntilEqual(() => incomesSection.incomeListItemCount(), 0)
  })

  it('Attachment can be deleted while editing income', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.attachmenUpload.uploadTestFile()
    await incomesSection.save()
    await waitUntilEqual(() => incomesSection.getAttachmentCount(), 1)

    await incomesSection.edit()
    await incomesSection.attachmenUpload.deleteUploadedFile(0)
    await incomesSection.cancelEdit()

    await waitUntilEqual(() => incomesSection.getAttachmentCount(), 0)
  })

  it('Income notifications are shown', async () => {
    const incomeEndDate = placementEnd.subMonths(1)
    await Fixture.income({
      personId: personId,
      validFrom: placementStart,
      validTo: incomeEndDate,
      modifiedBy: evakaUserId(financeAdminId),
      modifiedAt: placementStart.toHelsinkiDateTime(LocalTime.of(0, 0))
    }).save()

    await Fixture.incomeNotification({
      receiverId: personId,
      notificationType: 'INITIAL_EMAIL',
      created: incomeEndDate.subWeeks(2).toHelsinkiDateTime(LocalTime.of(6, 0))
    }).save()

    await Fixture.incomeNotification({
      receiverId: personId,
      notificationType: 'REMINDER_EMAIL',
      created: incomeEndDate.subWeeks(1).toHelsinkiDateTime(LocalTime.of(6, 0))
    }).save()

    await Fixture.incomeNotification({
      receiverId: personId,
      notificationType: 'NEW_CUSTOMER',
      created: incomeEndDate.subDays(1).toHelsinkiDateTime(LocalTime.of(6, 0))
    }).save()

    await page.reload()
    await incomesSection.toggleNotificationsCollapsible()
    await waitUntilEqual(() => incomesSection.incomeNotificationRows.count(), 3)
    await incomesSection.incomeNotificationRows
      .nth(0)
      .assertTextEquals('28.02.2020 06:00 (Aloittava asiakas)')
    await incomesSection.incomeNotificationRows
      .nth(1)
      .assertTextEquals('22.02.2020 06:00 (Toinen muistutus)')
    await incomesSection.incomeNotificationRows
      .nth(2)
      .assertTextEquals('15.02.2020 06:00 (Ensimmäinen muistutus)')
  })
})
