// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import EmployeeNav from '../../pages/employee/employee-nav'
import FinanceBasicsPage from '../../pages/employee/finance-basics'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let financeBasicsPage: FinanceBasicsPage
let nav: EmployeeNav

beforeEach(async () => {
  await resetServiceState()
})

describe('Finance basics', () => {
  beforeEach(async () => {
    const financeAdmin = await Fixture.employeeFinanceAdmin().save()

    page = await Page.open({ acceptDownloads: true })
    await employeeLogin(page, financeAdmin.data)
    await page.goto(config.employeeUrl)

    financeBasicsPage = new FinanceBasicsPage(page)
    nav = new EmployeeNav(page)
  })

  test('Navigate to finance basics page', async () => {
    await nav.openAndClickDropdownMenuItem('finance-basics')
    await financeBasicsPage.feesSection.root.waitUntilVisible()
  })

  test('Create a new set of retroactive fee thresholds', async () => {
    const { data } = Fixture.feeThresholds()
    await nav.openAndClickDropdownMenuItem('finance-basics')

    await financeBasicsPage.feesSection.createFeeThresholdsButton.click()
    await financeBasicsPage.feesSection.editor.fillInThresholds(data)
    await financeBasicsPage.feesSection.editor.save(true)

    const newThresholdsItem = financeBasicsPage.feesSection.item(0)
    await newThresholdsItem.element.waitUntilVisible()
    await newThresholdsItem.assertItemContains(data)
  })

  test('Creating a new set of retroactive fee thresholds ends the previous', async () => {
    const originalData = (await Fixture.feeThresholds().save()).data
    await nav.openAndClickDropdownMenuItem('finance-basics')

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
    const { data } = Fixture.feeThresholds().with({
      maxIncomeThreshold2: 300000
    })
    await nav.openAndClickDropdownMenuItem('finance-basics')

    await financeBasicsPage.feesSection.createFeeThresholdsButton.click()
    await financeBasicsPage.feesSection.editor.fillInThresholds(data)
    await financeBasicsPage.feesSection.editor.assertSaveIsDisabled()
    await financeBasicsPage.feesSection.editor
      .maxFeeError(2)
      .assertTextEquals('Enimmäismaksu ei täsmää (200 €)')
  })

  test('Copying existing fee thresholds', async () => {
    const originalData = (await Fixture.feeThresholds().save()).data
    await nav.openAndClickDropdownMenuItem('finance-basics')

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
    const originalData = (await Fixture.feeThresholds().save()).data
    await nav.openAndClickDropdownMenuItem('finance-basics')

    const thresholdsItem = financeBasicsPage.feesSection.item(0)
    await thresholdsItem.element.waitUntilVisible()

    await thresholdsItem.edit()
    await financeBasicsPage.feesSection.editor.minFeeInput.fill('20')
    await financeBasicsPage.feesSection.editor.temporaryFee.fill('39')
    await financeBasicsPage.feesSection.editor.save(true)

    await thresholdsItem.assertItemContains({
      ...originalData,
      minFee: 2000,
      temporaryFee: 3900
    })
  })

  test('Date overlap on fee thresholds with an end date prevents saving new fee thresholds', async () => {
    const originalData = (
      await Fixture.feeThresholds()
        .with({
          validDuring: new DateRange(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31)
          )
        })
        .save()
    ).data
    await nav.openAndClickDropdownMenuItem('finance-basics')

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
