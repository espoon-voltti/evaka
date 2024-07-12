// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../../config'
import { Fixture, testChild } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import { IncomeSection } from '../../pages/employee/guardian-information'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personId: UUID
let incomesSection: IncomeSection

beforeEach(async () => {
  await resetServiceState()

  await Fixture.person().with(testChild).saveChild()
  personId = testChild.id

  const financeAdmin = await Fixture.employeeFinanceAdmin().save()

  page = await Page.open()
  await employeeLogin(page, financeAdmin)
  await page.goto(config.employeeUrl + '/child-information/' + personId)

  const childInformationPage = new ChildInformationPage(page)
  incomesSection = await childInformationPage.openCollapsible('income')
})

describe('Child Income', () => {
  it('Create a new max fee accepted income', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('MAX_FEE_ACCEPTED')
    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.incomeListItemCount(), 1)
  })

  it('Create a new income with main income', async () => {
    await incomesSection.openNewIncomeForm()

    await incomesSection.fillIncomeStartDate('1.1.2020')
    await incomesSection.fillIncomeEndDate('31.1.2020')
    await incomesSection.confirmRetroactive.check()
    await incomesSection.chooseIncomeEffect('INCOME')

    await incomesSection.fillIncome('MAIN_INCOME', '5000')
    await incomesSection.save()

    await waitUntilEqual(() => incomesSection.incomeListItemCount(), 1)
    await waitUntilEqual(() => incomesSection.getIncomeSum(), '5000 €')
    await waitUntilEqual(() => incomesSection.getExpensesSum(), '0 €')
  })
})
