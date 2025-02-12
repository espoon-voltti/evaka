// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import {
  createDaycarePlacementFixture,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  resetServiceState,
  updateIncomeStatementHandled
} from '../../generated/api-clients'
import { CitizenChildIncomeStatementListPage } from '../../pages/citizen/citizen-child-income'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { envs, Page, testFileName } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let listPage: CitizenChildIncomeStatementListPage

describe.each(envs)('Child Income statements (%s)', (env) => {
  beforeEach(async () => {
    await resetServiceState()

    await Fixture.careArea(testCareArea).save()
    await Fixture.daycare(testDaycare).save()
    await Fixture.family({ guardian: testAdult, children: [testChild] }).save()

    await createDaycarePlacements({
      body: [
        createDaycarePlacementFixture(
          randomId(),
          testChild.id,
          testDaycare.id,
          LocalDate.todayInSystemTz(),
          LocalDate.todayInSystemTz()
        ),
        createDaycarePlacementFixture(
          randomId(),
          testChild.id,
          testDaycare.id,
          LocalDate.todayInSystemTz().addDays(1),
          LocalDate.todayInSystemTz().addDays(1)
        )
      ]
    })

    const viewport =
      env === 'mobile'
        ? { width: 375, height: 812 }
        : { width: 1920, height: 1080 }

    page = await Page.open({
      viewport,
      screen: viewport
    })
    await enduserLogin(page, testAdult)
    header = new CitizenHeader(page, env)
    listPage = new CitizenChildIncomeStatementListPage(page, 0, env)
  })

  test('Shows a warning of missing income statement', async () => {
    await header.selectTab('income')
    await listPage.assertChildName(
      'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani Karhula'
    )
    await listPage.assertIncomeStatementMissingWarningIsShown()
  })

  test('Create child income statement, edit, send, edit again and delete', async () => {
    // Create
    await header.selectTab('income')
    await listPage.assertChildCount(1)

    const editPage = await listPage.createIncomeStatement()
    await editPage.setValidFromDate('01.02.2034')
    await editPage.attachments.uploadTestFile()
    await editPage.saveDraftButton.click()
    await listPage.assertChildIncomeStatementRowCount(1)

    // Edit draft
    await listPage.editChildDraftIncomeStatement(0)
    await editPage.setValidFromDate('01.03.2034')
    await editPage.attachments.uploadTestFile()
    await editPage.otherInfoInput.fill('foo bar baz')
    await editPage.assure.check()
    await editPage.sendButton.click()
    await listPage.assertChildIncomeStatementRowCount(1)

    // Edit sent
    const editSentPage = await listPage.editChildSentIncomeStatement(0)
    await editSentPage.otherInfoInput.fill('foo bar baz and more')
    await editPage.sendButton.click()
    await listPage.assertChildIncomeStatementRowCount(1)

    // Delete
    await listPage.deleteChildIncomeStatement(0)
    await listPage.assertIncomeStatementMissingWarningIsShown()
  })

  test('Create child income statement, mark as handled, and view', async () => {
    await header.selectTab('income')
    await listPage.assertChildCount(1)

    const editPage = await listPage.createIncomeStatement()
    await editPage.setValidFromDate('01.03.2034')
    await editPage.attachments.uploadTestFile()
    await editPage.otherInfoInput.fill('foo bar baz')
    await editPage.assure.check()
    await editPage.sendButton.click()
    await listPage.assertChildIncomeStatementRowCount(1)

    // Mark as handled
    const incomeStatementId = await listPage.incomeStatementId(0)
    const employee = await Fixture.employee().save()
    await updateIncomeStatementHandled({
      body: {
        incomeStatementId,
        employeeId: employee.id,
        note: '',
        handled: true
      }
    })

    // View
    const viewPage = await listPage.viewChildHandledIncomeStatement(0)
    await viewPage.otherInfo.assertTextEquals('foo bar baz')
    await viewPage.assertAttachmentExists('CHILD_INCOME', testFileName)
  })
})
