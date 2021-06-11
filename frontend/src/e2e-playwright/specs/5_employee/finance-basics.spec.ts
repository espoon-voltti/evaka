// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import config from 'e2e-test-common/config'
import {
  insertEmployeeFixture,
  insertFeeThresholds,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { newBrowserContext } from 'e2e-playwright/browser'
import { employeeLogin } from 'e2e-playwright/utils/user'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import FinanceBasicsPage from 'e2e-playwright/pages/employee/finance-basics'
import { waitUntilEqual } from 'e2e-playwright/utils'

let page: Page
let financeBasicsPage: FinanceBasicsPage
let nav: EmployeeNav

beforeEach(async () => {
  await resetDatabase()
  await insertEmployeeFixture({
    id: config.financeAdminAad,
    externalId: `espoo-ad:${config.financeAdminAad}`,
    email: 'lasse.laskuttaja@espoo.fi',
    firstName: 'Lasse',
    lastName: 'Laskuttaja',
    roles: ['FINANCE_ADMIN']
  })

  page = await (await newBrowserContext({ acceptDownloads: true })).newPage()
  await employeeLogin(page, 'FINANCE_ADMIN')
  await page.goto(config.employeeUrl)

  financeBasicsPage = new FinanceBasicsPage(page)
  nav = new EmployeeNav(page)
})
afterEach(async () => {
  await page.close()
})

describe('Finance basics', () => {
  test('Navigate to finance basics page', async () => {
    await nav.openAndClickDropdownMenuItem('financeBasics')
    await financeBasicsPage.feesSection.root.waitUntilVisible()
  })

  test('Create a new set of retroactive fee thresholds', async () => {
    const data = {
      validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
      maxFee: 10000,
      minFee: 1000,
      minIncomeThreshold2: 100000,
      incomeMultiplier2: 0.1,
      maxIncomeThreshold2: 200000,
      minIncomeThreshold3: 200000,
      incomeMultiplier3: 0.1,
      maxIncomeThreshold3: 300000,
      minIncomeThreshold4: 300000,
      incomeMultiplier4: 0.1,
      maxIncomeThreshold4: 400000,
      minIncomeThreshold5: 400000,
      incomeMultiplier5: 0.1,
      maxIncomeThreshold5: 500000,
      minIncomeThreshold6: 500000,
      incomeMultiplier6: 0.1,
      maxIncomeThreshold6: 600000,
      incomeThresholdIncrease6Plus: 100000,
      siblingDiscount2: 0.5,
      siblingDiscount2Plus: 0.8
    }
    await nav.openAndClickDropdownMenuItem('financeBasics')

    await financeBasicsPage.feesSection.createFeeThresholdsButton.click()
    await financeBasicsPage.feesSection.editor.fillInThresholds(data)
    await financeBasicsPage.feesSection.editor.save(true)

    const newThresholdsItem = financeBasicsPage.feesSection.item(0)
    await newThresholdsItem.element.waitUntilVisible()
    await newThresholdsItem.assertItemContains(data)
  })

  test('Creating a new set of retroactive fee thresholds ends the previous', async () => {
    const originalData = {
      validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
      maxFee: 10000,
      minFee: 1000,
      minIncomeThreshold2: 100000,
      incomeMultiplier2: 0.1,
      maxIncomeThreshold2: 200000,
      minIncomeThreshold3: 200000,
      incomeMultiplier3: 0.1,
      maxIncomeThreshold3: 300000,
      minIncomeThreshold4: 300000,
      incomeMultiplier4: 0.1,
      maxIncomeThreshold4: 400000,
      minIncomeThreshold5: 400000,
      incomeMultiplier5: 0.1,
      maxIncomeThreshold5: 500000,
      minIncomeThreshold6: 500000,
      incomeMultiplier6: 0.1,
      maxIncomeThreshold6: 600000,
      incomeThresholdIncrease6Plus: 100000,
      siblingDiscount2: 0.5,
      siblingDiscount2Plus: 0.8
    }
    await insertFeeThresholds(originalData)
    await nav.openAndClickDropdownMenuItem('financeBasics')

    const newData = {
      ...originalData,
      validDuring: originalData.validDuring.withStart(
        originalData.validDuring.start.addYears(1)
      )
    }
    await financeBasicsPage.feesSection.createFeeThresholdsButton.click()
    await financeBasicsPage.feesSection.editor.fillInThresholds(newData)
    await financeBasicsPage.feesSection.editor.save(true)

    const newThresholdsItem = financeBasicsPage.feesSection.item(0)
    await newThresholdsItem.element.waitUntilVisible()
    await newThresholdsItem.assertItemContains(newData)

    const oldData = {
      ...originalData,
      validDuring: originalData.validDuring.withEnd(
        newData.validDuring.start.subDays(1)
      )
    }
    const oldThresholdsItem = financeBasicsPage.feesSection.item(1)
    await oldThresholdsItem.element.waitUntilVisible()
    await oldThresholdsItem.assertItemContains(oldData)
  })

  test('Creating fee thresholds shows a validation error when max fees do not match', async () => {
    // Family size 2 has a different max fee
    const data = {
      validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
      maxFee: 10000,
      minFee: 1000,
      minIncomeThreshold2: 100000,
      incomeMultiplier2: 0.1,
      maxIncomeThreshold2: 300000,
      minIncomeThreshold3: 200000,
      incomeMultiplier3: 0.1,
      maxIncomeThreshold3: 300000,
      minIncomeThreshold4: 300000,
      incomeMultiplier4: 0.1,
      maxIncomeThreshold4: 400000,
      minIncomeThreshold5: 400000,
      incomeMultiplier5: 0.1,
      maxIncomeThreshold5: 500000,
      minIncomeThreshold6: 500000,
      incomeMultiplier6: 0.1,
      maxIncomeThreshold6: 600000,
      incomeThresholdIncrease6Plus: 100000,
      siblingDiscount2: 0.5,
      siblingDiscount2Plus: 0.8
    }
    await nav.openAndClickDropdownMenuItem('financeBasics')

    await financeBasicsPage.feesSection.createFeeThresholdsButton.click()
    await financeBasicsPage.feesSection.editor.fillInThresholds(data)
    await financeBasicsPage.feesSection.editor.assertSaveIsDisabled()
    await waitUntilEqual(
      () => financeBasicsPage.feesSection.editor.maxFeeError(2).innerText,
      'Enimmäismaksu ei täsmää (200 €)'
    )
  })

  test('Copying existing fee thresholds', async () => {
    const originalData = {
      validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
      maxFee: 10000,
      minFee: 1000,
      minIncomeThreshold2: 100000,
      incomeMultiplier2: 0.1,
      maxIncomeThreshold2: 200000,
      minIncomeThreshold3: 200000,
      incomeMultiplier3: 0.1,
      maxIncomeThreshold3: 300000,
      minIncomeThreshold4: 300000,
      incomeMultiplier4: 0.1,
      maxIncomeThreshold4: 400000,
      minIncomeThreshold5: 400000,
      incomeMultiplier5: 0.1,
      maxIncomeThreshold5: 500000,
      minIncomeThreshold6: 500000,
      incomeMultiplier6: 0.1,
      maxIncomeThreshold6: 600000,
      incomeThresholdIncrease6Plus: 100000,
      siblingDiscount2: 0.5,
      siblingDiscount2Plus: 0.8
    }
    await insertFeeThresholds(originalData)
    await nav.openAndClickDropdownMenuItem('financeBasics')

    const originalThresholdsItem = financeBasicsPage.feesSection.item(0)
    await originalThresholdsItem.element.waitUntilVisible()

    const newDateRange = new DateRange(
      originalData.validDuring.start.addYears(1),
      originalData.validDuring.start.addYears(2)
    )
    await originalThresholdsItem.copy()
    await financeBasicsPage.feesSection.editor.validFromInput.fill(
      newDateRange.start.format()
    )
    await financeBasicsPage.feesSection.editor.validToInput.fill(
      newDateRange.end?.format() ?? ''
    )
    await financeBasicsPage.feesSection.editor.save(true)

    const newThresholdsItem = financeBasicsPage.feesSection.item(0)
    await newThresholdsItem.element.waitUntilVisible()
    await newThresholdsItem.assertItemContains({
      ...originalData,
      validDuring: newDateRange
    })

    const oldThresholdsItem = financeBasicsPage.feesSection.item(1)
    await oldThresholdsItem.element.waitUntilVisible()
    await oldThresholdsItem.assertItemContains({
      ...originalData,
      validDuring: originalData.validDuring.withEnd(
        newDateRange.start.subDays(1)
      )
    })
  })

  test('Editing existing fee thresholds', async () => {
    const originalData = {
      validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
      maxFee: 10000,
      minFee: 1000,
      minIncomeThreshold2: 100000,
      incomeMultiplier2: 0.1,
      maxIncomeThreshold2: 200000,
      minIncomeThreshold3: 200000,
      incomeMultiplier3: 0.1,
      maxIncomeThreshold3: 300000,
      minIncomeThreshold4: 300000,
      incomeMultiplier4: 0.1,
      maxIncomeThreshold4: 400000,
      minIncomeThreshold5: 400000,
      incomeMultiplier5: 0.1,
      maxIncomeThreshold5: 500000,
      minIncomeThreshold6: 500000,
      incomeMultiplier6: 0.1,
      maxIncomeThreshold6: 600000,
      incomeThresholdIncrease6Plus: 100000,
      siblingDiscount2: 0.5,
      siblingDiscount2Plus: 0.8
    }
    await insertFeeThresholds(originalData)
    await nav.openAndClickDropdownMenuItem('financeBasics')

    const thresholdsItem = financeBasicsPage.feesSection.item(0)
    await thresholdsItem.element.waitUntilVisible()

    await thresholdsItem.edit()
    await financeBasicsPage.feesSection.editor.minFeeInput.fill('20')
    await financeBasicsPage.feesSection.editor.save(true)

    await thresholdsItem.assertItemContains({
      ...originalData,
      minFee: 2000
    })
  })

  test('Date overlap on fee thresholds with an end date prevents saving new fee thresholds', async () => {
    const originalData = {
      validDuring: new DateRange(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 12, 31)
      ),
      maxFee: 10000,
      minFee: 1000,
      minIncomeThreshold2: 100000,
      incomeMultiplier2: 0.1,
      maxIncomeThreshold2: 200000,
      minIncomeThreshold3: 200000,
      incomeMultiplier3: 0.1,
      maxIncomeThreshold3: 300000,
      minIncomeThreshold4: 300000,
      incomeMultiplier4: 0.1,
      maxIncomeThreshold4: 400000,
      minIncomeThreshold5: 400000,
      incomeMultiplier5: 0.1,
      maxIncomeThreshold5: 500000,
      minIncomeThreshold6: 500000,
      incomeMultiplier6: 0.1,
      maxIncomeThreshold6: 600000,
      incomeThresholdIncrease6Plus: 100000,
      siblingDiscount2: 0.5,
      siblingDiscount2Plus: 0.8
    }
    await insertFeeThresholds(originalData)
    await nav.openAndClickDropdownMenuItem('financeBasics')

    const newData = {
      ...originalData,
      validDuring: new DateRange(
        originalData.validDuring.start.addMonths(6),
        originalData.validDuring.start.addMonths(18)
      )
    }
    await financeBasicsPage.feesSection.createFeeThresholdsButton.click()
    await financeBasicsPage.feesSection.editor.fillInThresholds(newData)
    await financeBasicsPage.feesSection.editor.assertSaveIsDisabled()
  })
})
