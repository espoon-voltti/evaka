// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'testcafe'
import EmployeeHome from '../../pages/employee/home'
import { Selector } from 'testcafe'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { cleanUpInvoicingDatabase } from '../../dev-api'
import { seppoAdminRole } from '../../config/users'
import FridgeHeadInformationPage from '../../pages/employee/fridge-head-information/fridge-head-information-page'

const page = new EmployeeHome()
const fridgeHeadInformation = new FridgeHeadInformationPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let personId: string

fixture('Invoicing - invoices')
  .meta({ type: 'regression', subType: 'invoices' })
  .page(page.url)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    personId = fixtures.enduserGuardianFixture.id
  })
  .beforeEach(async (t) => {
    await cleanUpInvoicingDatabase()
    await t.useRole(seppoAdminRole)
  })
  .after(async () => {
    await cleanUp()
  })

const newIncomeButton = Selector('[data-qa="add-income-button"]')
const incomeDateRange = Selector('[data-qa="income-date-range"]')
const incomeStartDateInput = incomeDateRange
  .find('[data-qa="date-range-input-start-date"]')
  .find('input')
const incomeEndDateInput = incomeDateRange
  .find('[data-qa="date-range-input-end-date"]')
  .find('input')
const incomeEffect = (effect: string) =>
  Selector(`[data-qa="income-effect-${effect}"]`)
const incomeInput = (type: string) =>
  Selector(`[data-qa="income-input-${type}"]`)
const saveIncomeButton = Selector('[data-qa="save-income"]')
const incomeSum = Selector('[data-qa="income-sum-income"]')
const expensesSum = Selector('[data-qa="income-sum-expenses"]')
const toggleIncomeItemButton = Selector('[data-qa="toggle-income-item"]')
const editIncomeItemButton = Selector('[data-qa="edit-income-item"]')
const coefficientSelect = (type: string) =>
  Selector(`[data-qa="income-coefficient-select-${type}"]`)
const coefficientOption = (type: string, option: string) =>
  Selector(`[data-qa="income-coefficient-select-${type}"]`).find(
    `[data-qa="select-option-${option}"]`
  )

async function chooseCoefficient(
  t: TestController,
  type: string,
  coefficient: string
) {
  await t.click(coefficientSelect(type))
  await t.click(coefficientOption(type, coefficient))
}

test('Create a new max fee accepted income.', async (t) => {
  await page.navigateToGuardianInformation(personId)

  await fridgeHeadInformation.openIncomesCollapsible()
  await t.click(newIncomeButton)

  await t.typeText(incomeStartDateInput, '01', { replace: true })
  await t.typeText(incomeStartDateInput, '01')
  await t.typeText(incomeStartDateInput, '20')

  await t.typeText(incomeEndDateInput, '31', { replace: true })
  await t.typeText(incomeEndDateInput, '01')
  await t.typeText(incomeEndDateInput, '20')

  await t.click(incomeEffect('MAX_FEE_ACCEPTED'))

  await t.click(saveIncomeButton)
})

test('Create a new income with multiple values.', async (t) => {
  await page.navigateToGuardianInformation(personId)

  await fridgeHeadInformation.openIncomesCollapsible()
  await t.click(newIncomeButton)

  await t.typeText(incomeStartDateInput, '01', { replace: true })
  await t.typeText(incomeStartDateInput, '01')
  await t.typeText(incomeStartDateInput, '20')

  await t.typeText(incomeEndDateInput, '31', { replace: true })
  await t.typeText(incomeEndDateInput, '01')
  await t.typeText(incomeEndDateInput, '20')

  await t.click(incomeEffect('INCOME'))

  await t.typeText(incomeInput('MAIN_INCOME'), '5000', { replace: true })
  await t.typeText(incomeInput('SECONDARY_INCOME'), '100,50')
  await t.typeText(incomeInput('ALL_EXPENSES'), '35,75')

  await t.click(saveIncomeButton)

  await t.click(toggleIncomeItemButton)
  await t.expect(incomeSum.textContent).contains('5100,50 €')
  await t.expect(expensesSum.textContent).contains('35,75 €')
})

test('Income editor save button is disabled with invalid values', async (t) => {
  await page.navigateToGuardianInformation(personId)

  await fridgeHeadInformation.openIncomesCollapsible()
  await t.click(newIncomeButton)

  // not a number
  await t.typeText(incomeInput('MAIN_INCOME'), 'asd', { replace: true })
  await t.expect(saveIncomeButton.hasAttribute('disabled')).ok()

  // too many decimals
  await t.typeText(incomeInput('MAIN_INCOME'), '123,123', { replace: true })
  await t.expect(saveIncomeButton.hasAttribute('disabled')).ok()

  await t.typeText(incomeInput('MAIN_INCOME'), '100', { replace: true })
  await t.expect(saveIncomeButton.hasAttribute('disabled')).notOk()

  // inverted date range
  await t.typeText(incomeStartDateInput, '31', { replace: true })
  await t.typeText(incomeStartDateInput, '01')
  await t.typeText(incomeStartDateInput, '20')
  await t.typeText(incomeEndDateInput, '01', { replace: true })
  await t.typeText(incomeEndDateInput, '01')
  await t.typeText(incomeEndDateInput, '20')
  await t.expect(saveIncomeButton.hasAttribute('disabled')).ok()
})

test('Existing income item can have its values updated.', async (t) => {
  await page.navigateToGuardianInformation(personId)

  // create new income item
  await fridgeHeadInformation.openIncomesCollapsible()
  await t.click(newIncomeButton)
  await t.typeText(incomeStartDateInput, '01', { replace: true })
  await t.typeText(incomeStartDateInput, '01')
  await t.typeText(incomeStartDateInput, '20')
  await t.typeText(incomeEndDateInput, '31', { replace: true })
  await t.typeText(incomeEndDateInput, '01')
  await t.typeText(incomeEndDateInput, '20')
  await t.click(incomeEffect('INCOME'))
  await t.typeText(incomeInput('MAIN_INCOME'), '5000', { replace: true })
  await t.click(saveIncomeButton)
  await t.click(toggleIncomeItemButton)
  await t.expect(incomeSum.textContent).contains('5000 €')
  await t.expect(expensesSum.textContent).contains('0 €')

  // edit existing item
  await t.click(editIncomeItemButton)

  await t.typeText(incomeInput('SECONDARY_INCOME'), '200')
  await t.typeText(incomeInput('ALL_EXPENSES'), '300')
  await t.selectText(incomeInput('MAIN_INCOME'))
  await t.pressKey('backspace')

  await t.click(saveIncomeButton)
  await t.expect(incomeSum.textContent).contains('200 €')
  await t.expect(expensesSum.textContent).contains('300 €')
})

test('Income coefficients are saved and affect the sum.', async (t) => {
  await page.navigateToGuardianInformation(personId)

  await fridgeHeadInformation.openIncomesCollapsible()
  await t.click(newIncomeButton)

  await t.typeText(incomeStartDateInput, '01', { replace: true })
  await t.typeText(incomeStartDateInput, '01')
  await t.typeText(incomeStartDateInput, '20')

  await t.typeText(incomeEndDateInput, '31', { replace: true })
  await t.typeText(incomeEndDateInput, '01')
  await t.typeText(incomeEndDateInput, '20')

  await t.click(incomeEffect('INCOME'))

  await t.typeText(incomeInput('MAIN_INCOME'), '100000', { replace: true })
  await chooseCoefficient(t, 'MAIN_INCOME', 'YEARLY')

  await t.typeText(incomeInput('ALIMONY'), '50')
  await t.typeText(incomeInput('ALL_EXPENSES'), '35,75')

  await t.click(saveIncomeButton)

  await t.click(toggleIncomeItemButton)
  await t.expect(incomeSum.textContent).contains('8380 €') // 8330 + 50
  await t.expect(expensesSum.textContent).contains('35,75 €')
})
