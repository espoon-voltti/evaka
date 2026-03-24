// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { UUID } from 'lib-common/types'

import config from '../../config'
import { Fixture, testChild } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import type { IncomeSection } from '../../pages/employee/guardian-information'
import { expect, test } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personId: UUID
let incomesSection: IncomeSection

test.beforeEach(async ({ evaka }) => {
  await resetServiceState()

  await testChild.saveChild()
  personId = testChild.id

  const financeAdmin = await Fixture.employee().financeAdmin().save()

  page = evaka
  await employeeLogin(page, financeAdmin)
  await page.goto(config.employeeUrl + '/child-information/' + personId)

  const childInformationPage = new ChildInformationPage(page)
  incomesSection = await childInformationPage.openCollapsible('income')
})

test.describe('Child Income', () => {
  test('Create a new max fee accepted income', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.save()

    await expect(incomesSection.incomeListItems).toHaveCount(1)
  })

  test('Create a new income with main income', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('INCOME')

    await incomesSection.fillIncome('MAIN_INCOME', '5000')
    await incomesSection.save()

    await expect(incomesSection.incomeListItems).toHaveCount(1)
    await expect.poll(() => incomesSection.getIncomeSum()).toBe('5000 €')
    await expect.poll(() => incomesSection.getExpensesSum()).toBe('0 €')
  })
})
