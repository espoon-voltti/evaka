// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
import { VasuPreviewPage } from '../../pages/employee/vasu/vasu'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let childId: UUID
let templateId: UUID
let vasuDocId: UUID

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
  vasuDocId = await insertVasuDocument(childId, templateId)
  await publishVasuDocument(vasuDocId)

  page = await Page.open()
  await enduserLogin(page)
})

describe('Citizen Vasu document page', () => {
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
