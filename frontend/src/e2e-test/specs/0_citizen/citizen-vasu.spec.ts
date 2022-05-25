// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertGuardianFixtures,
  insertVasuDocument,
  insertVasuTemplateFixture,
  publishVasuDocument,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  enduserGuardianFixture,
  uuidv4
} from '../../dev-api/fixtures'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { ChildDocumentsPage } from '../../pages/employee/vasu/child-documents'
import { VasuPreviewPage } from '../../pages/employee/vasu/vasu'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let childId: UUID
let templateId: UUID
let vasuDocId: UUID
let header: CitizenHeader

beforeEach(async () => {
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
  vasuDocId = await insertVasuDocument(childId, templateId)
  await publishVasuDocument(vasuDocId)
  page = await Page.open()
  header = new CitizenHeader(page, 'desktop')
  await enduserLogin(page)
})

describe('Citizen vasu document page', () => {
  const openDocument = async () => {
    await page.goto(`${config.enduserUrl}/vasu/${vasuDocId}`)
    return new VasuPreviewPage(page)
  }

  test('View child vasu document and give permission to share', async () => {
    await insertGuardianFixtures([
      {
        guardianId: enduserGuardianFixture.id,
        childId: childId
      }
    ])

    const vasuPage = await openDocument()
    await vasuPage.assertTitleChildName('Antero Onni Leevi Aatu Högfors')
    await vasuPage.givePermissionToShare()

    await vasuPage.assertGivePermissionToShareSectionIsNotVisible()
    await page.reload()
    await vasuPage.assertGivePermissionToShareSectionIsNotVisible()
  })
})

describe('Citizen child documents listing page', () => {
  test('Published vasu document is in the list', async () => {
    await insertGuardianFixtures([
      {
        guardianId: enduserGuardianFixture.id,
        childId: childId
      }
    ])
    await page.reload()
    await header.selectTab('child-documents')
    const childDocumentsPage = new ChildDocumentsPage(page)
    await childDocumentsPage.vasuCollapsible.open()
    await childDocumentsPage.assertVasuRow(
      vasuDocId,
      'Luonnos',
      LocalDate.today().format('dd.MM.yyyy')
    )
  })
})
