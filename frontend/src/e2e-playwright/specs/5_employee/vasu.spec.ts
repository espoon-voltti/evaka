// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertVasuDocument,
  insertVasuTemplateFixture,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import ChildInformationPage, {
  VasuAndLeopsSection
} from 'e2e-playwright/pages/employee/child-information'
import { newBrowserContext } from 'e2e-playwright/browser'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { UUID } from 'lib-common/types'
import VasuPage from '../../pages/employee/vasu/vasu'
import LocalDate from 'lib-common/local-date'

let page: Page
let childInformationPage: ChildInformationPage
let childId: UUID
let templateId: UUID

beforeAll(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )

  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  templateId = await insertVasuTemplateFixture()
})

describe('Child Information - Vasu documents section', () => {
  let section: VasuAndLeopsSection
  beforeEach(async () => {
    page = await (await newBrowserContext()).newPage()
    await employeeLogin(page, 'ADMIN')
    await page.goto(`${config.employeeUrl}/child-information/${childId}`)
    childInformationPage = new ChildInformationPage(page)
    section = await childInformationPage.openCollapsible('vasuAndLeops')
  })

  test('Can add a new vasu document', async () => {
    await section.addNew()
  })
})

describe('Vasu document page', () => {
  let vasuPage: VasuPage
  let vasuDocId: UUID

  const openDocument = async () => {
    await page.goto(`${config.employeeUrl}/vasu/${vasuDocId}`)
    return new VasuPage(page)
  }

  const editDocument = async () => {
    await page.goto(`${config.employeeUrl}/vasu/${vasuDocId}/edit`)
    return new VasuPage(page)
  }

  beforeAll(async () => {
    vasuDocId = await insertVasuDocument(childId, templateId)
  })

  beforeEach(async () => {
    page = await (await newBrowserContext()).newPage()
    await employeeLogin(page, 'ADMIN')
    vasuPage = await openDocument()
  })

  test('An unpublished vasu document has no followup questions', async () => {
    const count = await vasuPage.followupQuestionCount
    expect(count).toBe(0)
  })

  describe('With a finalized document', () => {
    const finalizeDocument = async () => {
      await vasuPage.finalizeButton.click()
      await vasuPage.modalOkButton.click()
      await vasuPage.modalOkButton.click()
      vasuPage = await openDocument()
      await vasuPage.assertDocumentVisible()
    }

    beforeAll(async () => {
      page = await (await newBrowserContext()).newPage()
      await employeeLogin(page, 'ADMIN')
      vasuPage = await openDocument()
      await finalizeDocument()
    })

    test('A published vasu document has one followup question', async () => {
      const count = await vasuPage.followupQuestionCount
      expect(count).toBe(1)
    })

    test('Adding a followup comment renders it on the page', async () => {
      vasuPage = await editDocument()
      await vasuPage.inputFollowupComment('This is a followup')
      let entryTexts = await vasuPage.followupEntryTexts
      expect(entryTexts).toEqual(['This is a followup'])
      await vasuPage.inputFollowupComment('A second one')
      entryTexts = await vasuPage.followupEntryTexts
      expect(entryTexts).toEqual(['This is a followup', 'A second one'])

      const entryMetadata = await vasuPage.followupEntryMetadata
      const expectedMetadataStr = `${LocalDate.today().format()} Seppo Sorsa`
      expect(entryMetadata).toEqual([expectedMetadataStr, expectedMetadataStr])
    })

    const lastElement = <T>(arr: Array<T>): T => arr[arr.length - 1]

    test('A user can edit his own followup comment', async () => {
      vasuPage = await editDocument()
      await vasuPage.inputFollowupComment('This will be edited')
      let entryTexts = await vasuPage.followupEntryTexts
      expect(lastElement(entryTexts)).toEqual('This will be edited')

      await vasuPage.editFollowupComment(entryTexts.length - 1, 'now edited: ')

      entryTexts = await vasuPage.followupEntryTexts
      expect(lastElement(entryTexts)).toEqual('now edited: This will be edited')

      const entryMetadata = await vasuPage.followupEntryMetadata
      const date = LocalDate.today().format()
      const expectedMetadataStr = `${date} Seppo Sorsa, muokattu ${date} Seppo Sorsa`
      expect(lastElement(entryMetadata)).toEqual(expectedMetadataStr)
    })
  })
})
