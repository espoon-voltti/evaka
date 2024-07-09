// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../../config'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import ChildInformationPage, {
  FeeAlterationsSection
} from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personId: UUID
let feeAlterationSection: FeeAlterationsSection

beforeEach(async () => {
  await resetServiceState()

  const fixtures = await initializeAreaAndPersonData()
  personId = fixtures.enduserChildFixtureJari.id

  const financeAdmin = await Fixture.employeeFinanceAdmin().save()

  page = await Page.open()
  await employeeLogin(page, financeAdmin)
  await page.goto(config.employeeUrl + '/child-information/' + personId)

  const childInformationPage = new ChildInformationPage(page)
  feeAlterationSection =
    await childInformationPage.openCollapsible('feeAlterations')
})

describe('Child fee alteration', () => {
  it('Create a new fee alteration with attachment', async () => {
    const testFileName1 = 'test_file.png'
    const testFilePath1 = `src/e2e-test/assets/${testFileName1}`

    const testFileName2 = 'test_file.jpg'
    const testFilePath2 = `src/e2e-test/assets/${testFileName2}`

    const feeAlteration = {
      startDate: '01.01.2020',
      endDate: '31.01.2020',
      amount: '10',
      attachment: testFilePath1
    }

    const feeAlteration2 = {
      startDate: '01.02.2020',
      endDate: '29.02.2020',
      amount: '20',
      attachment: testFilePath2
    }

    const editor = await feeAlterationSection.openNewFeeAlterationEditorPage()
    await editor.startDateInput.fill(feeAlteration.startDate)
    await editor.endDateInput.fill(feeAlteration.endDate)
    await editor.alterationValueInput.fill(feeAlteration.amount)
    await editor.uploadAttachmentAndAssert(feeAlteration.attachment)
    await editor.saveButton.click()

    const editor2 = await feeAlterationSection.openNewFeeAlterationEditorPage()
    await editor2.startDateInput.fill(feeAlteration2.startDate)
    await editor2.endDateInput.fill(feeAlteration2.endDate)
    await editor2.alterationValueInput.fill(feeAlteration2.amount)
    await editor2.uploadAttachmentAndAssert(feeAlteration2.attachment)
    await editor2.saveButton.click()

    await feeAlterationSection.assertAlterationDateRange(
      `${feeAlteration2.startDate} - ${feeAlteration2.endDate}`,
      0
    )
    await feeAlterationSection.assertAlterationDateRange(
      `${feeAlteration.startDate} - ${feeAlteration.endDate}`,
      1
    )
    await feeAlterationSection.assertAlterationAmount(
      `Alennus ${feeAlteration2.amount}%`,
      0
    )
    await feeAlterationSection.assertAlterationAmount(
      `Alennus ${feeAlteration.amount}%`,
      1
    )
    await feeAlterationSection.assertAttachmentExists(testFileName1)
    await feeAlterationSection.assertAttachmentExists(testFileName2)
  })
})
