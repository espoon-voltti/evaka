// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { resetDatabase } from '../../dev-api'
import { CitizenChildIncomeStatementListPage } from '../../pages/citizen/citizen-child-income'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let child1ISList: CitizenChildIncomeStatementListPage

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-test/assets/${testFileName}`

beforeEach(async () => {
  await resetDatabase()

  page = await Page.open()
  await enduserLogin(page)
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
    const editPage = await child1ISList.createIncomeStatement()
    await editPage.selectNoIncome()
    await editPage.selectAssure()
    await editPage.save()
    await child1ISList.assertChildIncomeStatementRowCount(1)

    // Edit
    await child1ISList.clickEditChildIncomeStatement(0)
    await editPage.selectHasIncome()
    await editPage.uploadAttachment(testFilePath)
    await editPage.selectAssure()
    await editPage.save()
    await child1ISList.assertChildIncomeStatementRowCount(1)

    // View
    const viewPage = await child1ISList.clickViewChildIncomeStatement(0)
    await viewPage.waitUntilReady()
    await viewPage.assertAttachmentExists(testFileName)
    await viewPage.clickGoBack()

    // Delete
    await child1ISList.deleteChildIncomeStatement(0)
    await child1ISList.assertChildIncomeStatementRowCount(0)
  })
})
