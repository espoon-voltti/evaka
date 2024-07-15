// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import {
  createDaycarePlacementFixture,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import { CitizenChildIncomeStatementListPage } from '../../pages/citizen/citizen-child-income'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let child1ISList: CitizenChildIncomeStatementListPage

const testFileName1 = 'test_file.png'
const testFilePath1 = `src/e2e-test/assets/${testFileName1}`
const testFileName2 = 'test_file.jpg'
const testFilePath2 = `src/e2e-test/assets/${testFileName2}`

beforeEach(async () => {
  await resetServiceState()

  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()

  await createDaycarePlacements({
    body: [
      createDaycarePlacementFixture(
        uuidv4(),
        testChild.id,
        testDaycare.id,
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      createDaycarePlacementFixture(
        uuidv4(),
        testChild.id,
        testDaycare.id,
        LocalDate.todayInSystemTz().addDays(1),
        LocalDate.todayInSystemTz().addDays(1)
      )
    ]
  })

  page = await Page.open()
  await enduserLogin(page, testAdult)
  header = new CitizenHeader(page)
  child1ISList = new CitizenChildIncomeStatementListPage(page, 0)
})

describe('Child Income statements', () => {
  test('Shows a warning of missing income statement', async () => {
    await header.selectTab('income')
    await child1ISList.assertChildName(
      'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani Karhula'
    )
    await child1ISList.assertIncomeStatementMissingWarningIsShown()
  })

  test('Create a highest fee income statement, change it as child income statement, view and delete it', async () => {
    // Create
    await header.selectTab('income')
    await child1ISList.assertChildCount(1)

    const editPage = await child1ISList.createIncomeStatement()
    await editPage.setValidFromDate('01.02.2034')
    await editPage.uploadAttachment(testFilePath1)
    await editPage.selectAssure()
    await editPage.save()
    await child1ISList.assertChildIncomeStatementRowCount(1)

    // Edit
    await child1ISList.clickEditChildIncomeStatement(0)
    await editPage.uploadAttachment(testFilePath2)
    await editPage.typeOtherInfo('foo bar baz')
    await editPage.selectAssure()
    await editPage.save()
    await child1ISList.assertChildIncomeStatementRowCount(1)

    // View
    const viewPage = await child1ISList.clickViewChildIncomeStatement(0)
    await viewPage.waitUntilReady()
    await viewPage.assertOtherInfo('foo bar baz')
    await viewPage.assertAttachmentExists(testFileName1)
    await viewPage.assertAttachmentExists(testFileName2)
    await viewPage.clickGoBack()

    // Delete
    await child1ISList.deleteChildIncomeStatement(0)
    await child1ISList.assertIncomeStatementMissingWarningIsShown()
  })
})
