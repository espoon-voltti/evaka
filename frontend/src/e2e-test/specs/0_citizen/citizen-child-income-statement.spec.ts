// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
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
import { test } from '../../playwright'
import type { EnvType, Page } from '../../utils/page'
import { testFileName } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

async function setupPageObjects(page: Page, env: EnvType) {
  await enduserLogin(page, testAdult)
  const header = new CitizenHeader(page, env)
  const listPage = new CitizenChildIncomeStatementListPage(page, 0, env)
  return { header, listPage }
}

const now = HelsinkiDateTime.of(2024, 11, 25, 12)

for (const env of ['desktop', 'mobile'] as const) {
  const viewport =
    env === 'mobile'
      ? { width: 375, height: 812 }
      : { width: 1920, height: 1080 }

  test.describe(`Child Income statements (${env})`, () => {
    test.use({ viewport })

    test.beforeEach(async () => {
      await resetServiceState()

      await testCareArea.save()
      await testDaycare.save()
      await Fixture.family({
        guardian: testAdult,
        children: [testChild]
      }).save()

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
    })

    test('Shows a warning of missing income statement', async ({ evaka }) => {
      const { header, listPage } = await setupPageObjects(evaka, env)
      await header.selectTab('income')
      await listPage.assertChildName(
        'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani Karhula'
      )
      await listPage.assertIncomeStatementMissingWarningIsShown()
    })

    test('Create child income statement, edit, send, edit again and delete', async ({
      evaka
    }) => {
      const { header, listPage } = await setupPageObjects(evaka, env)
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
      await editPage.assure.evaluate((e) =>
        e.scrollIntoView({ block: 'center' })
      )
      await editPage.assure.check()
      await editPage.sendButton.click()
      await editPage.assertAriaLiveExistsAndIncludesNotification()
      await listPage.assertChildIncomeStatementRowCount(1)

      // Edit sent
      await editPage.closeNotification()
      const editSentPage = await listPage.editChildSentIncomeStatement(0)
      await editSentPage.otherInfoInput.fill('foo bar baz and more')
      await editPage.sendButton.click()
      await editPage.assertAriaLiveExistsAndIncludesNotification()
      await listPage.assertChildIncomeStatementRowCount(1)

      // Delete
      await listPage.deleteChildIncomeStatement(0)
      await listPage.assertIncomeStatementMissingWarningIsShown()
    })

    test('Create child income statement, mark as handled, and view', async ({
      evaka
    }) => {
      const { header, listPage } = await setupPageObjects(evaka, env)
      await header.selectTab('income')
      await listPage.assertChildCount(1)

      const editPage = await listPage.createIncomeStatement()
      await editPage.setValidFromDate('01.03.2034')
      await editPage.attachments.uploadTestFile()
      await editPage.otherInfoInput.fill('foo bar baz')
      await editPage.assure.evaluate((e) =>
        e.scrollIntoView({ block: 'center' })
      )
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
          status: 'HANDLED'
        }
      })

      // View
      const viewPage = await listPage.viewChildHandledIncomeStatement(0)
      await viewPage.otherInfo.assertTextEquals('foo bar baz')
      await viewPage.assertAttachmentExists('CHILD_INCOME', testFileName)
    })

    test('Citizen cannot delete child income statement while employee is handling it', async ({
      evaka
    }) => {
      const { header, listPage } = await setupPageObjects(evaka, env)
      const employee = await Fixture.employee().save()
      const incomeStatement = await Fixture.incomeStatement({
        personId: testChild.id,
        data: {
          type: 'CHILD_INCOME',
          otherInfo: 'Some other info',
          startDate: now.toLocalDate(),
          endDate: null,
          attachmentIds: []
        },
        status: 'SENT'
      }).save()

      await header.selectTab('income')
      await listPage.assertNthIncomeStatementDeleteButtonDisabled(0, false)

      await updateIncomeStatementHandled({
        body: {
          incomeStatementId: incomeStatement.id,
          employeeId: employee.id,
          note: '',
          status: 'HANDLING'
        }
      })

      await evaka.reload()
      await listPage.assertNthIncomeStatementDeleteButtonDisabled(0, true)
    })
  })
}
